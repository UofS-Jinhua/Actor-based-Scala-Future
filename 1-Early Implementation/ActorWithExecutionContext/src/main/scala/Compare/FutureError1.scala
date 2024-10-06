package Compare

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration.Inf
import scala.concurrent.duration.Duration

object FutureError1 {
  def main(args: Array[String]): Unit = {
    // initial conditions
    var count = 0
    val futureNumb = 100000
    val calcu_to = 10000
    // implicit global execution context
    import scala.concurrent.ExecutionContext.Implicits.global
    val futures = for (j <- 1 to futureNumb) yield Future {
      // write your logic here
      var sum = 0
      for (i <- 1 to calcu_to) {
        sum += i
      }
      // cloure the count to the future, error prone
      count += 1
      // send result back
      sum
    }

    // await all futures
    futures.foreach(Await.result(_, Duration.Inf))

    printf(s"count is $count \n")
    // the count should be 100000, but it is not and every time it is different
  }
}
