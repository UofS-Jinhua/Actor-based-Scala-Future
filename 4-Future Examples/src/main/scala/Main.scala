import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.util._
import scala.util.Random


object future_creation_and_await extends App {
  val future1 = Future { 1 }

  val future2 = Future {
    throw new RuntimeException("Exception") 
  }


  
  try { 
    val result = Await.result(future1, 2.seconds)
    println(s"Success: Result = $result")
  } catch {
    case _: TimeoutException => println("Timeout!")
  }
  try { 
    Await.result(future2, 2.seconds)
  } catch {
    case _: TimeoutException => println("Timeout!")
    case e: RuntimeException => println(s"Exception caught: ${e.getMessage}")
  }
}



object callback extends App{
  val future = Future {
    if (Math.random() < 0.5) 42 else throw new RuntimeException("Error!")
  }
  // Example of foreach
  future.foreach { result =>
    println(s"The answer is: $result") //  may not be executed
  }
  // Example of onComplete
  future.onComplete {
    case Success(result) => println(s"Success: $result")
    case Failure(exception) => println(s"Failure: ${exception.getMessage}")
  }
  // Example of andthen
  val newFuture = future.andThen {
    case _ => println("Original future completed") 
  }
}

object transformation extends App {
  val future1 = Future { 5 }
  // Example of map:  
  val future2 = future1.map(x => x + 1)
  // Example of flatmap: 
  val future3 = future1.flatMap(x => Future { x + 1 })
  // Example of transform:
  val future4 = future1.transform {
    case Success(x) => Success(x + 1)  
    case Failure(e) => Failure(e)
  }
  // Example of transformwith:
  val future5 = future1.transformWith {
    case Success(x) => Future { x + 1 } 
    case Failure(e) => Future.failed(e) 
  }

  val futures = List(future1, future2, future3, future4, future5)
  futures.foreach { future =>
    future.onComplete {
      case Success(result) => println(s"Success: $result")
      case Failure(exception) => println(s"Failure: ${exception.getMessage}")
    }
  }
}




object combination extends App {
  val future1 = Future { 1 }
  val future2 = Future { 2 }
  val future3 = Future { 3 }
  val future4 = Future { "four" }
  val futures = List(future1, future2, future3)

  // Example of zip: --------- result: (1, "four")
  val future5 = future1.zip(future4)
  // Example of zipwith: ----- result: 3
  val future6 = future1.zipWith(future2)((x, y) => x + y)
  // Example of sequence: ---- result: List(1, 2, 3)
  val future7 = Future.sequence(futures)
  // Example of traverse: ---- result: List(2, 3, 4)
  val future8 = Future.traverse(futures)(x => x.map{ y => y + 1 })
  
  
  
  future5.foreach{println}
  future6.foreach{println}
  future7.foreach{println}
  future8.foreach{println}

}




object error_handling extends App{
  val future1: Future[Int] = Future { 
    Random.nextInt(10) match {
      case x if x < 5 => x
      case _ => throw new RuntimeException("Error!")
    } }

  // Example of recover: --------- result: 1
  val future2 = future1.recover {
    case e: RuntimeException => 1
  }
  // Example of recoverwith: ----- result: an integer between 0 and 9
  val future3 = future1.recoverWith {
      case e: RuntimeException => Future { Random.nextInt(10) }
  }
  // Example of fallbackto: ------ result: an integer between 0 and 9
  val future4 = future1.fallbackTo(Future { Random.nextInt(10) })


  future1.foreach(println)
  future2.foreach(println)
  future3.foreach(println)
  future4.foreach(println)
}