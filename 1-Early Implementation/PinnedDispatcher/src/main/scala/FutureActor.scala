import akka.actor._
import akka.event.Logging
import com.typesafe.config.Config


import RequestActor._
import akka.Done



// class WorkerThread[A](f: A => A, param: A, creator: ActorRef) extends Thread {

//   override def run() {
//   """
//   Java threads stop themselves by returning from the run() method. When a thread is 
//   interrupted, it should stop what it is doing and return from run() as quickly as possible
//   """

//     var interrupt = false

//     println("WorkerThread: " + Thread.currentThread().getName())

//     try{

//         while(!interrupt){
            
//           val result = f(param)

//           creator ! FutureActor.TResult(result, Thread.currentThread().getName())

//           interrupt = true

//         }

//       } catch {
//           case e: InterruptedException =>

//             interrupt = true

//             println("WorkerThread: interrupted ...")
          
//           case e: Exception =>

//             interrupt = true

//             println(s"Get unknown exception [$e]...")
//       }

//     }

// }

object FutureActor{

  case class TResult[A](result: A, thread: String)

  def props(): Props = Props(new FutureActor())
  // .withDispatcher("single-thread-dispatcher")

}


// Define the actor
class FutureActor extends Actor {

  import RequestActor._
  import RecActor._
  import FutureActor._



  """
  JAVA Fatal error: ThreadDeath

    - PreRestart cannot catch ThreadDeath ERROR in preRestart, not working
        
      - ThreadDeath Error will escalate to the supervisor ...
  """

  // val log = Logging(context.system, this)

  // override def preRestart(reason: Throwable, message: Option[Any]) {
  //   reason match {
  //     case _: ThreadDeath =>
  //       log.error(reason, "ThreadDeath caught! Cleaning up...")
  //       // Your cleanup code here...
  //       throw reason // rethrow to allow the ThreadDeath to propagate
  //     case _ => super.preRestart(reason, message)
  //   }
  // }
  def work[A] (Wthread: Thread = null): Receive = {

    case Task(f, param) =>
        
      // val WorkThread = new WorkerThread(f, param, self)

      // WorkThread.start()

      val WorkThread = Thread.currentThread()

      val manager = sender()

      // println(s"Worker: get new task now... workTread [${WorkThread.getName()}]")

      val result = f(param)

      manager ! Result(result)

      context.become(done(manager, WorkThread, result))
    
    case _ =>

      println("Worker: get unknown message")
  }

  def done[A](manager: ActorRef, Wthread: Thread, FR: A = None): Receive = {

    case TResult(result, tname) =>

      println(s"Worker: get result [$result] from thread [$tname]")

      manager ! Result(result)

    case Pause =>
        
      println("Worker: get pause message from manager")

      Wthread.stop()

      context.become(work())

    case _ =>

      println("Worker: get unknown message")
  }

  def receive = work()
}