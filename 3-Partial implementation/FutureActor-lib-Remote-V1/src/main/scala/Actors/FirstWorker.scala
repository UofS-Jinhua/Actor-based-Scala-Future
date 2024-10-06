package Actors

import akka.actor._
import Tasks._
import java.io.Serial


// Define the message for the actor: FirstWorker
object FirstWorker{

    case object Start

    // Message for the next actor
    case class AskResult(actor: ActorRef)
    case class NextActor(actor: ActorRef)
    case class RequiredValue[T](value: T)

    // Message for error handling

    case class Error_Handling_1[T](func: SerializableFunction_1[T])
    case class Default_Result[T](r: T)


    def props[T](creator: ActorRef, op: SerializableFunction_1[T]): Props = Props(new FirstWorker(creator, op))

}

class FirstWorker[T](creator: ActorRef, op: SerializableFunction_1[T]) extends Actor with Stash with ActorLogging {

    import FirstWorker._

    override def preStart(): Unit = {
        // Start the task once the actor is created
        self ! Start
    }


    def init: Receive = {
        case Start =>
            try{
                val result = op()
                context.become(done(result))
                unstashAll()
            } catch{
                case e: Exception =>
                    val e_msg = e.getMessage
                    log.info(s"FirstWorker Actor [$self] caught an Error: [$e_msg]")
                    creator ! e_msg
                    context.become(error_handling(e_msg))
                    unstashAll()
            }

        case NextActor(actor) =>
            stash()
        case AskResult(actor) =>
            stash()
        case Error_Handling_1(func) => 
            stash()
        case Default_Result(r) =>
            stash()
        case _ =>
            // ignore other messages
    }


    def error_handling(e: String): Receive = {
        case Error_Handling_1(func) =>
            try{
                val result = func().asInstanceOf[T]
                context.become(done(result))
                unstashAll()
            } catch{
                case e: Exception =>
                    val e_msg = e.getMessage
                    log.info(s"FirstWorker Actor [$self] caught an Error: [$e_msg] in error_handling")
                    creator ! e_msg
                    context.become(error_handling(e_msg))
            }
        case Default_Result(r) =>
            context.become(done(r.asInstanceOf[T]))
            unstashAll()

        case NextActor(actor) =>
            stash()
        case AskResult(actor) =>
            stash()
        case _ =>
            // ignore other messages

    }

    /*
    / The done behavior of the actor    
    / The actor will send the result to the next actor
    / @param result: T => the result of the task
    */
    def done(result: T): Receive = {
        case NextActor(actor) =>
            actor ! RequiredValue(result)
        case AskResult(actor) => 
            log.info(s"FirstWorker Actor [$self] send [$result] to [$actor]")
            actor ! result
    }

    def receive = init
}

