package UsingFuture

import akka.actor._
import akka.pattern.pipe
import scala.io.Source
import scala.concurrent.Future


// This file is used to test the feature of using Future sequentially in an actor

//     In this file, we create an actor that receives a message to fetch data from a URL in future
//     The actor will send the result back to itself

//     Then, the actor will count the number of words in the data in future
//     The actor will send the result back to itself

//     Process: Fetch data from URL -> Count the number of words in the data




object TestActor2{

    case class FetchData(url: String)
    case class Data(content: String)
    case object Count

    def props():Props = Props(new TestActor2())
}



class TestActor2 extends Actor {

  import context.dispatcher
  import TestActor2._

  var data: Future[String] = null

  def receive = {

    case FetchData(url) =>

      // Use Future to asynchronously fetch data from the URL
      val futureData = Future {

        println(Thread.currentThread().getName)
        // Fetch data from the URL, Send the data back to the self as a Data message
        Source.fromURL(url).mkString
      }

      // Assign the futureData to the data variable
      data = futureData

      self ! Count

      futureData.map(s => Data(s)).pipeTo(self)


      // Future {
      //   println(Thread.currentThread().getName)
      //   Thread.sleep(5000)
      //   Source.fromURL(url).mkString
      // }.flatMap(data => 
      //     Future{
      //     println(Thread.currentThread().getName)
      //     Thread.sleep(5000)
      //     data.split("\\s+").length})
      // .map(count => Data(count.toString())).pipeTo(self)
    

        
    case Data(content) =>
      println(s"Data: $content")

    case Count =>

      println("get count message")

      val futureWordCount = data.flatMap { data =>
        Future {
          println(Thread.currentThread().getName)
          // Count the number of words in the data
          data.split("\\s+").length
        }
      }

      futureWordCount.map(count => Data(count.toString())).pipeTo(self)

    case msg =>

      println(s"Undefined Msg: $msg")
  }
}

object main2 extends App{

  import TestActor2._

  // Create the actor system
  val system = ActorSystem("MyActorSystem2")

  // Create the actor
  val myActor = system.actorOf(TestActor2.props(), "myActor")

  // Send a FunctionMessage
  myActor ! FetchData("https://jsonplaceholder.typicode.com/posts/1")
  // myActor ! FetchData("https://jsonplaceholder.typicode.com/posts/1")

  for (i <- 1 to 10) {
    myActor ! i
  }

}
