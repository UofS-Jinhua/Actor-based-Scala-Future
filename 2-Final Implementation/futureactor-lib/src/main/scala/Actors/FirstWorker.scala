package Actors

import akka.actor._


// Define the message for the actor: FirstWorker
object FirstWorker{
    // Message for starting the task and getting the result
    case object Start
    case object IsDone
    case object GetCurrentResult

    // Message for the next actor
    case class AskResult(actor: ActorRef)
    case class NextActor(actor: ActorRef)
    case class RequiredValue[T](value: T)

    // Message for error handling
    case class Error_Handling_1[T](func: () => T)
    case class Default_Result[T](r: T)


    def props[T](creator: ActorRef, op: => T): Props = Props(new FirstWorker(creator, op))
    .withDispatcher("FJ-dispatcher")
}

/* 
/ FirstWorker: the first worker actor, which is the first actor in the chain of actors
/ @param creator: the actor who creates this actor
/ @param op: the operation that this actor will do
 */
class FirstWorker[T](creator: ActorRef, op: => T) extends Actor with Stash with ActorLogging {

    import FirstWorker._

    val recipient = creator 

    override def preStart(): Unit = {
        // Start the task once the actor is created
        self ! Start
    }

    // The initial behavior of the actor
    // The actor will start the task and become the done state
    // If the task fails, the actor will become the error_handling state
    def init: Receive = {
        case Start =>
            try{
                val result = op
                context.become(done(result))
                unstashAll()
            } catch{
                case e: Exception =>
                    val e_msg = e.getMessage
                    log.info(s"FirstWorker Actor [$self] caught an Error: [$e_msg]")
                    recipient ! e_msg
                    context.become(error_handling(e_msg))
                    unstashAll()
            }

        case NextActor(actor) =>
            stash()
        case IsDone =>
            recipient ! false
        case GetCurrentResult =>
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

    /* 
    / The error_handling behavior of the actor
    / The actor will try to recover from the error by calling the function func
    / If the task fails again, the actor will become the error_handling state again
    / If the task succeeds, the actor will become the done state
    / @param e: String => the error message
    */
    def error_handling(e: String): Receive = {
        case GetCurrentResult =>
            recipient ! e
        case Error_Handling_1(func) =>
            try{
                val result = func().asInstanceOf[T]
                context.become(done(result))
                unstashAll()
            } catch{
                case e: Exception =>
                    val e_msg = e.getMessage
                    log.info(s"FirstWorker Actor [$self] caught an Error: [$e_msg] in error_handling")
                    recipient ! e_msg
                    context.become(error_handling(e_msg))
            }
        case Default_Result(r) =>
            context.become(done(r.asInstanceOf[T]))
            unstashAll()

        case NextActor(actor) =>
            stash()
        case IsDone =>
            recipient ! false
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
        case IsDone =>
            recipient ! true
        case GetCurrentResult => 
            recipient ! result
        case AskResult(actor) => 
            actor ! result
    }

    def receive = init
}

