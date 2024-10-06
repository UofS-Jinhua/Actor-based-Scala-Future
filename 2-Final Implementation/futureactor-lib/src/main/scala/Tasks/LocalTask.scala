package Tasks

import Actors._
import akka.actor._
import scala.concurrent.duration.Duration
import scala.concurrent.{Awaitable, CanAwait}
import Test.future_main2.temp

class LocalTask[T](creator: ActorRef, prev_worker: ActorRef = null, cur_worker: ActorRef = null, canAwait: Boolean = false)
                 (implicit context: ActorContext) extends Task[T] {

    import FirstWorker._
    import NextWorker._
    import CombinedWorker._

    final private var isStopped: Boolean = false
    final private var result: Option[T] = None
    final private var isDone: Boolean = false
    final private var canWait: Boolean = canAwait

    // Create a temp actor to get the result from the current actor
    final private var tempActor = if (canWait && cur_worker != null) context.actorOf(TempActor.props(cur_worker)) else null


    def Start(op: => T): LocalTask[T] = {
        if (prev_worker == null && cur_worker == null) {
            val worker = context.actorOf(FirstWorker.props(creator, op))
            new LocalTask[T](creator, null, worker)
        }else{
            throw new Exception("Start should be called only once")
        }
    }


    def Zip[U](that: LocalTask[U]): LocalTask[(T, U)] = {
        if (cur_worker == null) {
            throw new Exception("Cannot zip with an empty task")
        }
        else {
            val new_worker = context.actorOf(CombinedWorker.props(creator, cur_worker, that.GetCurrentWorker))
            new LocalTask[(T, U)](creator,null, new_worker)
        }
    }


    def Group[T](tasks: Seq[LocalTask[T]]): LocalTask[List[T]] = {
        if (prev_worker == null && cur_worker == null) {
            val groupActor = context.actorOf(GroupedWorker.props(creator, tasks.map(_.GetCurrentWorker).toList))
            new LocalTask[List[T]](creator, null, groupActor)
        }else{
            throw new Exception("Start should be called only once")
        }
    }



    def NextStep[U](op2: T => U): LocalTask[U] = {
        if (prev_worker == null && cur_worker == null) {
            throw new Exception("First task has not been created, Start() should be called first")
        }
        else{
            val new_worker = context.actorOf(NextWorker.props(creator, cur_worker, op2))
            new LocalTask[U](creator,cur_worker, new_worker)
        }
    }

    def IsCompleted: Boolean = {
        isDone
    }

    def CanItWait: Boolean = {
        canWait
    }

    def GetResult: T = {
        if (cur_worker == null) {
            throw new Exception("No task has been created yet.")
        }
        else if (!isDone){
            throw new Exception("Task has not been completed yet.")
        }else{
            result.get
        }
    }


    def GetResultByActor: Unit = {
        if (cur_worker == null) {
            throw new Exception("No task has been created yet.")
        }else{
            cur_worker ! GetCurrentResult
        }
    }

    def Stop: Unit = {
        if (cur_worker != null && !isStopped) {
            context.stop(cur_worker)
            isStopped = true
        }
        else{
            throw new Exception("No task has been created yet.")
        }
    }

    def Recover(default_r: T): LocalTask[T] = {
        if (cur_worker == null) {
            throw new Exception("No task has been created yet.")
        }
        else{
            cur_worker ! Default_Result(default_r)
            this
        }

    }

    def RecoverWith(func:() => T): LocalTask[T] = {
        if (prev_worker != null) {
            throw new Exception("This RecoverWith should be only called for the first task")
        }
        else if (cur_worker == null) {
            throw new Exception("No task has been created yet.")
        }
        else{
            cur_worker ! Error_Handling_1(func)
            this
        }
    }

    def RecoverWith(func: Any => T): LocalTask[T] = {
        if (prev_worker == null) {
            throw new Exception("This RecoverWith should be only called for the rest tasks")
        }
        else if (cur_worker == null) {
            throw new Exception("No task has been created yet.")
        }
        else{
            cur_worker ! Error_Handling_2(func)
            this
        }
    }

    def GetCurrentWorker: ActorRef = {
        if (cur_worker == null) {
            throw new Exception("No task has been created yet.")
        }
        else{
            cur_worker
        }
    }


    def SendToActor(actor: ActorRef): LocalTask[T] = {
        if (cur_worker == null) {
            throw new Exception("No task has been created yet.")
        }
        else{
            cur_worker ! AskResult(actor)
            this
        }
    }


    def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {

        this.synchronized{

            if (tempActor == null) {
                canWait = true
                tempActor = context.actorOf(TempActor.props(cur_worker))
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
                tempActor = context.actorOf(TempActor.props(cur_worker))
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
            result.get
        }else{
            println(s"Cannot get the result in $atMost time")
            None.asInstanceOf[T]
        }
    }


    private [this] def setresult(r: T): Unit = {
        this.synchronized {
            result = Some(r)
            isDone = true
            this.notifyAll() // or this.notify() wake up a single thread
        }
    }



    object TempActor{
        def props(wa: ActorRef): Props = Props(new TempActor(wa)).withDispatcher("FJ-dispatcher")
    }


    class TempActor(worker: ActorRef) extends Actor with ActorLogging{

        override def preStart(): Unit = {
            if (worker != null) {
                worker ! AskResult(self)
            }
        }
        def receive = {
            case out =>
                // log.info(this.context.parent.path.name)
                out match
                    case x: Exception =>
                        isDone = true
                        tempActor = null
                    case _ =>
                        setresult(out.asInstanceOf[T])

                this.context.stop(self)
        }



    }
}