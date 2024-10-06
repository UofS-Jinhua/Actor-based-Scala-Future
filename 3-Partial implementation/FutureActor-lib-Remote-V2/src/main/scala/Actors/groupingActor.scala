package Actors

import akka.actor._


// Define the actor: GroupedWorker
object GroupedWorker{

    def props(creator: ActorRef, tasks: List[ActorRef]): Props = 
        Props(new GroupedWorker(creator, tasks))

}

/**
* GroupedWorker: the actor which groups the results from several actors
* @param creator: the actor who creates this actor chain
* @param tasks: the actors which will be grouped
*/
class GroupedWorker(creator: ActorRef, Tasks: List[ActorRef]) extends Actor with Stash with ActorLogging {

    import FirstWorker._
    import NextWorker._
    import CombinedWorker._

    val recipient = creator
    val workers = Tasks
    val num_of_workers = workers.length

    override def preStart(): Unit = {
        // Start the task once the actor is created
        for (worker <- workers) {
            worker ! AskResult(self)
        }
        log.info(s"GroupedWorker Actor [$self] is created, List of workers: [$workers], length: [$num_of_workers]")
    }

    /**
    * The initial behavior of the actor
    * The actor will get the results from workes
    */
    def Collecting(ResultMap: Map[Int, Any] = (1 to num_of_workers).map(i => (i, None)).toMap, 
                   collected: Int = 0): Receive = {
    
        case NextActor(actor) =>
            stash()
        case Error_Handling_1(func) =>
        case Error_Handling_2(func) =>
        case Default_Result(r) => 
            stash()
        case AskResult(actor) => 
            stash()
        case msg => 

            val senderRef = sender()
            
            if (workers.contains(senderRef)) {
                val result = msg
                val index = workers.indexOf(senderRef) + 1
                val has_collected = collected + 1

                if (has_collected == num_of_workers) {
                    val sortedValues: List[Any] = (ResultMap + (index -> result)).toList.sortBy(_._1).map(_._2)
                    log.info(s"grouping result: $sortedValues")
                    context.become(Done(sortedValues))
                    unstashAll()
                }else{
                    context.become(Collecting(ResultMap + (index -> result), has_collected))
                }
            }
    }

    /*
    * The done behavior of the actor
    * The actor will send the result to the actor who asked for the result
    * @param result: List[Any] the grouped results
    */
    def Done(result: List[Any]): Receive = {

        case NextActor(actor) =>
            actor ! RequiredValue(result)

        case AskResult(actor) => 
            actor ! result
        case Error_Handling_1(func) =>
        case Error_Handling_2(func) =>
        case Default_Result(r) => 
        case msg => 
            log.info(s"GroupedWorker Actor [$self] received an unexpected message [$msg] in Done")
    }

    def receive = Collecting()
}
