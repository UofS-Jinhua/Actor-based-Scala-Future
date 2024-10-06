package TestingFeature

import akka.actor._ 
import scala.concurrent.duration._



// 3 layers of the task distribution system
//      1. UserActor
//      2. Work Distributor
//      3. Worker

// UserActor is the actor that receives the message from the user
// Work Distributor is the actor that distributes the work to the workers
// Worker is the actor that does the work

// The UserActor sends a message (work) to the Work Distributor
// The Work Distributor creates a Worker and sends the work to the Worker

//  - if the current work depends on the previous work, 
//    then the Work Distributor should wait for the previous work to finish 
//    before sending the current work to the Worker

// problem: 
    
//  - how to make the Work Distributor wait for the previous work to finish?
//  - how to make the Work Distributor know that the previous work has finished?

//  - how to make the user know that the previous work has finished, and the current work has started?

// ------------------------------------------------------------------------------------

object UserActor{

    case object Start_Work
    case object Wait_Failed
    case class Wait_Work(wait_time: FiniteDuration, id: Int)


    def props(wd: ActorRef):Props = Props(new UserActor(wd))
}


// User Actor will send a message to the Work Distributor to start the work for the user

//      - User actor could wait for the work to finish

class UserActor(wd: ActorRef) extends Actor with Stash with ActorLogging{

    import UserActor._
    import WorkDistributor._
    import context.dispatcher

    def Working(TaskMap: Map[Int, Any] = Map.empty[Int, Any]): Receive = {
        
        // TaskMap is a map that maps the task id to the result of the task
        //      - if the result is null, then the task is not finished


        case Start_Work => 

            wd ! Independ_Work((x: Int) => x + 1, 12)
        
        case Task_ID(id) => 

            if (!TaskMap.contains(id)){

                log.info("UserActor received a new task id: " + id.toString)
               context.become(Working(TaskMap + (id -> null)))
            }
            else{
                log.info("UserActor received a duplicate task id")
            }

        case Wait_Work(duration, id) => 
            context.system.scheduler.scheduleOnce(duration, self, Wait_Failed)
            context.become(Waiting(TaskMap, id))

        case TaskResult(id, result) =>

            if (TaskMap.contains(id)){

                log.info(s"Received the result [ $result ] of a task [ $id ]")
                context.become(Working(TaskMap + (id -> result)))
            }else{
                log.info(s"Received the result [ $result ] of a task [ $id ] that is not in the TaskMap")
            }
    }

    def Waiting(TaskMap: Map[Int, Any], Wait_task: Int): Receive = {

        case Start_Work => 

            stash()

        case TaskResult(id, result) =>

            if (TaskMap.contains(id)){

                if (id == Wait_task){

                    log.info(s"Waiting task Finished. Received the result [ $result ] of the wait task [ $id ]")
                    context.become(Working(TaskMap + (id -> result)))
                    unstashAll()
                }else{
                    log.info(s"Received the result [ $result ] of a task [ $id ] that is not the wait task")
                    context.become(Waiting(TaskMap + (id -> result), Wait_task))
                }
            }else{
                log.info(s"Received the result [ $result ] of a task [ $id ] that is not in the TaskMap")
            }


    }

    def receive = Working()
}



// ------------------------------------------------------------------------------------

object WorkDistributor{
    
    case class Task_ID(id: Int)
    case class TaskResult(id: Int, result: Any)

    case class Independ_Work[A, B](func: A => B, param: A)
    case class Independ_Work_withID[A, B](func: A => B, param: A, id: Int)

    case class Depend_Work[A, B](func:  Any => B, prevTaskID: Int)
    case class Depend_Work_withID[A, B](func:  Any => B, prevTaskID: Int, id: Int)

    case class AskResult(id: Int)

    case object GetDistributeMap

    def props():Props = Props(new WorkDistributor())
}

class WorkDistributor extends Actor with ActorLogging{

    import WorkDistributor._
    import context.dispatcher



