package Compare

import scala.concurrent.Await
import scala.concurrent.duration.Duration
object MeasureFuture {
  def main(args: Array[String]): Unit = {
    import scala.concurrent.Future
    import scala.concurrent.ExecutionContext.Implicits.global

    // initial conditions
    val count = 100000
    val calcu_to = 10000
    val start_time2 = System.currentTimeMillis()
    print(s"received test future message, current time is $start_time2 \n")
    // val futures: Seq[Future[Int]] = Array.fill(count)(Future {
    //   var sum = 0
    //   for (i <- 1 to calcu_to) {
    //     sum += i
    //   }
    //   sum
    // })
    val futures = for (j <- 1 to count) yield Future {
      // write your logic here
      var sum = 0
      for (i <- 1 to calcu_to) {
        sum += i
      }
      // send result back
      sum
    }
    println(s"Future length is ${futures.length}, spend time is ${System.currentTimeMillis() - start_time2}ms")
    // await all futures
    futures.foreach(Await.result(_, Duration.Inf))
    val end_time2 = System.currentTimeMillis()
    println("Future run time: " + (end_time2 - start_time2) + "ms \n")
    print(s"All futures have run, current time is $end_time2 \n")
  }
}
