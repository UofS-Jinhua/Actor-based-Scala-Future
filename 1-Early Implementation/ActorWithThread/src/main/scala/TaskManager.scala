import akka.actor._
import akka.dispatch.Dispatcher
import akka.dispatch.{PriorityGenerator, UnboundedStablePriorityMailbox}
import com.typesafe.config.Config


import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


import TaskManager._



// --------------------------------------------------------------------------------------------------------------

 
// inherit, in this case, from UnboundedStablePriorityMailbox
// and seed it with the priority generator

class MyPrioMailbox(settings: ActorSystem.Settings, config: Config) extends UnboundedStablePriorityMailbox(

    // Create a new PriorityGenerator, lower prio means more important
    PriorityGenerator {

      // Result messages should be treated first if possible
      case Result(_) => 0

      case Pause => 1

      case ResumeTask => 2

      case KillTask => 3
 
      // We default to 1, which is in between high and low
      case otherwise     => 4
    })




// --------------------------------------------------------------------------------------------------------------


class WorkerThread[A, B](f: A => B, param: A, creator: ActorRef) extends Thread {

  override def run() {
  """
  Java threads stop themselves by returning from the run() method. When a thread is 
  interrupted, it should stop what it is doing and return from run() as quickly as possible
  """

    var interrupt = false

    // println("WorkerThread: " + Thread.currentThread().getName())

    try{

        while(!interrupt){
            
          val result = f(param)

          creator ! TaskManager.Result(result)

          interrupt = true

        }

      } catch {
          case e: InterruptedException =>

            interrupt = true

            println("WorkerThread: interrupted ...")
          
          case e: Exception =>

            interrupt = true

            println(s"Get unknown exception [$e]...")
      }

    }

}


// --------------------------------------------------------------------------------------------------------------

object TaskManager{

  case class Task[A, B](f: A => B, param: A)

  case class Result[A](out: A)

  case object CheckStatus

  case class TaskStatus(a: Boolean)

  case object Pause

  case object ResumeTask

  case object KillTask

  def props(): Props = Props(new TaskManager()).withDispatcher("prio-dispatcher")

}


class TaskManager extends Actor  {

  import TaskManager._

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
  def Init[A, B](WorkThread: Thread = null): Receive = {

    case Task(f, param) =>

      // println("Receiver Stat: " + context.dispatcher + " " + Thread.currentThread().getName())

      // Create the worker actor
      // println("Receiver: Init, get new task")

      val requester = sender()
      val task = Task(f, param)

      if (WorkThread == null) {
        
        val cur_WorkThread = new WorkerThread(f, param, self)

        cur_WorkThread.start()

        context.become(Busy(requester, cur_WorkThread, task))
        
      }else{

        // Transition to the Busy state
        context.become(Busy(requester, WorkThread, task))
      }
      

      // Process any buffered messages
      if (buffer.nonEmpty) {
        buffer.foreach(msg => self ! msg)
 
      }
      buffer.clear()
    
    case Pause =>

      buffer.addOne(Pause)
    
    case ResumeTask =>

      buffer.addOne(ResumeTask)
    
    case KillTask =>

      buffer.addOne(KillTask)
    
    case CheckStatus =>

      buffer.addOne(CheckStatus)


    case msg =>
      println(s"Receiver: Init, Ignoring message: $msg")
  }

  // The busy state. Waiting for the result from the worker
  def Busy[A, B](requester: ActorRef, WorkThread: Thread, work: Task[A, B], result: Option[A] = None): Receive = {
    
    case Result(out) =>

      // Get the result back from worker, and transition to the Done state

      // println("Receiver: Busy, get result: " + out + "  Change behavior to Done")

      val newResult = Some(out)
      
      // send the result back to the requester 
      requester ! Result(newResult.get)

      // println(WorkThread.getState())

      context.become(Done(requester, WorkThread, newResult, complete = true))

    case CheckStatus =>

      // get checking message from the requester, and send back the task status: False

      sender() ! TaskStatus(false)

    case Pause =>

      // get pause message from the requester, and pause the workThread
      println("Receiver: Busy, get Pause message")

      WorkThread.suspend()

      // Transition to the Paused state
      context.become(Paused(requester, WorkThread, work))

    case KillTask =>

      // get kill message from the requester, and stop the Wthread
      WorkThread.stop()

      context.stop(self)      

    
    case msg =>

      println(s"Ignoring message: $msg")
  }


  def Paused[A, B](requester: ActorRef, WorkThread: Thread, work: Task[A, B]): Receive = {

    // Notice: the stash() and unstashAll() are not working here 
    // because the customized priority mailbox is used for RecActor

    case ResumeTask =>

      

      // get resume message from the requester, and resume the worker
      println("Receiver: Paused, get Resume message")


      WorkThread.resume()

      // Transition to the Busy state
      context.become(Busy(requester, WorkThread, work))


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



  def Done[A](requester: ActorRef, WorkThread: Thread, result: Option[A], complete: Boolean): Receive = {


    case CheckStatus =>

      // get checking message from the requester, and send back the task status: True

      requester ! TaskStatus(complete)



    case msg =>

      println(s"Ignoring message: $msg")
  }


  // Start in the Init state
  def receive = Init()

}


