package ActorWithExecutorService


import akka.actor.{ Actor, Props }
import java.util.concurrent.{ Executors, ExecutorService }

// Define the messages
case object Start
case object Stop
case object killthread



// In a typical ExecutorService, you don't have control over which thread is used to execute a specific task. 
// The ExecutorService manages a pool of threads and assigns tasks to them as they become available.

class MyActor extends Actor {
    // Create the ExecutorService
    val executor: ExecutorService = Executors.newFixedThreadPool(10)

    var thread: Thread = null

    def receive = {
        case Start =>
            // Submit tasks to the executor

            executor.submit(new Runnable {
                def run(): Unit = {
                    // Replace this with your task
                    self ! Thread.currentThread

                    Thread.sleep(3000)
                    println(s"Task 0 running on thread ${Thread.currentThread.getName}")
                }
            })


            for (i <- 1 to 20) {
                executor.submit(new Runnable {
                    def run(): Unit = {
                        // Replace this with your task
                        Thread.sleep(1000)
                        println(s"Task $i running on thread ${Thread.currentThread.getName}")
                    }
                })
            }


        case Stop =>
            // Shut down the executor
            executor.shutdown()
        
        case t: Thread =>
            
            println(t.getName())
            thread = t

        case killthread =>

            thread.stop()
    }
}

object MyActor {
    def props: Props = Props[MyActor]
}


object Main extends App {
    import akka.actor.ActorSystem

    val system = ActorSystem("MySystem")

    val myActor = system.actorOf(MyActor.props, "myactor")

    myActor ! Start
    // myActor ! Stop
    import system.dispatcher

    import scala.concurrent.duration._

    system.scheduler.scheduleOnce(1000.milliseconds) {
        myActor ! killthread
    }

}