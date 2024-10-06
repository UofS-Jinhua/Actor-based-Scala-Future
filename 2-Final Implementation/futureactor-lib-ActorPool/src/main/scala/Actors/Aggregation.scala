package Actors

import Tasks._

import akka.actor._
import scala.concurrent.duration.Duration
import scala.concurrent.{Awaitable, CanAwait}
import scala.concurrent.Await
import scala.util.control.Breaks._

object Aggregate_Actor{

    case class Find(LocalTask: LocalTask[_], w_time: Duration, id: Int)
    case class GetResult(askActor: ActorRef)
    case class FirstCompleted(tasks: Seq[LocalTask[?]], w_time: Duration)
    case object sendFinish


    def props[T](creator: ActorRef): Props = Props(new Aggregate_Actor(creator)).withDispatcher("FJ-dispatcher")
}


class Aggregate_Actor[T](creator: ActorRef) extends Actor with ActorLogging with Stash {

    import Aggregate_Actor._
    import context.dispatcher

    val recipient = creator


    def Aggregating(taskMap: Map[Int, Any] = Map.empty[Int, Any]): Receive = {


        case FirstCompleted(tasks, w_time) => 

            log.info(s"Aggregate_Actor receive FirstCompleted($tasks, $w_time)")
            var completedTask: Option[(LocalTask[_], Int)] = None
            var start = System.currentTimeMillis()
            while (completedTask.isEmpty) {
                completedTask = tasks.zipWithIndex.find { case (task, _) => task.IsCompleted }
                var end = System.currentTimeMillis()
                if (end - start > w_time.toMillis) {
                    log.info(s"Aggregate_Actor: TimeOut")
                    context.become(Done(taskMap))
                    unstashAll()
                    break
                }
            }

            completedTask.foreach { case (task, i) =>
                val task_result = Await.result(task, w_time)
                // log.info(s"Aggregate_Actor receive FirstCompleted($tasks, $w_time), result: $task_result")
                context.become(Done(taskMap + (i -> task_result)))
                unstashAll()
            }

            
        case Find(task, w_time, id) =>

            // log.info(s"Aggregate_Actor receive Find($task, $w_time, $id)")
            val task_result = Await.result(task, w_time)
            // log.info(s"Aggregate_Actor receive Find($task, $w_time, $id), result: $task_result")
            context.become(Aggregating(taskMap + (id -> task_result)))

        case GetResult(askActor) =>
            stash()

        case sendFinish =>
            context.become(Done(taskMap))
            unstashAll()
        

    }   

    def Done(Result: Map[Int, Any]): Receive = {
        case GetResult(askActor) =>
            // log.info(s"Aggregate_Actor receive GetResult, result: $Result ")
            askActor ! Result
    }


    def receive = Aggregating()
}