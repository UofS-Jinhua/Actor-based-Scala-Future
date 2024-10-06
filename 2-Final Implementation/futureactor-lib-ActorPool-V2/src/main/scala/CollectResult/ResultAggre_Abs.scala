package CollectResult


import scala.concurrent.duration.Duration
import scala.concurrent.{Awaitable, CanAwait}
import scala.concurrent.Await
import akka.actor._

import Tasks._


/*
/ Abstract class for result aggregation
*/
abstract class ResultCollect extends Awaitable[Map[Int, Any]]{


    /**
    * LookingFor: try to get the result from tasks. If the results are not available, wait for w_time.
    * @param task: a sequence of Task
    * @param w_time: wait time
    * @return: an ResultCollect object
    */
    def LookingFor(task: Seq[LocalTask[_]], w_time: Duration): this.type


    /**
    * LookingFor: try to get the result from task. If the result is not available, wait for w_time.
    * @param task: a Task
    * @param w_time: wait time
    * @return: an ResultCollect object
    */
    def LookingFor(task: LocalTask[_], w_time: Duration): this.type


    /**
    * Anyof: try to get a result from several tasks. If the result is not available, wait for w_time.
    * @param task: a sequence of Task
    * @param w_time: wait time
    * @return: an ResultCollect object
    */
    def Anyof(task: Seq[LocalTask[_]], w_time: Duration): this.type

    
    /**
    * PrintResult: print the result, if the result is not available, register a callback function to print the result
    */
    def PrintResult: Unit


    def sendToActor(actor: ActorRef): this.type



    /**
     * Ready method for Awaitable
     * @param atMost the maximum wait time
     * @return the current object
     */
    def ready(atMost: Duration)(implicit permit: CanAwait): this.type


    /**
     * Result method for Awaitable
     * @param atMost the maximum wait time
     * @return the result of the result map
     */
    def result(atMost: Duration)(implicit permit: CanAwait): Map[Int, Any]


}