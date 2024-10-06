package UsingFuture

import akka.actor._
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

// Message case class
case class Message(content: String)

// ActorB
class ActorB extends Actor {
    def receive = {
        case Message(content) =>
            Thread.sleep(6000)
            sender() ! Message(content.toUpperCase)
    }
}

// ActorA
class ActorA(actorB: ActorRef) extends Actor {
    implicit val timeout: Timeout = Timeout(5.seconds)

    def receive = {
        case msg: Message =>
            val future = actorB ? msg
            future.mapTo[Message].foreach(response => println(s"Received response: ${response.content}"))
    }
}

object TestActor4_ask extends App {
    // Actor system
    val system = ActorSystem("AskPatternExample")

    val actorB = system.actorOf(Props[ActorB], "actorB")
    val actorA = system.actorOf(Props(new ActorA(actorB)), "actorA")

    // Send a message to ActorA
    actorA ! Message("Hello, world!")
}