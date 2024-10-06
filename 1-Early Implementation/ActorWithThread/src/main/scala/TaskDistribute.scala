import akka.actor._
import scala.collection.immutable._

import TaskManager._

object TaskDistribute{

    case class TPause(ta: ActorRef)

    case class TResume(ta: ActorRef)

    case class TKill(ta: ActorRef)

    case class TResult(out: Any, ta: ActorRef)

    case object Submitted_Task

    def props(): Props = Props(new TaskDistribute())

}

class TaskDistribute extends Actor{

    import TaskManager._
    import TaskDistribute._



    def TaskDistributing[A, B](RecipientMap: Map[ActorRef, List[ActorRef]] = Map.empty[ActorRef, List[ActorRef]] , 
                               TaskMap: Map[ActorRef, (A, Boolean, ActorRef)] = Map.empty[ActorRef, (A, Boolean, ActorRef)]): Receive ={

        // RecipientMap: Map[ActorRef, List[ActorRef]]
        //                         - key: the sender of the task
        //                         - value: the list of taskActor that the sender has sent the task to'
        // TaskMap: Map[ActorRef, (A, Boolean, ActorRef)]
        //                         - key: the taskActor
        //                         - value: (Result, isDone, sender)

        case Task(f, param) => 
            

            val taskActor = context.actorOf(TaskManager.props())
            taskActor ! Task(f, param)
            
            // update the matching taskActor to the sender, if sender is not in the map, get the empty list
            val updatedList = RecipientMap.get(sender()).getOrElse(List.empty) :+ taskActor

            context.become(TaskDistributing(RecipientMap + (sender() -> updatedList), 
                                            TaskMap + (taskActor -> (null, false, sender()) ) ) ) 


        case Result(out) =>

            if (TaskMap.contains(sender())){

                val recipient = TaskMap(sender())._3
                recipient ! TResult(out, sender())
                
                val updatedValue = (out, true, recipient)
                context.become(TaskDistributing(RecipientMap, TaskMap.updated(sender(), updatedValue)))

            }else{

                println("TaskDistribute: TaskMap doesn't contain the sender")
            }
        
        case CheckStatus =>

            //  get the list of taskActors that the sender has sent the task to
            //  get the status of the taskActors that are in the list
            //  return the statusMap: Map[ActorRef, Boolean]
            

            val taskActors = RecipientMap.getOrElse(sender(), List.empty)

            if (taskActors.isEmpty){

                sender() ! "No task submission from this sender"
            }else{

                val statusMap = taskActors.collect {
                    case taskActor if TaskMap.contains(taskActor) => (taskActor, TaskMap(taskActor)._2)
                }.toMap

                sender() ! statusMap
            }

        case Submitted_Task =>

            //  get the list of taskActors that the sender has sent the task to
            //  return the list of taskActors

            val taskActors = RecipientMap.getOrElse(sender(), List.empty)

            if (taskActors.isEmpty){

                sender() ! "No task submission from this sender"
            }else{

                val submittedTask = taskActors.collect {
                    case taskActor if TaskMap.contains(taskActor) => taskActor -> TaskMap(taskActor)}.toMap
                
                sender() ! submittedTask    
            }

        case TPause(ta) =>
            if (TaskMap.contains(ta)){

                ta ! Pause
            }else{

                println("TaskDistribute: TaskMap doesn't contain the taskActor")
            }


        case TResume(ta) =>

            if (TaskMap.contains(ta)){

                ta ! ResumeTask
            }else{ 
                    
                println("TaskDistribute: TaskMap doesn't contain the taskActor")
            }

        case TKill(ta) =>

            if (TaskMap.contains(ta)){

                ta ! KillTask

                // remove the taskActor from the TaskMap and RecipientMap                
                val updatedTaskMap = TaskMap - ta
                val updatedRecipientMap = RecipientMap.map{
                    case (sender, taskActors) => 
                          sender -> taskActors.filterNot(_ == ta)}

                context.become(TaskDistributing(updatedRecipientMap, updatedTaskMap))
                
            }else{
                
                println("TaskDistribute: TaskMap doesn't contain the taskActor")
            }



        case msg =>

            println(s"undefined message [$msg]")
    }
            
    def receive: Receive = TaskDistributing()

    
}