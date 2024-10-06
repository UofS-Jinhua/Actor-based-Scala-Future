package Actors

import akka.actor._


// Define the message for the actor: FirstWorker
object Worker{

    // -------------------------------------------------------------------------------------------------------

    // Message for identifying the Worker type
    case object FirstWorker
    case object NextWorker
    case object ZipWorker
    case object GroupWorker


    // Message for FirstWorker
    case class Start[T](op: () => T)
    case class IsDone(recipient: ActorRef)
    case class Error_Handling_1[T](func: () => T)           // Message for error handling (FirstWorker)
    case class Default_Result[T](r: T)

    case class AskResult(actor: ActorRef)
    case class NextActor(actor: ActorRef)


    // Message for the next actor
    case class SubTask[U](op: Any => U)
    case class RequiredValue[T](value: T)
    case class GetPre_R(Pre_worker: ActorRef)               // Get the previous result from the previous actor
    case class Error_Handling_2[U](func: Any => U)          // Message for error handling (NextWorker)
    case class Default_Result_2[U](r: U)                


    // Message for the ZipWorker
    case class GetResults(actor1: ActorRef, actor2: ActorRef)


    // Message for the GroupWorker
    case class FromWorkers(workers: List[ActorRef])



// -------------------------------------------------------------------------------------------------------

    // Props for creating the Worker actor
    def props(): Props = Props(new Worker())
}

/* 
/ Worker: the worker actor, which is the actor in the chain of actors
- FirstWorker: the first actor in the chain
- NextWorker: the next actor in the chain
- ZipWorker: the actor that will zip the results from the given 2 actors
- GroupWorker: the actor that will group the results from the given actors into a list with the same order, the result type of the actors should be the same
 */
class Worker[T, -U]() extends Actor with Stash with ActorLogging {

    import Worker._

    // override def preStart(): Unit = {
    //     // Start the task once the actor is created
    //     // self ! Start
    // }


    def init: Receive = {
        case FirstWorker =>
            context.become(init_FirstWorker)
            unstashAll()
        case NextWorker =>
            context.become(init_NextWorker())
            unstashAll()
        case ZipWorker =>
            context.become(init_ZipWorker())
            unstashAll()
        case GroupWorker =>
            context.become(init_GroupWorker)
            unstashAll()
        case _ =>
            stash()
    }









    // The initial behavior of the FirstWorker actor
    // The actor will start the task and become the done state
    // If the task fails, the actor will become the error_handling state
    def init_FirstWorker[T]: Receive = {
        case Start(op) =>
            try{
                val result = op()
                context.become(done_FirstWorker(result))
                unstashAll()

            } catch{

                case e: Exception =>
                    val e_msg = e.getMessage
                    log.info(s"FirstWorker Actor [$self] caught an Error: [$e_msg]")
                    
                    context.become(error_handling_FirstWorker(e_msg))
                    unstashAll()
            }

        case NextActor(actor) =>
            stash()
        case AskResult(actor) =>
            stash()
        case IsDone(rec) =>
            rec ! false
        case Error_Handling_1(func) => 
            stash()
        case Default_Result(r) =>
            stash()
        case _ =>
            // ignore other messages
    }

    /* 
    * The error_handling behavior of the FirstWorker actor
    * The actor will try to recover from the error by calling the function func
    * If the task fails again, the actor will become the error_handling state again
    * If the task succeeds, the actor will become the done state
    * @param e: String => the error message
    */
    def error_handling_FirstWorker(e: String): Receive = {

        case Error_Handling_1(func) =>
            try{
                val result = func()
                context.become(done_FirstWorker(result))
                unstashAll()
            } catch{
                case e: Exception =>
                    val e_msg = e.getMessage
                    log.info(s"FirstWorker Actor [$self] caught an Error: [$e_msg] in error_handling")
                    
                    context.become(error_handling_FirstWorker(e_msg))
            }
        case Default_Result(r) =>
            context.become(done_FirstWorker(r))
            unstashAll()

        case NextActor(actor) =>
            stash()
        case IsDone(rec) =>
            rec ! false
        case AskResult(actor) =>
            stash()
        case _ =>
            // ignore other messages

    }

    /*
    * The done behavior of the FirstWorker actor  
    * The actor will send the result to the next actor
    * @param result: Any => the result of the task
    */
    def done_FirstWorker(result: Any): Receive = {
        case NextActor(actor) =>
            actor ! RequiredValue(result)
        case IsDone(rec) =>
            rec ! true

        case AskResult(actor) => 
            actor ! result
    }







    /* 
    * The initial behavior of the NextWorker actor
    * The actor will get the previous result from the previous actor
    * @param p_result: Any => the previous result
    */
    def init_NextWorker(p_result: Any = null): Receive = {

        case GetPre_R(prev_worker) =>
            prev_worker ! NextActor(self)

        case NextActor(actor) =>
            stash()

        case IsDone(rec) =>
            rec ! false

        case AskResult(actor) => 
            stash()

        case RequiredValue(value) => 
            context.become(init_NextWorker(value))
            unstashAll()

        case SubTask(op) =>

            if (p_result == null) {
                stash()

            }else{

                try {
                    val result = op(p_result)
                    context.become(done_NextWorker(result))
                    unstashAll()
                } catch{
                    case e: Exception =>
                        val e_msg = e.getMessage
                        log.info(s"NextWorker Actor [$self] caught an Error: [$e_msg]")
                        
                        context.become(error_handling_NextWorker(e_msg, p_result))
                }

            }

    }

