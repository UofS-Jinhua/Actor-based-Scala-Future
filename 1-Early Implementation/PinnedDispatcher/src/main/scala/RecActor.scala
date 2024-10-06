import akka.actor._
import akka.dispatch.Dispatcher
import akka.dispatch.{PriorityGenerator, UnboundedStablePriorityMailbox}
import com.typesafe.config.Config


import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


import RequestActor._
import FutureActor._ 
import RecActor._





 
// inherit, in this case, from UnboundedStablePriorityMailbox
// and seed it with the priority generator

class MyPrioMailbox(settings: ActorSystem.Settings, config: Config) extends UnboundedStablePriorityMailbox(

    // Create a new PriorityGenerator, lower prio means more important
    PriorityGenerator {

      // Result messages should be treated first if possible
      case Result(_) => 0

      case GetActiveThread(_) => 1

      case Pause => 2

      case ResumeTask => 3
 
      // We default to 1, which is in between high and low
      case otherwise     => 4
    })




object RecActor{

  case class GetActiveThread(t: Thread)

  case object KillFutureActor

  def props(): Props = Props(new RecActor()).withDispatcher("prio-dispatcher")

}


class RecActor extends Actor  {

  import RequestActor._
  import RecActor._

  // a buffer to store the unhandled messages when the actor is paused
  val buffer = ListBuffer.empty[Any]



  """
  JAVA Fatal Error: ThreadDeath 
  
    - ThreadDeath caused by Thread.stop() cannot be caught by the supervisorStrategy

      Single-thread-dispatcher will pass a uncaught ThreadDeath error to ActorSystem, coordinate shutdown of the ActorSystem
  """

  import SupervisorStrategy._

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1. minute) {
      case _: ThreadDeath     => Restart
      case _: Exception       => Restart
      case _                  => Escalate
    }




  // The initial state. Waiting for a task
  def Init[A]: Receive = {

    case Task(f, param) =>

      // println("Receiver Stat: " + context.dispatcher + " " + Thread.currentThread().getName())

      // Create the worker actor

      // println("Receiver: Init, get new task")

      val worker = context.actorOf(FutureActor.props())
      val requester = sender()
      val task = Task(f, param)

      // Send the task to the worker actor
      worker ! Task(f, param)

      // Transition to the Busy state
      context.become(Busy(requester, worker, task))
  }

  // The busy state. Waiting for the result from the worker
  def Busy[A](requester: ActorRef, worker: ActorRef, work: Task[A], result: Option[A] = None): Receive = {
    
    case Result(out) =>

      // Get the result back from worker, and transition to the Done state
      // println("Receiver: Busy, get result: " + out + "  Change behavior to Done")

      val newResult = Some(out)
      
      // send the result back to the requester 
      requester ! Result(newResult.get)

      context.become(Done(requester, worker, newResult, complete = true))

    case CheckStatus =>

      // get checking message from the requester, and send back the task status
      println("Receiver: Busy, Status: False")

      sender() ! Status(false)

    case Pause =>

      // get pause message from the requester, and stop the worker
      println("Receiver: Busy, get Pause message")

      worker ! Pause

      // Transition to the Paused state
      context.become(Paused(requester, worker, work))
      

    
    case msg =>

      println(s"Ignoring message: $msg")
  }


  def Paused[A](requester: ActorRef, worker: ActorRef, work: Task[A]): Receive = {

    // Notice: the stash() and unstashAll() are not working here 
    // because the customized priority mailbox is used for RecActor

    case ResumeTask =>

      

      // get resume message from the requester, and resume the worker
      println("Receiver: Paused, get Resume message")


      worker ! work

      // Transition to the Busy state
      context.become(Busy(requester, worker, work))


      // Process any buffered messages
      if (buffer.nonEmpty) {
        buffer.foreach(msg => self ! msg)
 
      }
      buffer.clear()
    
    case msg => 

      // get other messages from the requester, and Buffer it
      buffer.addOne(msg)
        
      println(s"Receiver: Paused, Store message: $msg")


  }



  def Done[A](requester: ActorRef, worker: ActorRef, result: Option[A], complete: Boolean): Receive = {


    case CheckStatus =>

      println("Receiver: Done, Status: True")

      requester ! Status(complete)



    case msg =>

      println(s"Ignoring message: $msg")
  }


  // Start in the Init state
  def receive = Init

}


