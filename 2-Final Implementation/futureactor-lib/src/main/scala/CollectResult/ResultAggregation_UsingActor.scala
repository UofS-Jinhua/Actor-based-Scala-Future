package CollectResult

import Actors._
import akka.actor._
import scala.concurrent.duration.Duration
import scala.concurrent.{Awaitable, CanAwait}
import scala.concurrent.Await

import Tasks._
import Actors.FirstWorker.AskResult
import akka.util.LineNumbers.Result

class ACollect(sendTo: ActorRef, can_wait: Boolean = false)(implicit context: ActorContext) extends ResultCollect{

    import Aggregate_Actor2._


    val aggregate_actor = context.actorOf(Aggregate_Actor2.props(sendTo).withDispatcher("FJ-dispatcher"))

    private var result: Option[Map[Int, Any]] = None
    private var isDone: Boolean = false
    private var isTimeout: Boolean = false
    private var callbacks: List[() => Unit] = Nil
    private var canWait: Boolean = can_wait

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
        aggregate_actor ! Waitfor(w_time)
        aggregate_actor ! Find(task, 0)
        aggregate_actor ! SendFinish
        aggregate_actor ! GetResult(sendTo)
        this
    }
    
    
    def LookingFor(task: Seq[LocalTask[_]], w_time: Duration): this.type = {
        if (isDone){
            throw new Exception("Collect has been done")
        }
        aggregate_actor ! Waitfor(w_time)
        for (i <- 0 until task.length){
            aggregate_actor ! Find(task(i), i)
        }
        aggregate_actor ! SendFinish
        aggregate_actor ! GetResult(sendTo)
        this
    }

    def Anyof(task: Seq[LocalTask[_]], w_time: Duration): this.type = {
        if (isDone){
            throw new Exception("Collect has been done")
        }
        aggregate_actor ! Waitfor(w_time)
        for (i <- 0 until task.length){
            aggregate_actor ! Oneof(task(i), i)
        }
        aggregate_actor ! GetResult(sendTo)
        this
    }

    
    def PrintResult: Unit = {
        if (!isDone){
            registerCallback(() => PrintResult)
            println("Waiting for the result")
        }else if (isTimeout){
            println("Timeout")}
        else{
            println(s"ACollect - Result: ${result.get}")
        }
    }



    def sendToActor(actor: ActorRef): this.type = {
        aggregate_actor ! GetResult(actor)
        this
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


    // def Result: Seq[(Int, Any)] = {
    //     if (!isDone){
    //         println("Waiting for the result")
    //         Seq.empty[(Int, Any)]
    //     }else if (isTimeout){
    //         println("Timeout")
    //         Seq.empty[(Int, Any)]}
    //     else{
    //         result.get.toSeq.sortBy(_._1)
    //     }
    // }


    override def toString: String = {
        if (!isDone){
            "ACollect < NotComplete >"
        }else if (isTimeout){
            "ACollect < Timeout >"}
        else{
            "ACollect < Complete >"
        }
    }



    private [this] def setResult(new_result: Map[Int, Any]): Unit = {
        this.synchronized {
            isDone = true
            result = Some(new_result)
            this.notifyAll() // or this.notify() wake up a single thread
        }

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
                out match
                    case x: Exception =>
                        isDone = true
                        exec_callbacks()
                    case x: Map[_, _] =>
                        // println(s"get result $out, now change result value")
                        setResult(out.asInstanceOf[Map[Int, Any]])
                        exec_callbacks()
                    case x: String =>
                        // println(s"get result $out")
                        isDone = true
                        isTimeout = true
                        exec_callbacks()

                this.context.stop(self)
        }
    }

}
