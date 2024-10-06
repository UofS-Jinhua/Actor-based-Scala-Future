package Tasks

import Actors._
import akka.actor._
import scala.concurrent.duration.Duration
import scala.concurrent.{Awaitable, CanAwait}
import akka.actor.Kill


import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream


object RemoteTask{

    var exist_task = List.empty[Int]
}

class RemoteTask[T](RSActor: ActorSelection, taskID: Int = -1, ishead: Boolean = false, max_num_task: Int = 1000, canAwait: Boolean = false)(implicit context: ActorContext) extends Task[T] {

    import RemoteSeverActor._
    // import CombinedWorker._
    final private val head = ishead
    final protected val TID = taskID
    final private var Result: Option[T] = None
    final private var canWait = canAwait
    final private var tempActor: ActorRef = null
    final private var isDone = false

    private [this] def AppendTaskID(): Int = this.synchronized{
        val cur_id = Stream.continually(scala.util.Random.nextInt(max_num_task)).find(id => ! RemoteTask.exist_task.contains(id)).get
        RemoteTask.exist_task = RemoteTask.exist_task :+ cur_id
        cur_id
    }


    def Start(op: SerializableFunction_1[T]): RemoteTask[T] = {
        if (TID == -1) {
            val cur_id = AppendTaskID()
            RSActor ! NewTask(cur_id, serializedOp(op))
            
            new RemoteTask[T](RSActor, cur_id, true) 
        }else{
            throw new Exception("Start should be called only once for this task chain") 
        }
    }


    def Zip[U](that: RemoteTask[U]): RemoteTask[(T, U)] = {
        if (TID == -1) {
            throw new Exception("Cannot zip with an empty task")
        }
        else {
            val cur_id = AppendTaskID()
            RSActor ! ZipTask(TID, that.TID, cur_id)

            new RemoteTask[(T, U)](RSActor, cur_id, true)
        }
    }


    def Group[T](tasks: Seq[RemoteTask[T]]): RemoteTask[List[T]] = {
        if (TID == -1) {
            val cur_id = AppendTaskID()
            val ids = for (task <- tasks) yield task.TID
            RSActor ! GroupTask(ids.toList, cur_id)

            new RemoteTask[List[T]](RSActor, cur_id, true)
        }else{
            throw new Exception("Group should be called with an empty task chain.")
        }
    }



    def NextStep[U](op2: SerializableFunction_2[T,U]): RemoteTask[U] = {
        if (TID == -1) {
            throw new Exception("First task has not been created, Start() should be called first")
        }
        else{
            val cur_id = AppendTaskID()
            RSActor ! NextTask(TID, cur_id, serializedOp(op2))

            new RemoteTask[U](RSActor, cur_id, false)
        }
    }


    def Recover(default_r: T): RemoteTask[T] = {
        if (TID == -1) {
            throw new Exception("No task has been created yet.")
        }
        else{
            RSActor ! Server_DR(TID, default_r)
            this
        }

    }

    def RecoverWith(func: SerializableFunction_1[T]): RemoteTask[T] = {
        if (!head) {
            throw new Exception("This RecoverWith should be only called for the first task")
        }
        else if (TID == -1) {
            throw new Exception("No task has been created yet.")
        }
        else{
            RSActor ! Server_EH_1(TID, serializedOp(func))
            this
        }
    }

    def RecoverWith(func: SerializableFunction_3[T]): RemoteTask[T] = {
        if (head) {
            throw new Exception("This RecoverWith should be only called for the rest tasks")
        }
        else if (TID == -1) {
            throw new Exception("No task has been created yet.")
        }
        else{
            RSActor ! Server_EH_2(TID, serializedOp(func))
            this
        }
    }


    def SendToActor(actor: ActorRef): RemoteTask[T] = {
        if (TID == -1) {
            throw new Exception("No task has been created yet.")
        }
        else{
            RSActor ! RequestResult(TID, actor)
            this
        }
    }


    def CanItWait: Boolean = canWait


    def GetCurrentWorker: ActorRef = ???


    def GetResult: T = Result match {
        case Some(r) => r
        case None => throw new Exception("The task has not been completed yet.")
    }

    def GetResultByActor: Unit = ???


    def IsCompleted: Boolean = isDone


    def Stop: Unit = {
        if (TID != -1) {
            RSActor ! KillTask(TID)
        }else throw new Exception("No task has been created yet.")
    }

    def serializedOp(op: SerializableFunction_1[T]): Array[Byte] = {
        val byteStream = new ByteArrayOutputStream()
        val objectStream = new ObjectOutputStream(byteStream)
        objectStream.writeObject(op)
        objectStream.flush()
        byteStream.toByteArray()
    }


    def serializedOp[U](op: SerializableFunction_2[T, U]): Array[Byte] = {
        val byteStream = new ByteArrayOutputStream()
        val objectStream = new ObjectOutputStream(byteStream)
        objectStream.writeObject(op)
        objectStream.flush()
        byteStream.toByteArray()
    }

    def serializedOp(op: SerializableFunction_3[T]): Array[Byte] = {
        val byteStream = new ByteArrayOutputStream()
        val objectStream = new ObjectOutputStream(byteStream)
        objectStream.writeObject(op)
        objectStream.flush()
        byteStream.toByteArray()
    }


    def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {

        this.synchronized{

            if (tempActor == null) {
                canWait = true
                tempActor = context.actorOf(TempActor.props(RSActor))
            }
            if (!isDone) {
                try{
                    this.wait(atMost.toMillis)
                }catch{
                    case e: Exception =>
                        println(e)
                
                }
            }
        }
        if (isDone) {
            this
        }else{
            println(s"Cannot get the result in $atMost time")
            this
        }
    }


    def result(atMost: Duration)(implicit permit: CanAwait): T = {

        this.synchronized{

            if (tempActor == null) {
                canWait = true
                tempActor = context.actorOf(TempActor.props(RSActor))
            }
            if (Result.isEmpty) {
                try{
                    this.wait(atMost.toMillis)
                }catch{
                    case e: Exception =>
                        println(e)
                
                }
            }
        }
        if (isDone) {
            Result.get
        }else{
            println(s"Cannot get the result in $atMost time")
            None.asInstanceOf[T]
        }
    }



    private [this] def setresult(r: T): Unit = {
        this.synchronized {
            Result = Some(r)
            isDone = true
            this.notifyAll() // or this.notify() wake up a single thread
        }
    }



    object TempActor{
        def props(remoteA: ActorSelection): Props = Props(new TempActor(remoteA))
    }


    class TempActor(worker: ActorSelection) extends Actor with ActorLogging{

        import RemoteSeverActor._

        override def preStart(): Unit = {
            if (worker != null) {
                worker ! RequestResult(TID, self)
            }
        }
        def receive = {
            case out =>
                // log.info(this.context.parent.path.name)
                out match{
                    case x: Exception =>
                        Result = None
                        tempActor = null
                    case _ =>

                        setresult(out.asInstanceOf[T])
                }
                this.context.stop(self)
        }



    }


}