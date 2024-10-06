package CollectResult

import Actors._
import akka.actor._
import scala.concurrent.duration.Duration
import scala.concurrent.{Awaitable, CanAwait}
import scala.concurrent.Await

import Tasks._
import Actors.FirstWorker.AskResult

class Collect(creator: ActorRef, can_Wait: Boolean = false)(implicit context: ActorContext) extends ResultCollect{

    import Aggregate_Actor._
    val aggregate_actor = context.actorOf(Aggregate_Actor.props(creator))

    final private var result: Option[Map[Int, Any]] = None
    final private var isDone: Boolean = false
    final private var isTimeout: Boolean = false
    final private var callbacks: List[() => Unit] = Nil
    final private var canWait: Boolean = can_Wait

    // Create a temp actor to get the result from the current actor
    private var tempActor = if (canWait) context.actorOf(TempActor.props(aggregate_actor)) else null


    private [this] def registerCallback(callback: () => Unit): Unit = {
        callbacks = callback :: callbacks
    }


    private [this] def exec_callbacks(): Unit = {
        callbacks.foreach(callback => callback())
    }


    def LookingFor(task: LocalTask[_], w_time: Duration): this.type = {
        if (isDone){
            throw new Exception("Collect has been done")
        }
        aggregate_actor ! Find(task, w_time, 0)
        aggregate_actor ! sendFinish
        this
    }
    
    def LookingFor(task: Seq[LocalTask[_]], w_time: Duration): this.type = {
        if (isDone){
            throw new Exception("Collect has been done")
        }
        for (i <- 0 until task.length){
            aggregate_actor ! Find(task(i), w_time, i)
        }
        aggregate_actor ! sendFinish
        this
    }

    def Anyof(task: Seq[LocalTask[_]], w_time: Duration): this.type = {
        if (isDone){
            throw new Exception("Collect has been done")
        }
        val canWaitTasks = task.filter(_.CanItWait)
        if (canWaitTasks.length > 0) aggregate_actor ! FirstCompleted(canWaitTasks, w_time)
        this
    }

    
    def PrintResult: Unit = {
        if (!isDone){
            registerCallback(() => PrintResult)
            println("Collect: Waiting for the result")
        }else if (isTimeout){
            println("Collect: Timeout")}
        else{
            println(s"Collect - Result: ${result.get}")
        }
    }


    def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
        this.synchronized{

            if (tempActor == null) {
                canWait = true
                tempActor = context.actorOf(TempActor.props(aggregate_actor))
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


    def result(atMost: Duration)(implicit permit: CanAwait): Map[Int, Any] = {
        this.synchronized{

            if (tempActor == null) {
                canWait = true
                tempActor = context.actorOf(TempActor.props(aggregate_actor))
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
            Map.empty[Int, Any]
        }
    }



    private [this] def setresult(r: Map[Int, Any]): Unit = {
        this.synchronized {
            result = Some(r)
            isDone = true
            this.notifyAll() // Notify all the threads that are waiting for the result
        }
    }


    override def toString: String = {
        if (!isDone){
            "Collect < NotComplete >"
        }else if (isTimeout){
            "Collect < Timeout >"}
        else{
            "Collect < Complete >"
        }
    }


    def sendToActor(recipient: ActorRef): this.type = {
        aggregate_actor ! GetResult(recipient)
        this
    }


    object TempActor {

        def props(agg_actor: ActorRef): Props = Props(new TempActor(agg_actor)).withDispatcher("FJ-dispatcher")
    }

    class TempActor(agg_actor: ActorRef) extends Actor with ActorLogging{


        override def preStart(): Unit = {

            agg_actor ! GetResult(self)

        }
        def receive = {
            case out =>
                // log.info(s"Temp actor receive $out")
                out match
                    case x: Exception =>
                        isDone = true
                        exec_callbacks()
                    case _ =>
                        setresult(out.asInstanceOf[Map[Int, Any]])
                        isTimeout = if (result.get.isEmpty) true else false
                        exec_callbacks()

                this.context.stop(self)
        }
    }


}
     