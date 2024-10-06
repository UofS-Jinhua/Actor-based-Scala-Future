package Actors

import akka.actor._

// Define the message for the actor: NextWorker
object NextWorker{
    case object GetPR                                   // Get the previous result from the previous actor
    case class Error_Handling_2[U](func: Any => U)     // Error handling message
    case class Default_Result_2[U](r: U)                // Default result message for error handling

    def props[T, U](creator: ActorRef, prev_wa: ActorRef, op: T => U): Props = Props(new NextWorker(creator, prev_wa, op))
    .withDispatcher("FJ-dispatcher")
}

/*
/ NextWorker: the next worker actor, which is the next actor in the chain of actors
/ @param creator: the actor who creates this actor chain
/ @param prev_wa: the previous actor
/ @param op: the operation that this actor will do
*/
class NextWorker[T, -U](creator: ActorRef, prev_wa: ActorRef, op: T => U) extends Actor with Stash with ActorLogging {

    import FirstWorker._
    import NextWorker._

    val recipient = creator
    val prev_worker = prev_wa

    override def preStart(): Unit = {
        // Get the previous result once the actor is created
        self ! GetPR
    }

    /*
    / The initial behavior of the actor
    / The actor will get the previous result, do the execution, and become the done state
    / If the task fails, the actor will become the error_handling state
    */
    def init: Receive = {
        case GetPR =>
            prev_worker ! NextActor(self)
        case NextActor(actor) =>
            stash()
        case IsDone =>
            recipient ! false
        case GetCurrentResult => 
            stash()
        case AskResult(actor) => 
            stash()
        case RequiredValue(value) => 
            val pre_value = value.asInstanceOf[T]
            try {
                val result = op(pre_value)
                context.become(done(result))
                unstashAll()
            } catch{
                case e: Exception =>
                    val e_msg = e.getMessage
                    log.info(s"NextWorker Actor [$self] caught an Error: [$e_msg]")
                    recipient ! e_msg
                    context.become(error_handling(e_msg, pre_value))
            }
            val result = op(pre_value)
            context.become(done(result))
            unstashAll()
    }

    /*
    / The error_handling behavior of the actor
    / The actor will try to recover from the error by calling the function func
    / If the task fails again, the actor will become the error_handling state again
    / If the task succeeds, the actor will become the done state
    / @param e: String => the error message
    / @param pre_value: T => the previous result
    */
    def error_handling(e: String, pre_value: T): Receive = {
        case GetCurrentResult =>
            recipient ! e
        case NextActor(actor) => 
            stash()
        case Error_Handling_2(func) =>
            try{
                val result = func(pre_value).asInstanceOf[U]
                context.become(done(result))
                unstashAll()
            } catch{
                case e: Exception =>
                    val e_msg = e.getMessage
                    log.info(s"NextWorker Actor [$self] caught an Error: [$e_msg] in error_handling")
                    recipient ! e_msg
                    context.become(error_handling(e_msg, pre_value))
            }
        case Default_Result_2(r) =>
            context.become(done(r.asInstanceOf[U]))
            unstashAll()
    }

    /*
    / The done behavior of the actor
    / The actor will send the result to the next actor
    / @param result: U => the result of the task
    */
    def done(result: U): Receive = {
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
