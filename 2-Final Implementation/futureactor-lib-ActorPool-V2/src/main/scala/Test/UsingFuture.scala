package Test

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.actor._
import akka.pattern.pipe
import Test.future_main.futures
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import scala.concurrent.ExecutionContext
import Test.future_main.start
import java.io.PrintWriter
import java.io.FileOutputStream
import java.io.File


object future_main extends App{
    var futures = List[Future[Int]]()
    val start = System.currentTimeMillis()

    for (i <- 1 to 10000){
        val f1 = Future{
           var i = 0
           for (j <- 1 to 10000){
               i += j
           }
           i
        }
        futures = f1 :: futures
    }

    val result = Await.result(Future.sequence(futures), Duration.Inf)

    val end = System.currentTimeMillis()
    println(s"Time: ${end - start} ms, # of Result: ${result.length}")

}


object future_main2 extends App{


    val system = ActorSystem("ActorSystem")
    val temp = system.actorOf(Props(new Actor {

        var counter = 0
        var s_time: Long = _
        var e_time: Long = _
        def receive = {
            case "Start" =>
                s_time = System.currentTimeMillis()

                for (i <- 1 to 10000){
                    val f1 = Future{
                        var i = 0
                        for (j <- 1 to 10000){
                            i += j
                        }
                        i
                    }.pipeTo(self)

                }

            case x: Int => 
                counter += 1
                if (counter == 10000){
                    e_time = System.currentTimeMillis()
                    println(s"Time: ${e_time - s_time} ms")
                }
        }
    }), "temp")

    temp ! "Start"


}



object future_main6 extends App{

    case class Start(a: ActorRef)

    val system = ActorSystem("FutureActorSystem")



    val creator = system.actorOf(
        Props(new Actor {

            def receive = {
                case "Start" =>


                    val f1 = Future{
                        println(s"1st GetTask, ${Thread.currentThread().getName()}")
                        Thread.sleep(3000)
                        1 + 1
                    }.foreach(x => 
                        println(x)
                        sender() ! x)
                    
                case x => 

                    println(s"Get result: $x")
            }
        }), "creator")


    val ta1 = system.actorOf(Props(new Actor {
        def receive = {
            case Start(a) =>

                a ! "Start"

            case x => 
                println(s"ta1 Get result: $x")
        }
    }), "ta1")

    val ta2 = system.actorOf(Props(new Actor {
        def receive = {
            case Start(a) =>

                a ! "ta2 send a message"

            case x => 
                println(s"ta2 Get result: $x")
        }
    }), "ta2")


    
    ta1 ! Start(creator)
    // ta2 ! Start(creator)

    system.scheduler.scheduleOnce(10.seconds){
        system.terminate()
    }(system.dispatcher)
}



object future_main7 extends App{


    val system = ActorSystem("FutureActorSystem")

    val testingActor = system.actorOf(Props(new Actor {
        def receive = {
            case "Start" =>
                val testing_f1 = (i: Int) => i
                val testing_f2 = (i: Int, t: Duration) => {
                    val start_time = System.currentTimeMillis()
                    while (System.currentTimeMillis() <= start_time + t.toMillis){} 
                    i}
                
                val start_time = System.currentTimeMillis()

                // concurrent testing

                // // val futures: List[Future[Int]] = List.tabulate(1000)(i => Future(testing_f1(i)))


                val futures: List[Future[Int]] = List.tabulate(1000)(i => Future(testing_f2(i, 10.millis)))
                val result = Await.result(Future.sequence(futures), Duration.Inf)
                val end_time = System.currentTimeMillis()
                println(s"Time: ${end_time - start_time} ms, lenth: ${result.length}")

                // single long running task testing

                // val future1 = Future(testing_f2(1, 10.millis))
                // val result = Await.result(future1, Duration.Inf)
                // val end_time = System.currentTimeMillis()
                // println(s"Time: ${end_time - start_time} ms")





            case x => 

                println(s"Get result: $x")
        }
    }), "testingActor")


    testingActor ! "Start"
}