    def Working(DistributeMap: Map[Int, (ActorRef, ActorRef, Boolean, Any)] = Map.empty[Int, (ActorRef, ActorRef, Boolean, Any)], 
                TaskID: Int = 0,
                RequestMap: Map[Int, List[ActorRef]] = Map.empty[Int, List[ActorRef]]): Receive = {

        // DistributeMap is a map that maps the task id to the tuple of (recipient, worker, isFinished, result)
        //     - key: task id
        //     - value: (recipient, worker, isFinished, result)
        //
        // TaskID 
        //      - is the id of the current task
        // RequestMap 
        //      - is a map that maps the task id to the list of actors that are waiting for the result of the task
        //      - key: task id
        //      - value: list of actors that are waiting for the result of the task

        case Independ_Work(func, arg) => 

            val id = TaskID + 1
            val workactor = context.actorOf(Worker.props(self))
            val recipient = sender

            recipient ! Task_ID(id)

            workactor ! Independ_Work_withID(func, arg, id)

            context.become(Working(DistributeMap + (id -> ((recipient, workactor, false, null))), id, RequestMap))


        case Depend_Work(func, previd) => 

            val id = TaskID + 1
            val workactor = context.actorOf(Worker.props(self))
            val recipient = sender

            workactor ! Depend_Work_withID(func, previd, id)

            context.become(Working(DistributeMap + (id -> ((recipient, workactor, false, null))), id, RequestMap))

        case TaskResult(id, result) => 

            if (DistributeMap.contains(id)){

                val values = DistributeMap(id)
                val recipient = values._1
                val isFinished = values._3

                if (! isFinished){

                    recipient ! TaskResult(id, result)

                    if (RequestMap.contains(id)){

                        val requesters = RequestMap(id)
                        requesters.foreach( _ ! TaskResult(id, result))

                        context.become(Working(DistributeMap + (id -> ((recipient, values._2, true, result))), TaskID, RequestMap - id))

                    }else{

                        context.become(Working(DistributeMap + (id -> ((recipient, values._2, true, result))), TaskID, RequestMap))
                    }

                }else{
                    log.info(s"Received the result [ $result ] of a task [ $id ] that is already finished")
                }
            }else{
                log.info(s"Received the result [ $result ] of a task [ $id ] that is not in the DistributeMap")
            }

        case AskResult(id) =>

            if (DistributeMap.contains(id)){

                val values = DistributeMap(id)
                val recipient = values._1
                val isFinished = values._3

                if (isFinished){

                    sender ! TaskResult(id, values._4)

                }else{

                    if (RequestMap.contains(id)){

                        val requesters = RequestMap(id)
                        context.become(Working(DistributeMap, TaskID, RequestMap + (id -> (sender :: requesters))))

                    }else{

                        context.become(Working(DistributeMap, TaskID, RequestMap + (id -> List(sender))))
                    }
                }
            }else{
                log.info(s"Received a request for the result of a task [ $id ] that is not in the DistributeMap")
            }
        
        case GetDistributeMap =>
            sender ! DistributeMap

    }

    def receive = Working()
}



// ------------------------------------------------------------------------------------

object Worker{

    def props(wd: ActorRef):Props = Props(new Worker(wd))
}

// Worker is the actor that does the work
//      - it receives the work from the Work Distributor
//      - it sends the result back to the Work Distributor
//      - once the work is done, it stops itself


class Worker(wd: ActorRef) extends Actor with ActorLogging{

    import Worker._
    import WorkDistributor._

    val workdistributor = wd

    def Init(ID: Int = -1): Receive = {

        case Independ_Work_withID(func, arg, id) => 

            context.become(Working(id))
            self ! Independ_Work(func, arg)


        case Depend_Work_withID(func, previd, id) => 

            context.become(Waiting(id))
            self ! Depend_Work(func, previd)

    }

    def Working(ID: Int): Receive = {

        case Independ_Work(f, param) => 
                
            val result = f(param)
            workdistributor ! TaskResult(ID, result)

            context.stop(self)
        
    }


    def Waiting[A, B](ID: Int, ReqResult: A = null, func: Option[A => B] = None): Receive = {

        case Depend_Work(f, prevId) => 

            workdistributor ! AskResult(prevId)
            context.become(Waiting(ID, null, Some(f)))
        
        case TaskResult(id, result) =>

            context.become(Working(ID))
            self ! Independ_Work(func.get, ReqResult)

            

    }


    def receive = Init()
}


// ------------------------------------------------------------------------------------


object main_test_in_PassingFunctionInActors extends App{

    import UserActor._
    import WorkDistributor._


    val system = ActorSystem("test_system")
    val wd = system.actorOf(WorkDistributor.props(), "WorkDistributor")
    val user = system.actorOf(UserActor.props(wd), "UserActor")

    user ! Start_Work

    // Thread.sleep(1000)

    // user ! Wait_Work(5.seconds, 1)

}
