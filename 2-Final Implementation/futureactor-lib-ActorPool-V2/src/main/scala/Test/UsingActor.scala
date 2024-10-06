package Test

import Tasks._
import CollectResult._
import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global




// Test 1:
// - Testing the Sart method of LocalTask
// - Testing the Zip method of LocalTask
// - Testing the Group method of LocalTask
// - Testing the NextStep method of LocalTask

object actor_main1 extends App{


        val system = ActorSystem("FutureActorSystem_1")
        val creator = system.actorOf(
            Props(new Actor {
    
                var counter = 0
                var start: Long = _
                var end: Long = _
                def receive = {
                    case "Start" =>
                        println(s"Creator: Start --- ${Thread.currentThread().getName()}")
                        LocalTask(self).ActorPool_Init(1000, 100)

                        // 1. Testing the Start method of LocalTask
                        val f1 = LocalTask(self).Start{
                            println(s"Task 1 --- ${Thread.currentThread().getName()}")
                            1
                        }.SendToActor(self)

                        val f2 = LocalTask(self).Start{
                            println(s"Task 2 --- ${Thread.currentThread().getName()}")
                            "T2"
                        }.SendToActor(self)

                        // 2. Testing the NextStep method of LocalTask
                        val f3 = f1.NextStep{ i => 
                            println(s"Task 3 --- ${Thread.currentThread().getName()}")
                            i + 2
                        }.SendToActor(self)
                        val f4 = f2.NextStep{ i => 
                            println(s"Task 4 --- ${Thread.currentThread().getName()}")
                            i.length() + 2
                        }.SendToActor(self)


                        // 3. Testing the Zip method of LocalTask
                        val f5 = f1.Zip(f2).SendToActor(self)

                        // 4. Testing the Group method of LocalTask
                        val f6 = LocalTask(self).Group(List(f1, f3, f4)).SendToActor(self)


                        println("Should receive 1, T2, 3, 4, (1, T2), List(1, 3, 4), order may vary")
                        
                    case msg => 

                        println(s"gotten: $msg")

                   }
            }))
        
        creator ! "Start"

        system.scheduler.scheduleOnce(5.seconds){
            system.terminate()
        }
    
}


// Performance Test: 
object actor_main extends App{
    
        val testing_f = (i: Int) => i

        val testing_f2 = (i: Int, t: Duration) => {
            val start_time = System.currentTimeMillis()
            while (System.currentTimeMillis() <= start_time + t.toMillis){} 
            i}


        val system = ActorSystem("FutureActorSystem")
        val creator = system.actorOf(
            Props(new Actor {
    
                var counter = 0
                var start: Long = _
                var end: Long = _
                def receive = {
                    case "Start" =>


                        LocalTask(self, poolsize = 1200, min_num = 100).ActorPool_Init(1000, 100)

                        Thread.sleep(1000)

                        start = System.currentTimeMillis()

                        // concurrent testing

                        for (i <- 1 to 1000){
                            val f = LocalTask(self).Start{
                                testing_f2(i, 1000.millis)
                            }
                            f.SendToActor(self)
                        }



                        // single long running task testing

                        // val f = LocalTask(self).Start{
                        //     // testing_f(1)
                        //     testing_f2(1, 1000.millis)
                        // }.GetResultByActor
                        
                    case i: Int => 
                        counter += 1

                        // concurrent testing


                        if (counter == 1000){
                            end = System.currentTimeMillis()
                            println(s"Time: ${end - start} ms, # of task: $counter")



                        // single long running task testing


                        // if (counter == 1){
                        //     end = System.currentTimeMillis()
                        //     println(s"Time: ${end - start} ms")





                        }



                   }
            }), "creator")
        
        creator ! "Start"
}




// Performance Test: Heavy workload
// - Testing the performance of the LocalTask with 1000 tasks, Poolsize = 100, min_num = 50
object actor_main2 extends App{
    
        val testing_f = (i: Int) => i

