package UsingFuture

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.pipe
import scala.concurrent.Future


// This file is used to test the feature of using Future in an actor

//     In this file, we create an actor that receives a message to calculate the nth Fibonacci number in future
//     The actor will send the result back to itself
//     The actor will pipe the result to itself  

object TestActor1{

  case class Task(param: Int)
  case object GetResult

  def props(): Props = Props(new TestActor1())
}


class TestActor1 extends Actor {

  import context.dispatcher
  import TestActor1._

  var Result: Future[Int] = null


  def receive = {


    case Task(param) =>

      val futureResult = Future {

        def fib(i: Int): Int = i match{
   
            case 0 | 1 => i
            case i => fib(i - 1) + fib(i - 2)
          
        }

        fib(param)

      }

      Result = futureResult

      futureResult.pipeTo(self)

    case GetResult =>

      println(s"Received result: $Result")

    case msg =>

      println(s"Received unknown message: $msg")
  }
}


object Main extends App {

  import TestActor1._

  // Create the actor system
  val system = ActorSystem("MyActorSystem")

  // Create the actor
  val myActor = system.actorOf(TestActor1.props(), "myActor")



  // Send a Task message to the actor
  myActor ! Task(40)
  myActor ! GetResult
  myActor ! 1
  myActor ! 2
  myActor ! 3
}
