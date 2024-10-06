package TestingFeature

import scala.concurrent._
import scala.concurrent.duration._
import scala.util._
import scala.concurrent.ExecutionContext.Implicits.global
import java.time.Duration._
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration


object Testing_Future_Create_and_Await extends App{

  // Before we create any Future, we need to provide an implicit ExecutionContext. 
  // This specifies how and on which thread pool the Future code will execute. We can create it from Executor or ExecutorService.



  // 1. Future.apply
  //  - Future.apply is a factory method that creates a Future instance.
  //  - It takes a block of code that will be executed asynchronously.
  //  - The block of code is executed by some thread in the future.
  //  - The Future.apply method returns immediately with a Future instance.

  val future1 = Future {
    println(s"Future1 is running on Thread [${Thread.currentThread().getName()}] .....")
    val current_time = System.currentTimeMillis()
    Thread.sleep(Random.nextInt(2000))
    val end_time = System.currentTimeMillis()

    if (end_time - current_time > 2000) {
      throw new Exception("Future1 is running too long")
    }
    1 + 1
  } 
  // Future1 will be either completed sucessfully or failed with an exception
  


  // 2. Await for Future
  //  - Await.result blocks the current thread until the Future has completed.
  //  - It returns the result of the Future.

  //  - Await.ready blocks the current thread until the Future has completed.
  //  - It returns the Future itself.


  val waiting_time: FiniteDuration = Duration(4, TimeUnit.SECONDS)
  val result1 = Await.result(future1, waiting_time)
  val result2 = Await.ready(future1, waiting_time)
  
  println(s"result1: $result1")
  println(s"result2: $result2")
}



object Testing_Future_Callbacks extends App{


  val future2 = Future.successful(100)
  val future3 = Future.failed(new Exception("Future3 failed"))
  // 3. Callbacks
  //  - Callbacks are functions that are executed once the Future completes.
  //  - Callbacks are executed in the same thread that completes the Future.
  //  - Callbacks are executed in the order they are registered.

  // 3.1 OnComplete
  //  - OnComplete is called when the Future completes.
  //  - It takes a PartialFunction that is called with a Try of the Future result.
  //  - It returns Unit.


  future2.onComplete {
    case Success(value) => println(s"Future2 completed sucessfully with value: [$value], current thread: [${Thread.currentThread().getName()}]")
    case Failure(exception) => println(s"Future2 failed with exception: $exception")
  }
  future3.onComplete {
    case Success(value) => println(s"Future3 completed sucessfully with value: [$value], current thread: [${Thread.currentThread().getName()}]")
    case Failure(exception) => println(s"Future3 failed with exception: $exception")
  }
}


object Testing_Future_Callbacks2 extends App{

  """
    3. Callbacks

      - onSucess and onFailure are called when the Future completes.

      - Removed in Scala 2.12

  """

  // 3.2 OnSuccess
  //  - OnSuccess is called when the Future completes sucessfully.
  //  - It takes a PartialFunction that is called with the Future result.
  //  - It returns Unit.

  // 3.3 OnFailure
  //  - OnFailure is called when the Future completes with an exception.
  //  - It takes a PartialFunction that is called with the Future exception.
  //  - It returns Unit.


  val future2 = Future.successful(100)
  val future3 = Future.failed(new Exception("Future3 failed"))


  // foreach is a special case of onSuccess
  // - foreach is called when the Future completes sucessfully.
  // - It takes a function that is called with the Future result.
  // - It returns Unit.

  future2.foreach(value => println(s"Future2 completed sucessfully with value: [$value], current thread: [${Thread.currentThread().getName()}]"))
  future3.foreach(value => println(s"Future3 completed sucessfully with value: [$value], current thread: [${Thread.currentThread().getName()}]"))





}


object Testing_Future_Map_FlatMap extends App{

  // 4. Transform Future

  // 4.1 Map
  //  - Map is called when the Future completes sucessfully.
  //  - It takes a function that is called with the Future result.
  //  - It returns a new Future with the result of the function.

