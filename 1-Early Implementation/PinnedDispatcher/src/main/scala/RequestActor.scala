import akka.actor._
import RecActor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object RequestActor{

  case class Task[A](f: A => A, param: A)

  case class Result[A](out: A)

  case object CheckStatus

  case class Status(a: Boolean)

  case object Pause

  case object ResumeTask

  def props(): Props = Props(new RequestActor())

}


class RequestActor[A] extends Actor{

  import RequestActor._


  """
  still doesn't work to handle ThreadDeath exception here in supervisorStrategy
  """
  import SupervisorStrategy._

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1. minute) {
      case _: ThreadDeath     => Resume
      case _: Exception       => Restart
      case _                  => Escalate
    }

  var result: Option[A] = None

  // For testing purpose
  var start_time = System.currentTimeMillis() 
  var end_time = System.currentTimeMillis()
  var counter = 0
  var teststart = false
  // ================================================
  

  def receive = {

    case Task(f, param) =>

      // For testing purpose
      if (!teststart){
        start_time = System.currentTimeMillis()
        teststart = true
      }
      // ================================================

      // println("Requester: create a new task now...")

      val receiver = context.actorOf(RecActor.props())

      receiver ! Task(f, param)


    case Result(out) =>

      // println("Requester: get result: " + out)

      result = Some(out.asInstanceOf[A])


      // For testing purpose
      counter += 1
      if (counter == 1000){
        end_time = System.currentTimeMillis()
        println("Requester: time elapsed: " + (end_time - start_time) + " ms")
      }



    // case CheckStatus =>

    //   receiver ! CheckStatus

    case Status(a: Boolean) =>

      if (a){
            
            println("Task Done! Get status: " + a)
      } else{
            
            println("Task is still running... Get status: " + a)
      }

    // case Pause =>

    //   receiver ! Pause
    
    // case ResumeTask =>  
        
    //   receiver ! ResumeTask
    
    case _ =>
            
        println("Requester: Ignoring message")
  }
}