        val testing_f2 = (i: Int, t: Duration) => {
            val start_time = System.currentTimeMillis()
            while (System.currentTimeMillis() <= start_time + t.toMillis){} 
            i}


        val system = ActorSystem("FutureActorSystem")
        val creator = system.actorOf(
            Props(new Actor {
    
                var counter = 0
                var start: Long = _
                var end: Long = _
                def receive = {
                    case "Start" =>


                        LocalTask(self, poolsize = 1000, min_num = 50).ActorPool_Init(100, 50)

                        Thread.sleep(2000)

                        start = System.currentTimeMillis()

                        // concurrent testing

                        for (i <- 1 to 1000){
                            val f = LocalTask(self).Start{
                                testing_f2(i, 10.millis)
                            }
                            f.SendToActor(self)
                        }



                        // single long running task testing

                        // val f = LocalTask(self).Start{
                        //     // testing_f(1)
                        //     testing_f2(1, 10.millis)
                        // }.GetResultByActor
                        
                    case i: Int => 
                        counter += 1

                        // concurrent testing


                        if (counter == 1000){
                            end = System.currentTimeMillis()
                            println(s"Time: ${end - start} ms, # of task: $counter")



                        // single long running task testing


                        // if (counter == 1){
                        //     end = System.currentTimeMillis()
                        //     println(s"Time: ${end - start} ms")





                        }



                   }
            }), "creator")
        
        creator ! "Start"
}


// Memory Test: Heavy workload
object actor_main3 extends App{
        import scala.concurrent.duration._
        import scala.language.postfixOps


        val runtime = Runtime.getRuntime
        def printMemoryUsage(stage: String, rungc: Boolean = true): Unit = {
            if (rungc) runtime.gc() // 运行垃圾回收器
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            println(s"Memory used after $stage: ${usedMemory} bytes")
        }
    
        val testing_f = (i: Int) => i

        val testing_f2 = (i: Int, t: Duration) => {
            val start_time = System.currentTimeMillis()
            while (System.currentTimeMillis() <= start_time + t.toMillis){} 
            i}


        val system = ActorSystem("FutureActorSystem")
        val creator = system.actorOf(
            Props(new Actor {
    
                var counter = 0
                var start: Long = _
                var end: Long = _
                def receive = {
                    case "Start" =>

                        Thread.sleep(2000)
                        printMemoryUsage("Start")

                        LocalTask(self, poolsize = 1000, min_num = 0).ActorPool_Init(100, 50)

                        Thread.sleep(2000)

                        start = System.currentTimeMillis()

                        // concurrent testing

                        for (i <- 1 to 1000){
                            val f = LocalTask(self).Start{
                                testing_f2(i, 1.millis)
                            }
                            f.SendToActor(self)
                        }

                        // printMemoryUsage("After task creation")

                        
                    case i: Int => 
                        counter += 1

                        // concurrent testing


                        if (counter == 1000){
                            end = System.currentTimeMillis()
                            println(s"Time: ${end - start} ms, # of task: $counter")

                            printMemoryUsage("End", false)
                        }



                   }
            }), "creator")
        
        creator ! "Start"
}

// Memory Test: 
object actor_main4 extends App{
        import scala.concurrent.duration._
        import scala.language.postfixOps


        val runtime = Runtime.getRuntime
        def printMemoryUsage(stage: String, rungc: Boolean = true): Long = {
            if (rungc) runtime.gc() // 运行垃圾回收器
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            println(s"Memory used after $stage: ${usedMemory} bytes")
            usedMemory /1024
        }
    

        val system = ActorSystem("FutureActorSystem")
        val creator = system.actorOf(
            Props(new Actor {
    
                var counter = 0

                def receive = {
                    case "Start" =>

                        Thread.sleep(2000)
                        val start = printMemoryUsage("Start")
                        LocalTask(self, poolsize = 1, min_num = 0).ActorPool_Init(100, 50)
                        Thread.sleep(2000)
                        val end = printMemoryUsage("After task creation", false)

                        print(s"Memory used: ${end - start} kb")

                   }
            }), "creator")
        
        creator ! "Start"
}