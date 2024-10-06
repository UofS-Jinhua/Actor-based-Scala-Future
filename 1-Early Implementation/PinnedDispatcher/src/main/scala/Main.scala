import akka.actor.ActorSystem

import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


import akka.actor._
import RequestActor._
import RecActor._
import FutureActor._



object Main extends App {
  import RequestActor._


  def fibonacci(n: Int): Int = {
    n match {
      case 0 | 1 => n
      case _ => fibonacci(n - 1) + fibonacci(n - 2)
    }
  }

  def testing_f(n: Int): Int = n

  // Create the actor system
  val system = ActorSystem("TestingSystem")

  val requester = system.actorOf(RequestActor.props(), "ReqActor")

  // Send a message to the actor
  // requester ! Task(fibonacci, 10)
  // requester ! Task(testing_f, 1)


  for (i <- 1 to 1000) {
    requester ! Task(testing_f, i)
  }

  

  // requester ! Pause
  // requester ! CheckStatus
  // requester ! CheckStatus
  // requester ! CheckStatus
  // requester ! ResumeTask

  // requester ! CheckStatus
  // requester ! CheckStatus
  // requester ! CheckStatus
  // requester ! CheckStatus
  // requester ! CheckStatus
  // requester ! CheckStatus
  // requester ! CheckStatus
  // requester ! CheckStatus

  system.scheduler.scheduleOnce(3.seconds) {
    system.terminate()
  }

  // implicit val timeout: Timeout = Timeout(5.seconds)


  // // Select all user actors in the system
  // val selection = system.actorSelection("/user/*")


  // println(selection)


  /// Test the priority mailbox

  // val receiver = system.actorOf(RecActor.props(), "RecActor")


  // receiver ! SomeFunction(fibonacci, 20)

  // receiver ! CheckStatus
  // receiver ! CheckStatus

  // receiver ! CheckStatus
  // receiver ! CheckStatus

  // receiver ! CheckStatus
  // receiver ! CheckStatus

  // receiver ! CheckStatus
  // receiver ! CheckStatus

  // receiver ! PoisonPill
  // receiver ! CheckStatus
  // receiver ! CheckStatus

  // Thread.sleep(1000)
  // system.terminate()
  // Schedule a message to be sent to the system after 3 seconds
}


// For testing the default actor system
object main_default extends App {


  def testing_f(n: Int): Int = n

  // Create the actor system
  val system = ActorSystem("TestingSystem")

  val requester = system.actorOf(RequestActor.props(), "ReqActor")

  // Send a message to the actor
  // requester ! Task(fibonacci, 10)
  for (i <- 1 to 1000) {
    requester ! Task(testing_f, i)
  }

  system.scheduler.scheduleOnce(30.seconds) {
    system.terminate()
  }


}




// For testing the future
object main_mixF extends App {
  import scala.concurrent.{Await, Future}

  def testing_f(n: Int): Int = n
  
  val system = ActorSystem("MixActorSystem")

  class MixActor extends Actor {

    override def preStart(): Unit = {
      self ! "Start"
    }

    def receive = {
      case "Start" =>
        val start = System.currentTimeMillis
        // For testing purpose: single future

        // val all = Future{testing_f(1)}

        val futures = for (i <- 1 to 1000) yield {
          Future{testing_f(i)}
        }
        val all = Future.sequence(futures)
        val allResult = Await.result(all, 10.seconds)
        val end = System.currentTimeMillis
        println("MixActor: time elapsed: " + (end - start) + " ms")

      case Result(out) =>
        println("MixActor: get result: " + out)
    }
  }
  val mixActor = system.actorOf(Props(new MixActor()), "MixActor")

  system.scheduler.scheduleOnce(2.seconds) {
    system.terminate()
  }



}