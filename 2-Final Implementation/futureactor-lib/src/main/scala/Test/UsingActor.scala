package Test

import Tasks._
import CollectResult._
import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.runtime.stdLibPatches.language.future
import java.io.PrintWriter
import java.io.FileOutputStream
import java.io.File



// Using Collect to collect result
// - Await is used
object main1 extends App{

    val system = ActorSystem("FutureActorSystem")
    val creator = system.actorOf(
        Props(new Actor {

            def receive = {
                case "Start" =>

                    val f1 = LocalTask(self).Start{
                        println("1st LocalTask")
                        Thread.sleep(1000)
                        1 + 1
                    }

                    val f2 = LocalTask(self).Start{
                        println("2nd LocalTask")
                        "Ops"
                    }
                    // println(Await.result(f1, 1000. millis))
                    val f1_result = ACollect(self).LookingFor(f1, 5.seconds)
                    val f_result = ACollect(self).LookingFor( Seq(f1, f2), 8.seconds)
                    f1_result.PrintResult
                    f_result.PrintResult

                    val f1_wait = Await.result(f1_result, 5.seconds)
                    val f2_wait = Await.result(f_result, 8.seconds)
                    println(s"Waiting result f1 = $f1_wait, f2 = $f2_wait")



                case x => println(s"Client get: $x")
            }
        }), "creator")
    
    creator ! "Start"

    system.scheduler.scheduleOnce(5.seconds){
        system.terminate()
    }(system.dispatcher)
}



// Using ACollect to collect result
// - Await is not used
// - Actor is used to collect result by message passing
object main2 extends App{

    val system = ActorSystem("FutureActorSystem")
    val creator = system.actorOf(
        Props(new Actor {

            def receive = {
                case "Start" =>

                    val f1 = LocalTask(self).Start{
                        println(s"1st LocalTask, ${Thread.currentThread().getName()}")
                        Thread.sleep(2000)
                        1 + 1
                    }

                    val f2 = LocalTask(self).Start{
                        println(s"2nd LocalTask, ${Thread.currentThread().getName()}")
                        "Ops"
                    }

                    // val f1_result = ACollect(self).LookingFor(f1, 5.seconds)
                    // val f_result = ACollect(self).LookingFor( Seq(f1, f2), 5.seconds)


                    // val f1_wait = Await.result( ACollect(self).LookingFor(f1, 5.seconds), 3.seconds)
                    // val f2_wait = Await.result( ACollect(self).LookingFor( Seq(f1, f2), 5.seconds), 5.seconds)


                    val f1_wait = Await.result(f1, 5.seconds)
                    val f2_wait = Await.result(f2, 5.seconds)

                    println(s"f1_wait: $f1_wait")
                    println(s"f2_wait: $f2_wait")




                case x => println(x)
            }
        }), "creator")
    
    creator ! "Start"


    system.scheduler.scheduleOnce(10.seconds){
        system.terminate()
    }(system.dispatcher)
}


// Performance test
//
// Using ACollect to collect result of several tasks: adding 1 to 10000 from 10000 actors
// - Await is not used
// - Actor is used to collect result by message passing
object main3 extends App{


    val system = ActorSystem("FutureActorSystem")
    val creator = system.actorOf(
        Props(new Actor {

            var s_time = System.currentTimeMillis()

            def receive = {
                case "Start" =>
                    // var result = List[Int]()
                    var tasks = List[LocalTask[Int]]()
                    s_time = System.currentTimeMillis()

                    for (i <- 1 to 10000){

                        val f1 = LocalTask(self).Start{
                            var i = 0
                            for (j <- 1 to 10000){
                                i += j
                            }
                            i
                        }
                        tasks = f1 :: tasks

                    }

                    ACollect(self).LookingFor(tasks, 5.seconds)

                case x: Map[Int, Any] => 
                    var e_time = System.currentTimeMillis()
                    println(s"Time: ${e_time - s_time} ms, # of Result: ${x.size}")
                    // println(x)
            }
        }), "creator")
    
    creator ! "Start"

    system.scheduler.scheduleOnce(5.seconds){
        system.terminate()
    }(system.dispatcher)

}

// Performance test
//
// Let the worker actor send the result to the creator directly
object main4 extends App{

