import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging

class Caculate extends Actor {
  val log = Logging(context.system, this)

  import Caculate._

  // Message loop
  def receive = {

    case Cal(para1) => {
      // write your logic here
      var sum = 0
      for (i <- 1 to para1) {
        sum += i
      }

      // send result back
      sender() ! Result(sum)
    }

    case Stop => {
      log.info("received stop message")
      context.stop(self)
    }
    case msg => log.info(s"received unknown message {$msg}}")
  }
}

object Caculate {
  // Define message type the actor will receive here
  sealed trait Message
  case class Cal(para1: Int) extends Message
  case object Show extends Message
  case object Stop extends Message
  case class Result(result: Int) extends Message

  // Define Props used to create Actor
  def props: Props = Props(new Caculate)
}