    /*
    * The error_handling behavior of the NextWorker actor
    * The actor will try to recover from the error by calling the function func
    * If the task fails again, the actor will become the error_handling state again
    * If the task succeeds, the actor will become the done state
    * @param e: String => the error message
    * @param pre_value: Any => the previous result
    */
    def error_handling_NextWorker(e: String, pre_value: Any): Receive = {

        case NextActor(actor) => 
            stash()
        case Error_Handling_2(func) =>
            try{
                val result = func(pre_value)
                context.become(done_NextWorker(result))
                unstashAll()
            } catch{
                case e: Exception =>
                    val e_msg = e.getMessage
                    log.info(s"NextWorker Actor [$self] caught an Error: [$e_msg] in error_handling")
   
                    context.become(error_handling_NextWorker(e_msg, pre_value))
            }
        case Default_Result_2(r) =>
            context.become(done_NextWorker(r))
            unstashAll()
    }

    /*
    * The done behavior of the actor
    * The actor will send the result to the next actor
    * @param result: U => the result of the task
    */
    def done_NextWorker(result: Any): Receive = {
        case NextActor(actor) =>
            actor ! RequiredValue(result)
        case IsDone(rec) => 
            rec ! true
        case AskResult(actor) => 
            actor ! result
    }



// -------------------------------------------------------------------------------------------------------





    /*
    * The initial behavior of the ZipWorker actor
    * The actor will get the results from the two actors, combine the results, and become the done state
    * @param Result1: Option[Any] => the result from the first actor
    * @param Result2: Option[Any] => the result from the second actor
    * @param first_worker: ActorRef => the first actor
    * @param second_worker: ActorRef => the second actor
    * 
    */
    def init_ZipWorker(Result1: Option[Any] = None, Result2: Option[Any] = None, first_worker: ActorRef = null, second_worker: ActorRef = null): Receive = {

        case GetResults(actor1, actor2) =>
            actor1 ! AskResult(self)
            actor2 ! AskResult(self)
            context.become(init_ZipWorker(Result1, Result2, actor1, actor2))
        
        case NextActor(actor) =>
            stash()
        case IsDone(rec) =>
            rec ! false

        case AskResult(actor) => 
            stash()
        case msg => 
            
            val senderRef = sender()

            if (senderRef == first_worker) {
                val result = msg
                if (Result2 != None){
                    context.become(done_ZipWorker((result, Result2.get)))
                    unstashAll()
                }
                else{
                    context.become(init_ZipWorker(Some(result), Result2, first_worker, second_worker))
                }

            } else if (senderRef == second_worker) {
                val result = msg
                if (Result1 != None){
                    context.become(done_ZipWorker((Result1.get, result)))
                    unstashAll()
                }
                else{
                    context.become(init_ZipWorker(Result1, Some(result), first_worker, second_worker))
                }

            }
    }

    /*
    * The done behavior of the ZipWorker actor
    * The actor will send the result to the actor who asked for the result
    * @param result: (Any, Any) => the result of the task
    */
    def done_ZipWorker(result: (Any, Any)): Receive = {
        case NextActor(actor) =>
            actor ! RequiredValue(result)
        case IsDone(rec) => 
            rec ! true
        case AskResult(actor) => 
            actor ! result
    }





// -------------------------------------------------------------------------------------------------------


    /**
    * The initial behavior of the GroupWorker actor 
    * - The actor will get the list of workers and ask for the results from the workers
    * - A map will be created to store the results from the workers
    * - The actor will become the collecting state
    */
    def init_GroupWorker: Receive = {

        case FromWorkers(workers) =>
            for (worker <- workers) {
                worker ! AskResult(self)
            }

            val result_map: Map[Int, Any] = (1 to workers.length).map(i => (i, None)).toMap

            context.become(collecting_GroupWorker(workers, result_map))
            unstashAll()


    }



    /**
     * The collecting behavior of the GroupWorker actor
     * - The actor will collect the results from the workers
     * - The actor will become the done state once all the results are collected
     * @param workers: List[ActorRef] the list of workers
     * @param ResultMap: Map[Int, Any] the map to store the results from the workers
     **/


    def collecting_GroupWorker(workers: List[ActorRef], ResultMap: Map[Int, Any], collected: Int = 0): Receive = {
    
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

                if (has_collected == workers.length) {
                    val sortedValues: List[Any] = (ResultMap + (index -> result)).toList.sortBy(_._1).map(_._2)
                    log.info(s"grouping result: $sortedValues")
                    context.become(done_GroupWorker(sortedValues))
                    unstashAll()
                }else{
                    context.become(collecting_GroupWorker(workers, ResultMap + (index -> result), has_collected))
                }
            }
    }

    /*
    * The done behavior of the GroupWorker actor
    * The actor will send the result to the actor who asked for the result
    * @param result: List[Any] the grouped results
    */
    def done_GroupWorker(result: List[Any]): Receive = {

        case NextActor(actor) =>
            actor ! RequiredValue(result)
        case IsDone(rec) => 
            rec ! true

        case AskResult(actor) => 
            actor ! result
        case Error_Handling_1(func) =>
        case Error_Handling_2(func) =>
        case Default_Result(r) => 
        case msg => 
            log.info(s"GroupedWorker Actor [$self] received an unexpected message [$msg] in Done")
    }



// -------------------------------------------------------------------------------------------------------

    def receive = init
}

