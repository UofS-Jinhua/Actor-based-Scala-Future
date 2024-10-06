package Tasks

import akka.actor.ActorRef
import scala.concurrent.{Awaitable, CanAwait}
import scala.concurrent.duration.Duration


/**
 * Abstract class representing a task with a result of type `T`.
 *
 * @tparam T the type of the result of the task
 */
abstract class Task[T] extends Awaitable[T]{


    /**
     * Starts the task with the given operation.
     *
     * @param op the operation to start the task
     * @return a `GetTask` instance containing the result of the operation
     */
    def Start(op: => T): Task[T]


    /**
     * Combines the result of the current task with the result of the given task.
     *
     * @param task the task to combine with the current task
     * @tparam U the type of the result of the given task
     * @return Task[(T, U)]: A new task instance containing the result of current task and the given task
     */
    def Zip[U](task: LocalTask[U]): Task[(T, U)]


    /**
      * Combines the result of serveral tasks with the same type, return a new task instance containing the result of all tasks.
      * @param tasks the tasks to combine with
      * @tparam T the type of the result of the given tasks
      * @return Task[List[T]]: A new task instance containing the result of all tasks
      */
      def Group[T](tasks: Seq[LocalTask[T]]): Task[List[T]]


    /**
     * Returns a new `Task` instance containing the result of the operation `op2` applied to the result of the current task.
     *
     * @param op2 the operation to apply to the result of the current task
     * @tparam U the type of the result of the new task
     * @return a `Task` instance containing the result of the operation `op2` applied to the result of the current task
     */
    def NextStep[U](op2: T => U): Task[U]


    /**
     * Returns `true` if the task has been completed, `false` otherwise.
     *
     * @return `true` if the task has been completed, `false` otherwise
     */
    def IsCompleted: Boolean


    /**
     * Returns `true` if the task can wait, `false` otherwise.
     *
     * @return `true` if the task can wait, `false` otherwise
     */
    def CanItWait: Boolean

    
    /**
     * Returns the result of the task.
     *
     * @return the result of the task
     */
    def GetResult: T

    /**
     * Returns the result of the task by sending a message to the actor who created the task.
     */
    def GetResultByActor: Unit

    /**
     * Stops the task. Kill the actor who is doing the task.
     */
    def Stop: Unit


    /**
     * Returns the current `Task` instance. This method is used to replace the current failed operation with a default value.
     *
     * @param default_r the default value to replace the current operation result that has failed
     * @tparam T the type of the result of the default value
     * @return the current `Task` instance
     */
    def Recover(default_r: T): Task[T]


    /**
     * Notice: This RecoverWith is used for the first task.
     * /
     * 
     * Returns the current `Task` instance. This method is used to replace the current failed operation with a new operation.
     *
     * @param func the operation to replace the current operation that has failed
     * @tparam T the type of the result of the new operation
     * @return the current `Task` instance
     */
    def RecoverWith(func:() => T): Task[T]


    /**
     * Notice: This RecoverWith is used for the rest task.
     * /
     * 
     * Returns the current `Task` instance. This method is used to replace the current failed operation with a new operation.
     *
     * @param func the operation to replace the current operation that has failed
     * @tparam T the type of the input of the new operation
     * @tparam U the type of the result of the new operation
     * @return the current `Task` instance
     */
    def RecoverWith(func: Any => T): Task[T]


    /**
     * Returns the current Worker ActorRef for this operation.
     *
     * @return the current worker actorRef
     */
    def GetCurrentWorker: ActorRef


    /**
     * Send the current task result to the given actor when the task is completed.
     *
     * @param actor the actor which will receive the result of the task
     * @return the current `Task` instance
     */
    def SendToActor(actor: ActorRef): Task[T]



    /**
     * Ready method for Awaitable
     * @param atMost the maximum wait time
     * @return the current `Task` instance
     */
    def ready(atMost: Duration)(implicit permit: CanAwait): this.type


    /**
     * Result method for Awaitable
     * @param atMost the maximum wait time
     * @return the result of the task
     */
    def result(atMost: Duration)(implicit permit: CanAwait): T

}
