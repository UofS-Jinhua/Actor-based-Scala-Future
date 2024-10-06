package Actors

import akka.actor._

// Define the message for the actor: CombinedWorker
object CombinedWorker{
    

    def props[T, U](creator: ActorRef, first: ActorRef, second: ActorRef): Props = 
        Props(new CombinedWorker(creator, first, second))

}

/*
/ CombinedWorker: the actor which combines the results from two actors
/ @param creator: the actor who creates this actor chain
/ @param first: the first actor
/ @param second: the second actor which will be combined with the first actor
*/
class CombinedWorker[T, -U](creator: ActorRef, first: ActorRef, second: ActorRef) extends Actor with Stash with ActorLogging {

    import FirstWorker._
    import NextWorker._
    import CombinedWorker._

    val recipient = creator
    val first_worker = first
    val second_worker = second

    override def preStart(): Unit = {
        // Get results from the two actors once the actor is created
        first_worker ! AskResult(self)
        second_worker ! AskResult(self)
    }

    /*
    / The initial behavior of the actor
    / The actor will get the results from the two actors, combine the results, and become the done state
    */
    def init(Result1: Option[T] = None, Result2: Option[U] = None): Receive = {

        case NextActor(actor) =>
            stash()
        case AskResult(actor) => 
            stash()
        case msg => 
            
            val senderRef = sender()

            if (senderRef == first_worker) {
                val result = msg.asInstanceOf[T]
                if (Result2 != None){
                    context.become(Done((result, Result2.get)))
                    unstashAll()
                }
                else{
                    context.become(init(Some(result), Result2))
                }

            } else if (senderRef == second_worker) {
                val result = msg.asInstanceOf[U]
                if (Result1 != None){
                    context.become(Done((Result1.get, result)))
                    unstashAll()
                }
                else{
                    context.become(init(Result1, Some(result)))
                }

            }
    }

    /*
    / The done behavior of the actor
    / The actor will send the result to the actor who asked for the result
    / @param result: (T, U) => the result of the task
    */
    def Done(result: (T, U)): Receive = {
        case NextActor(actor) =>
            actor ! RequiredValue(result)

        case AskResult(actor) => 
            actor ! result
    }

    def receive = init()
}
