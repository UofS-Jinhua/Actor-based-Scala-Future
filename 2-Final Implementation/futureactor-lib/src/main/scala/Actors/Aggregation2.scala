package Actors

import Tasks._
import akka.actor._
import scala.concurrent.duration._



// Define the message for the actor: Aggregate_Actor2
object Aggregate_Actor2{

    case class Find(LocalTask: LocalTask[_], id: Int)       // message: Find the result of the task
    case class Oneof(LocalTask: LocalTask[_], id: Int)      // message: Find the first finished task
    case class GetResult(askActor: ActorRef)            // message: Get the result from the worker actor
    case class Waitfor(time: Duration)                  // message: Wait for the result for a certain time
    case object SendFinish                              // message: End of the requests of task result
    case object TimeoutNow                              // message: Timeout

    def props[T](creator: ActorRef): Props = 

        Props(new Aggregate_Actor2(creator))
        .withDispatcher("FJ-dispatcher")
}


/* 
/ Aggregate_Actor2: the actor that gathers results from the worker actor(s)
/ @param creator: the actor who creates this actor
*/
class Aggregate_Actor2[T](creator: ActorRef) extends Actor with ActorLogging with Stash {

    import Aggregate_Actor2._
    import FirstWorker._

    val recipient = creator



    /* 
    / The initial behavior of the actor
    / The actor will send request of asking the result to the worker actor
    /
    / @param TaskMap: Map[ActorRef, Int] => 
                      the map that pairs the worker actor and the id of the task
    */
    def Aggregating(TaskMap: Map[ActorRef, Int] = Map.empty[ActorRef, Int]): Receive = {

        case Find(task, id) =>

            val worker = task.GetCurrentWorker
            worker ! AskResult(self)
            context.become(Aggregating(TaskMap + (worker -> id)))
        
        case Oneof(task, id) =>
            val worker = task.GetCurrentWorker
            worker ! AskResult(self)
            context.become(Anyof(TaskMap + (worker -> id)))

        case GetResult(askActor) =>
            stash()

        case SendFinish =>
            context.become(WaitingResponse(TaskMap))
            unstashAll()
        
        case Waitfor(time) => 
            if (time.isFinite){
                val finiteTime: FiniteDuration = FiniteDuration(time.toNanos, NANOSECONDS)
                context.system.scheduler.scheduleOnce(finiteTime, self, TimeoutNow)(context.dispatcher, self)
            }

        case TimeoutNow => 
            context.become(Run_out_of_Time)
            unstashAll()

        case msg =>
            stash()
    }


    /*
    / The Anyof behavior of the actor
    /       - Aggregate_actor will store the result of the first finished task
    /
    / @param TaskMap: Map[ActorRef, Int] => 
                      the map that pairs the worker actor and the id of the task
    / @param ResultMap: Map[Int, Any] =>
    /                 the map that pairs the id of the task and the result            
    */
    def Anyof(TaskMap: Map[ActorRef, Int], 
              ResultMap: Map[Int, Any] = Map.empty[Int, Any]): Receive = {

        case GetResult(askActor) =>
            stash()

        case TimeoutNow => 
            context.become(Run_out_of_Time)
            unstashAll()
        
        case Oneof(task, id) =>
            val worker = task.GetCurrentWorker
            worker ! AskResult(self)
            context.become(Anyof(TaskMap + (worker -> id), ResultMap))

        case msg =>
            val worker = sender()
            val result = msg
            // log.info(s"Aggregate_Actor receive $result from $worker, ID: $id")
            if (TaskMap.contains(worker)){
                val id = TaskMap(worker)
                context.become(Done(ResultMap + (id -> result)))
                unstashAll()
            }
    }


    /*
    / The WaitingResponse behavior of the actor
    /       - Aggregate_actor will wait for all the responses, and store them. 
    /
    / @param TaskMap: Map[ActorRef, Int] => 
                      the map that pairs the worker actor and the id of the task
    / @param ResultMap: Map[Int, Any] => 
                      the map that pairs the id of the task and the result
    */
    def WaitingResponse(TaskMap: Map[ActorRef, Int], 
                        ResultMap: Map[Int, Any] = Map.empty[Int, Any]): Receive = {

        case GetResult(askActor) =>
            stash()

        case TimeoutNow => 
            context.become(Run_out_of_Time)
            unstashAll()

        case msg =>

            
            val worker = sender()
            val id = TaskMap(worker)
            val result = msg
            // log.info(s"Aggregate_Actor receive $result from $worker, ID: $id")
            if (TaskMap.contains(worker) && TaskMap.size > 1){

                context.become(WaitingResponse(TaskMap - worker, ResultMap + (id -> result)))

            } else {
                // log.info(s"Aggregate_Actor receive Final Result")
                context.become(Done(ResultMap + (id -> result)))
                unstashAll()
            }
    }



    /*
    / The Done behavior of the actor
    /      - Aggregate_actor will send the result to the creator actor and those who ask for the result
    /
    / @param ResultMap: Map[Int, Any] => 
                      the map that pairs the id of the task and the result
    */
    def Done(ResultMap: Map[Int, Any]): Receive = {

        case GetResult(askActor) =>
            // log.info(s"Aggregate_Actor receive GetResult($askActor)")
            // println(ResultMap)
            askActor ! ResultMap
        
        case _: Any => 
    }  


    /*
    / The Run_out_of_Time behavior of the actor
    /     - Aggregate_actor didnt receive the result in time
    */
    def Run_out_of_Time: Receive = {

        case GetResult(askActor) =>
            askActor ! "Timeout"
            
        case _ =>
    } 

    def receive = Aggregating()
}