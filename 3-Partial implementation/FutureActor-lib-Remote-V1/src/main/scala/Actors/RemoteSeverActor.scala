package Actors

import Tasks._
import akka.actor._
import akka.stream.javadsl.Zip

object RemoteSeverActor{
    // Message for creating new task
    case class NewTask[T](ID: Int, func: SerializableFunction_1[T])
    case class NextTask[T, U](Prev_ID: Int, cur_ID: Int, func: SerializableFunction_2[T, U])
    case class KillTask(ID: Int)
    case class RequestResult(ID: Int, actor: ActorRef)  
    case class ZipTask(thisID: Int, thatID: Int, curID: Int)
    case class GroupTask(IDs: List[Int], curID: Int)
    //    ===>case class AskResult(actor: ActorRef)



    case class Server_EH_1[T](ID: Int, func: SerializableFunction_1[T])
    case class Server_EH_2[U](ID: Int, func: SerializableFunction_3[U])   
    case class Server_DR[T](ID: Int, r: T)




    def props: Props = Props(new RemoteSeverActor())
}


class RemoteSeverActor extends Actor with Stash with ActorLogging {

    import RemoteSeverActor._
    import FirstWorker._
    import NextWorker._

    def Working(TaskMap: Map[Int, ActorRef] = Map.empty[Int, ActorRef]): Receive ={


        case NewTask(id, func) =>

            val recipient = sender()
            // println("The recipient is: " + sender() + " and the class type is " + sender().getClass())
            val worker = context.actorOf(FirstWorker.props(sender(), func), s"FirstWorker_$id")
            context.become(Working(TaskMap + (id -> worker)))


        case NextTask(pid, cid, func) =>
            val recipient = sender()
            val prev_worker = TaskMap(pid)
            val worker = context.actorOf(NextWorker.props(recipient, prev_worker, func), s"NextWorker_$cid")
            context.become(Working(TaskMap + (cid -> worker)))

        case ZipTask(thisID, thatID, curID) => 

            val recipient = sender()
            val this_worker = TaskMap(thisID)
            val that_worker = TaskMap(thatID)
            val worker = context.actorOf(CombinedWorker.props(recipient, this_worker, that_worker), s"ZipWorker_$curID")
            context.become(Working(TaskMap + (curID -> worker)))
        
        case GroupTask(ids, curID) => 
            val recipient = sender()
            val workers = for (id <- ids) yield TaskMap(id)
            val worker = context.actorOf(GroupedWorker.props(recipient, workers), s"GroupedWorker_$curID")
            context.become(Working(TaskMap + (curID -> worker)))
                
        
        
        case RequestResult(id, a) =>
            val worker = TaskMap(id)
            worker ! AskResult(a)

        case Server_EH_1(id, func) =>
            val worker = TaskMap(id)
            worker ! Error_Handling_1(func)

        case Server_DR(id, r) =>
            val worker = TaskMap(id)
            worker ! Default_Result(r)

        case Server_EH_2(id, func) =>
            val worker = TaskMap(id)
            worker ! Error_Handling_2(func)

        case KillTask(id) => 
            val worker = TaskMap(id)
            context.stop(worker)
            context.become(Working(TaskMap - id))
            
        case msg =>
            // println(s"Sender = ${sender()}, sender class = ${sender().getClass()}")
            // log.info(s"Received unknown message: $msg")
            sender() ! s"Received unknown message: [$msg] at RemoteSeverActor"

    }


    def receive: Receive = Working()
}