    val system = ActorSystem("FutureActorSystem")
    val creator = system.actorOf(
        Props(new Actor {

            var s_time: Long = _ 
            var counter = 0

            def receive = {
                case "Start" =>
                    // var result = List[Int]()
                    s_time = System.currentTimeMillis()

                    for (i <- 1 to 10000){

                        val f1 = LocalTask(self).Start{
                            var i = 0
                            for (j <- 1 to 10000){
                                i += j
                            }
                            i
                        }.GetResultByActor

                    }

                case x => 
                    counter += 1
                    if (counter == 10000){
                        var e_time = System.currentTimeMillis()
                        println(s"Time: ${e_time - s_time} ms")
                    }

                    // println(x)
            }
        }), "creator")
    
    creator ! "Start"

    system.scheduler.scheduleOnce(3.seconds){
        system.terminate()
    }(system.dispatcher)
}


// Feature test: Zip
object main5 extends App{

    val system = ActorSystem("FutureActorSystem")
    val creator = system.actorOf(
        Props(new Actor {

            def receive = {
                case "Start" =>


                    val f1 = LocalTask(self).Start{
                        println(s"1st LocalTask, ${Thread.currentThread().getName()}")
                        1 + 1
                    }


                    val f2 = LocalTask(self).Start{
                        println(s"2nd LocalTask, ${Thread.currentThread().getName()}")
                        "Ops"
                        // 4
                    }.Zip(f1)


                    f2.GetResultByActor

                    val f3 = f2.NextStep{
                        (x,y) => 
                            val new_x = x.length()
                            val x_value = x + "hahaha"
                            var new_y  = 0
                            for (i <- 1 to 100) {new_y += i}
                            (new_x, x_value, new_y)
                    }
                    f3.GetResultByActor


                    ACollect(self).LookingFor( Seq(f1, f2, f3), 5.seconds)

                case x => 

                    println(s"Get result: $x")
            }
        }), "creator")
    
    creator ! "Start"

    system.scheduler.scheduleOnce(5.seconds){
        system.terminate()
    }(system.dispatcher)
}


// Feature test: Group
object main6_groupTest extends App{

    val system = ActorSystem("FutureActorSystem")
    val creator = system.actorOf(
        Props(new Actor {

            def receive = {
                case "Start" =>


                    val f1 = LocalTask(self).Start{

                        1 + 1
                    }

                    val f2 = LocalTask(self).Start{
                        "Ops"
                        // 4
                    }

                    val f3 = f1.NextStep(_ + 1)
                    val f4 = f3.NextStep(_ + 1)
                    val f5 = f4.NextStep(_ + 1)

                    // val f6 = LocalTask(self).Group( Seq(f1, f2, f3, f4, f5)).GetResultByActor

                    val f6 = LocalTask(self).Group( Seq(f1, f3, f4, f5))
                    f6.GetResultByActor
                    val f7 = f6.NextStep(_.map(_ * 10))

                    f7.GetResultByActor


                case x => 

                    println(s"Get result: $x")
            }
        }), "creator")
    
    creator ! "Start"

    system.scheduler.scheduleOnce(5.seconds){
        system.terminate()
    }(system.dispatcher)
}


// Test: Closures
object main6 extends App{

    case class Start(a: ActorRef)

    val system = ActorSystem("FutureActorSystem")



