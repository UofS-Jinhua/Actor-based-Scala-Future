package ActorWithExecutorService

import akka.actor._
import scala.collection.immutable._
import java.util.concurrent.{ Executors, ExecutorService }
import scala.concurrent.ExecutionContext

object TaskManager{

  case class Task(f: ()=> Any)

  case class TaskWithParam[A, B](f: A => B, param: A)

  case class Result(out: Any)

  // TD: TaskDone: (output, taskNum)
  case class TD(out: Any, tn: Int)

  case class TaskNumber(tn: Int)

  // TP_info: TaskPool_info
  case class TP_info(taskNum: Int, t: Thread)

  // Task control messages
  // PauseTask: Pause the task with taskNum tn
  // ResumeTask: Resume the task with taskNum tn
  // KillTask: Kill the task with taskNum tn
  case class PauseTask(tn: Int)

  case class ResumeTask(tn: Int)

  case class KillTask(tn: Int)

  def props(pool_size: Int = 10): Props = Props(new TaskManager(pool_size))

}


class TaskManager(pool_size: Int = 10) extends Actor {

  import TaskManager._

  val es: ExecutorService = Executors.newFixedThreadPool(pool_size)
  val ec: ExecutionContext = ExecutionContext.fromExecutorService(es)

  var taskNum: Int = 0

  override def postStop(): Unit = {
    es.shutdown()
  }

  def TaskDelivering[A, B](
    TaskMap: Map[Int, (Task, Boolean, Any, ActorRef, Thread)] = 
             Map.empty[Int, (Task, Boolean, Any, ActorRef, Thread)]): Receive ={
    
    // TaskMap: Map[Int, (Task, Boolean, Any, ActorRef, Thread)]
    //                         - key: taskNum
    //                         - value: (Task, isDone, Result, recipient, thread)

    case Task(f) =>

        taskNum += 1
        val recipient = sender()
        val task = Task(f)
        val tn = taskNum
        val creator = self
        

        val new_runnable = new Runnable {

          val capturedTask = task
          val capturedTn = tn
          val capturedCreator = creator
          def run(): Unit = {
            
            val cur_t = Thread.currentThread
            capturedCreator ! TP_info(tn, cur_t)

            // println(s"Task $capturedTn running on thread ${cur_t.getName}")

            val out = capturedTask.f()

            creator ! TD(out, capturedTn)

          }
        }

        ec.execute(new_runnable)

        recipient ! TaskNumber(tn)

        context.become(TaskDelivering(TaskMap + (tn -> (task, false, null, recipient, null))))
    
    
    case TaskWithParam(f, param) =>

        taskNum += 1
        val recipient = sender()
        val task = TaskWithParam(f, param)
        val tn = taskNum
        val creator = self
        

        val new_runnable = new Runnable {

          val capturedTask = task
          val capturedTn = tn
          val capturedCreator = creator
          def run(): Unit = {
            
            val cur_t = Thread.currentThread
            capturedCreator ! TP_info(tn, cur_t)

            // println(s"Task $capturedTn running on thread ${cur_t.getName}")

            val out = capturedTask.f(capturedTask.param)

            creator ! TD(out, capturedTn)

          }
        }

        ec.execute(new_runnable)

        // recipient ! TaskNumber(tn)

        context.become(TaskDelivering(TaskMap + (tn -> (null, false, null, recipient, null))))

    case TP_info(tn, t) =>

        val taskInfo = TaskMap(tn)

        context.become(TaskDelivering(TaskMap.updated(tn, (taskInfo._1, taskInfo._2, taskInfo._3, taskInfo._4, t))))

    case TD(out, tn) =>

        // if out is Unit
        if (out.isInstanceOf[Unit]){

          val taskInfo = TaskMap(tn)

          context.become(TaskDelivering(TaskMap.updated(tn, (taskInfo._1, true, taskInfo._3, taskInfo._4, taskInfo._5))))
        }else{

          val taskInfo = TaskMap(tn)

          taskInfo._4 ! Result(out)

          context.become(TaskDelivering(TaskMap.updated(tn, (taskInfo._1, true, out, taskInfo._4, taskInfo._5))))
        }
    
    case PauseTask(tn) =>

        val taskInfo = TaskMap(tn)

        taskInfo._5.suspend()
    
    case ResumeTask(tn) =>
          
          val taskInfo = TaskMap(tn)
  
          taskInfo._5.resume()
    
    case KillTask(tn) =>
            
            val taskInfo = TaskMap(tn)
    
            taskInfo._5.stop()
  
            context.become(TaskDelivering(TaskMap - tn))
        
    case msg =>

        // Do nothing

  }

  def receive = TaskDelivering()

}