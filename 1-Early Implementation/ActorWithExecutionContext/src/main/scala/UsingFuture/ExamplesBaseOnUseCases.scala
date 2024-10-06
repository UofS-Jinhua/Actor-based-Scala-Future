package UsingFuture

// There are some variations on the preceding use case where futures are the best tool for the job. 
// In general the use cases have one or more of the following characteristics:


/*
*     -   You don’t want to block (wait on the current thread) to handle the result of a function.
*/
import akka.actor._
import akka.pattern.pipe
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class NonBlockingActor extends Actor {
  def receive = {
    case x: Int =>
      val future = Future {
        // Some time-consuming computation here
        x * x
      }
      future pipeTo sender
  }
}

// The NonBlockingActor receives an integer x, performs a time-consuming computation in a Future, 
// and then sends the result back to the sender. 
// The pipeTo method is used to avoid blocking the actor while waiting for the Future to complete.



  
/*
*    -   Calling a function once-off and handling the result at some point in the future.
*/
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util.{Success, Failure}

object main_case2 extends App{

    val system = ActorSystem("case2")
    val actor = system.actorOf(Props[NonBlockingActor], "actor")
    implicit val timeout: Timeout = 5.seconds

    val future = actor ? 12  // Send a message to the actor and get a Future
    future.onComplete {
    case Success(result) => println(s"Result: $result")
    case Failure(e) => println(s"Error: ${e.getMessage}")
    }
}

// The ask pattern is used to send a message to an actor and receive a Future representing the response. 
// The onComplete method is used to handle the result of the Future at some point in the future.



    
/*
*     -   Combining many once-off functions and combining the results.
*/

object main_case3 extends App{
    implicit val timeout: Timeout = 5.seconds
    val system = ActorSystem("case3")
    val actor = system.actorOf(Props[NonBlockingActor], "actor")

    val futures = for (i <- 1 to 10) yield (actor ? i).mapTo[Int]
    val future = Future.sequence(futures)
    future.onComplete {
        case Success(results) => println(s"Results: $results")
        case Failure(e) => println(s"Error: ${e.getMessage}")
    }
}

// In this example, multiple messages are sent to an actor using the ask pattern, 
// and the responses are combined into a single Future using Future.sequence.




/*
*     -   Calling many competing functions and only using some of the results, for instance only the fastest response.
*/

object main_case4 extends App{

    val system = ActorSystem("case4")
    val actor = system.actorOf(Props[NonBlockingActor], "actor")

    implicit val timeout: Timeout = 5.seconds
    val futures = for (i <- 1 to 10) yield (actor ? i).mapTo[Int]
    val future = Future.firstCompletedOf(futures)

    future.onComplete {
        case Success(result) => println(s"Fastest result: $result")
        case Failure(e) => println(s"Error: ${e.getMessage}")
    }
}

// Multiple messages are sent to an actor using the ask pattern, and the fastest response is selected using Future.firstCompletedOf.





/*
*     -   Calling a function and returning a default result when the function throws an exception so the flow can continue.
*/

object main_case5 extends App{

    val system = ActorSystem("case5")
    val actor = system.actorOf(Props[NonBlockingActor], "actor")

    implicit val timeout: Timeout = 5.seconds

    val future = (actor ? "hello, world").mapTo[Int].recover { case _ => 0 }
    future.onComplete {
        case Success(result) => println(s"Result: $result")
        case Failure(e) => println(s"Error: ${e.getMessage}")
    }

}

// the recover method is used to provide a default result when the Future fails.




/*
*     -   Pipelining these kind of functions, where one function depends on one or more results of other functions.
*/

object main_case6 extends App{

    val system = ActorSystem("case6")
    val actor1 = system.actorOf(Props[NonBlockingActor], "actor1")
    val actor2 = system.actorOf(Props[NonBlockingActor], "actor2")

    implicit val timeout: Timeout = 5.seconds
    val future1 = (actor1 ? 10).mapTo[Int]
    val future2 = future1.flatMap { result =>
    (actor2 ? result).mapTo[Int]
    }
    future2.onComplete {
        case Success(result) => println(s"Result: $result")
        case Failure(e) => println(s"Error: ${e.getMessage}")
    }

}

// the result of a Future is used as input to another Future, creating a pipeline of computations.