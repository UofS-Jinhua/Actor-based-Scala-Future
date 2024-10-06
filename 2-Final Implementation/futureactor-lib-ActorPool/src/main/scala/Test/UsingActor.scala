package Test

import Tasks._
import CollectResult._
import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.Await





// Same examples did in early implementation
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


                        LocalTask(self).ActorPool_Init(500, 100)

                        // Thread.sleep(1000)

                        start = System.currentTimeMillis()

                        // concurrent testing

                        for (i <- 1 to 1000){
                            val f = LocalTask(self).Start{()=>
                                testing_f2(i, 100.millis)
                            }
                            f.GetResultByActor
                        }



                        // single long running task testing

                        // val f = LocalTask(self).Start{()=>
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
            }))
        
        creator ! "Start"
}