    val creator = system.actorOf(
        Props(new Actor {

            def receive = {
                case "Start" =>


                    val f1 = LocalTask(self).Start{
                        println(s"1st LocalTask, ${Thread.currentThread().getName()}")
                        Thread.sleep(1000)
                        println(sender())
                        1 + 1
                    }.SendToActor(sender())

                case x => 
                    Thread.sleep(2000)
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
    ta2 ! Start(creator)

    system.scheduler.scheduleOnce(10.seconds){
        system.terminate()
    }(system.dispatcher)
}


// Some examples of using LocalTask
object main7 extends App{


// This is used to test the feature of using Future sequentially in an actor (LocalTask)

//     In this example, we create an actor that receives a message to fetch data from a URL in future
//     The actor will send the result back to itself

//     Then, the actor will count the number of words in the data in future
//     The actor will send the result back to itself

//     Process: Fetch data from URL -> Count the number of words in the data


    import scala.io.Source

    object TestActor2{

        case class FetchData(url: String)
        case class Data(content: String)
        case object Count

        def props():Props = Props(new TestActor2())
    }



    class TestActor2 extends Actor {

        import context.dispatcher
        import TestActor2._

        var data: LocalTask[String] = null
        // var data: Future[String] = null


        def receive = {

            case FetchData(url) =>

                // Use Our Future to asynchronously fetch data from the URL

                val futureData = LocalTask(self).Start {
                    println(Thread.currentThread().getName)
                    // Fetch data from the URL, Send the data back to the self as a Data message
                    Source.fromURL(url).mkString
                }

                // val futureData = Future {

                //     println(Thread.currentThread().getName)
                //     // Fetch data from the URL, Send the data back to the self as a Data message
                //     Source.fromURL(url).mkString
                // }


                // Assign the futureData to the data variable
                data = futureData

                self ! Count
                
                futureData.NextStep(s => Data(s)).SendToActor(self)
                // futureData.map(s => Data(s)).pipeTo(self)


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

                val futureWordCount = data.NextStep { data =>
                    println(Thread.currentThread().getName)
                    // Count the number of words in the data
                    data.split("\\s+").length
                }

                // val futureWordCount = data.flatMap { data =>
                //     Future {
                //     println(Thread.currentThread().getName)
                //     // Count the number of words in the data
                //     data.split("\\s+").length
                //     }
                // }


                futureWordCount.NextStep(count => Data(count.toString())).SendToActor(self)
                // futureWordCount.map(count => Data(count.toString())).pipeTo(self)

            case msg =>

                println(s"Undefined Msg: $msg")
        }

    


    }

    import TestActor2._

    // Create the actor system
    val system = ActorSystem("MyActorSystem2")

    // Create the actor
    val myActor = system.actorOf(TestActor2.props(), "myActor")

    // Send a FunctionMessage
    myActor ! FetchData("https://jsonplaceholder.typicode.com/posts/1")
    // myActor ! FetchData("https://jsonplaceholder.typicode.com/posts/1")

    // for (i <- 1 to 10) {
    //     // Thread.sleep(1000)
    //     myActor ! i
    // }

    system.scheduler.scheduleOnce(10.seconds){
        system.terminate()
    }(system.dispatcher)

}



// Same examples did in early implementation
object main8 extends App{
    
        val testing_f = (i: Int) => i

        val testing_f2 = (i: Int, t: Duration) => {
            val start_time = System.currentTimeMillis()
            while (System.currentTimeMillis() <= start_time + t.toMillis){} 
            i+1}


        val system = ActorSystem("FutureActorSystem")
        val creator = system.actorOf(
            Props(new Actor {
    
                var counter = 0
                var start: Long = _
                var end: Long = _
                def receive = {
                    case "Start" =>
                        start = System.currentTimeMillis()

                        // concurrent testing
                        for (i <- 1 to 1000){
                            val f = LocalTask(self).Start{
                                testing_f2(i, 1000.millis)
                            }
                            f.GetResultByActor
                        }

                        // single long running task testing
                        // val f = LocalTask(self).Start{
                        //     // testing_f(1)
                        //     testing_f2(1, 1.millis)
                        // }.GetResultByActor
                        
                    case i: Int => 
                        counter += 1

                        // concurrent testing
                        if (counter == 1000){
                            end = System.currentTimeMillis()
                            println(s"Time: ${end - start} ms, # of task: $counter")


                            val pw = new PrintWriter(new FileOutputStream(new File("fututre_actor_log_1000_1000.txt"), true))
                            pw.write(s"Execution time: ${end - start} ms, # of task = $counter\n")
                            pw.close()
                        }

                        // single long running task testing
                        // if (counter == 1){
                        //     end = System.currentTimeMillis()
                        //     println(s"Time: ${end - start} ms")

                            // val pw = new PrintWriter(new FileOutputStream(new File("fututre_actor_log_1.txt"), true))
                            // pw.write(s"Execution time: ${end - start} ms\n")
                            // pw.close()
                        // }



                   }
            }))
        
        creator ! "Start"
}
