import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import _root_.akka.actor.ActorRef

class Count1(parameter: Int) extends Actor {
  import Count1._
  // log or use extends Actor with ActorLogging
  val log = Logging(context.system, this)

  // Initial setup here
  val initialState = parameter

  // use the field to store the state
  var state = initialState
  var start_time = 0L
  var end_time = 0L
  var start_time2 = 0L
  var end_time2 = 0L
  val count = 1000
  val calcu_to = 10000
  var result_count = 0
  var test1: akka.actor.ActorRef = null
  // Message loop
  def receive = {

    case Request =>
      start_time = System.currentTimeMillis()
      log.info(
        s"received request message, begin to test actor run time, current time is $start_time"
      )
      test1 = context.actorOf(Caculate.props, "test1")
      for (i <- 1 to count) {
        test1 ! Caculate.Cal(calcu_to)
      }
    case Caculate.Result(sum) => {
      result_count += 1
      if (result_count == count) {
        end_time = System.currentTimeMillis()
        println("time: " + (end_time - start_time) + "ms")
        log.info(s"current time is $end_time")
        test1 ! Caculate.Stop
      }
    }

    case TestFuture(calcu_to) => {
      import scala.concurrent.Future
      import scala.concurrent.ExecutionContext.Implicits.global
      start_time2 = System.currentTimeMillis()
      log.info(s"received test future message, current time is $start_time2")
      val futures: Seq[Future[Int]] = Array.fill(count)(Future {
        var sum = 0
        for (i <- 1 to calcu_to) {
          sum += i
        }
        sum
      })
      // await all futures
      futures.foreach(Await.result(_, Duration.Inf))
      end_time2 = System.currentTimeMillis()
      println("Future run time: " + (end_time2 - start_time2) + "ms")
      log.info(s"All futures have run, current time is $end_time2")
    }

    case Stop =>
      // other way is send actor PoisonPill or Kill
      context.stop(self)
    case _ => log.info("received unknown message")
  }
}

object Count1 {
  // Define message type the actor will receive here
  sealed trait Message
  case object Request extends Message
  case object Stop extends Message
  case class TestFuture(para1: Int) extends Message

  // Define Props used to create Actor
  def props(parameter: Int): Props = Props(new Count1(parameter))
  // create actor:
  // context.actorOf(Count1.props("real parameter"),"actor name")
}

import akka.actor.ActorSystem
object MeasureActor extends App {
  import Count1._
  val system: ActorSystem = ActorSystem("system")
  val actor1 = system.actorOf(Count1.props(1), "actor1")
  actor1 ! Request
  Thread.sleep(10000)
//   actor1 ! TestFuture(10000)
//   Thread.sleep(1000)
  system.terminate()
  
}