  def devide(x: Int): Int = 1 / x

  val future4 = Future.successful(100)
  val future5 = future4.map(devide).foreach(println)

  val future6 = Future.failed(new Exception("Future6 failed"))
  val future7 = future6.map(devide).onComplete {
    case Success(value) => println(s"Future7 completed sucessfully with value: [$value], current thread: [${Thread.currentThread().getName()}]")
    case Failure(exception) => println(s"Future7 failed with exception: $exception")
  }
  

  // 4.2 FlatMap
  //  - FlatMap is called when the Future completes sucessfully.
  //  - It takes a function that is called with the Future result.
  //  - It returns a new Future with the result of the function.

  val future8 = Future.successful(0)
  val future9 = future8.flatMap(x => Future(100 / x)).onComplete(println)

}

object Testing_Future_Transform extends App{


  // 4.3 transform
  //  - transform is called when the Future completes.
  //  - It takes a function that is called with the Future result.
  //  - It returns a new Future with the result of the function.

  val future10 = Future{
    val current_time = System.currentTimeMillis()
    Thread.sleep(Random.nextInt(3000))
    val end_time = System.currentTimeMillis()
    if (end_time - current_time > 2000) {
      throw new Exception("Future10 is running too long")
    }
    100
  }

  val future11 = future10.transform(x => x * 10, e => new Exception("Future11 failed")).onComplete(println)




  // 4.4 transformWith
  //  - transformWith is called when the Future completes.
  //  - It takes a function that is called with the Future result.
  //  - It returns a new Future with the result of the function.

  val future12 = future10.transformWith{
    case Success(value) => Future(value * 10)
    case Failure(exception) => Future.failed(new Exception("Future12 failed"))
  }.onComplete(println)

  Thread.sleep(4000)
}


object Testing_Future_ErrorHandling extends App{
  
  // 5. Error Handling
  // - Error handling is done with callbacks.

  val failedF: Future[Int] = Future.failed(new IllegalArgumentException("Boom!"))
  val failureF: Future[Throwable] = failedF.failed

  // 5.1 Recover
  //  - Recover is called when the Future completes with an exception.
  //  - It takes a PartialFunction that is called with the Future exception.
  //  - It returns a new Future with the result of the function.
  //
  // If we want to handle a particular exception by providing an alternative value, we should use the recover method:

  val recoveredF: Future[Int] = Future(3 / 0).recover {
    case _: ArithmeticException => 1
  }
  println(recoveredF)
  val result = Await.result(recoveredF, Duration.Inf)
  println(result)


  // 5.2 RecoverWith
  // - RecoverWith is called when the Future completes with an exception.
  // - It takes a PartialFunction that is called with the Future exception.
  // - It returns a new Future with the result of the function.
  //
  // If we want to handle a particular exception with another Future, we should use recoverWith instead of recover:
    
  val recoveredWithF: Future[Int] = Future(3 / 0).recoverWith {
    case _: ArithmeticException => Future(2)
  }
  val result2 = Await.result(recoveredWithF, Duration.Inf)
  println(result2)
  println(recoveredWithF)

  // 5.3 FallbackTo
  // - FallbackTo is called when the Future completes with an exception.
  // - It takes a Future that is called with the Future exception.
  // - It returns a new Future with the result of the function.

  trait DatabaseRepository {
    def readMagicNumber(): Future[Int]
    def updateMagicNumber(number: Int): Future[Boolean]
  }
  trait FileBackup {
    def readMagicNumberFromLatestBackup(): Future[Int]
  }

  trait MagicNumberService {
    val repository: DatabaseRepository
    val backup: FileBackup

    // It takes an alternative Future in case of a failure of the current one and evaluates them simultaneously. 
    // If both fail, the resulting Future will fail with the Throwable taken from the current one.

    val magicNumberF: Future[Int] =
      repository.readMagicNumber()
        .fallbackTo(backup.readMagicNumberFromLatestBackup())
  }
}