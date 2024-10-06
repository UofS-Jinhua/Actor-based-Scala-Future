package Tasks

import Actors._
import akka.actor._
import scala.concurrent.duration.Duration
import scala.concurrent.{Awaitable, CanAwait}
import Test.future_main2.temp
import akka.actor.ProviderSelection.Local



// The companion object of LocalTask
// It contains the initialization of the actor pool
// It also contains the minimum number of workers
// It also contains the actor pool

object LocalTask {
  
  // Initialize the actor pool
  private var init_flag: Boolean = false

  // The default number of workers
  private var defaultNumWorkers: Int = _
 
  // The minimum number of workers
  private var minimumWorkers: Int = _
  
  // The actor pool
  var actorPool_Workers: List[ActorRef] = _




  /*
    * Initialize the actor pool
    * @param Num_actor: the number of actors in the pool
    * @param Min: the minimum number of workers
    * @param system: the actor system
  */
  private def init( Num_actor: Int = defaultNumWorkers, Min: Int = minimumWorkers)(implicit system: ActorSystem): Unit = {

    LocalTask.synchronized{
        if (init_flag) {
            return
        }

        init_flag = true
        setMinimumWorkers(Min)
        setDefaultNumWorkers(Num_actor)
        actorPool_Workers = (for (i <- 1 to Num_actor) yield system.actorOf(Worker.props())).toList
        println(s"[$this]: [Init flag = $init_flag], the length of the actor pool is: ${actorPool_Workers.length}")

        // Create a pool monitor
        system.actorOf(PoolMonitor.props(Num_actor, Min))
    }
  }
  
  

  /*
    * Set the minimum number of workers
    * @param num: the number of workers
  */
  private def setMinimumWorkers(num: Int): Unit = {
    LocalTask.synchronized{
        minimumWorkers = num
    }
  }


   /*
   * Set the default number of workers
   * @param num: the number of workers
  */
  private def setDefaultNumWorkers(num: Int): Unit = {
    LocalTask.synchronized{
        defaultNumWorkers = num
    }
  }


  /*
    * Get the first actor from the pool
    * @param system: the actor system
  */
  private def getActor(implicit system: ActorSystem): ActorRef = {
    this.synchronized{


        // Dynamic increase the number of workers
        // if (actorPool_Workers.length < minimumWorkers) {
        //     while (actorPool_Workers.length < minimumWorkers) {
        //         val newActor = system.actorOf(Worker.props())
        //         actorPool_Workers = newActor :: actorPool_Workers
        //     }
        // }


        // println(s"[$this], the length of the actor pool is: ${actorPool_Workers.length}")
        // If the actor pool is empty, 
        // => print a message and create a new worker for the task

        if (actorPool_Workers.isEmpty) {
            println("The actor pool is empty. Created a new worker...")
            system.actorOf(Worker.props())
        }else{

        // Get the first actor from the pool
        val actor = actorPool_Workers.head

        // println(s"Given actor: ${actor.path}")
        actorPool_Workers = actorPool_Workers.tail
        actor
        

        }


    }
  }

  // Some helper functions
  // - getPoolSize: get the size of the actor pool
  // - getMinimumWorkers: get the minimum number of workers
  // - getDefaultNumWorkers: get the default number of workers
  private def getPoolSize: Int = {
    actorPool_Workers.length
  }

  private def getMinimumWorkers: Int = {
    minimumWorkers
  }

  private def getDefaultNumWorkers: Int = {
    defaultNumWorkers
  }


  /*
        * Add actors to the actor pool
        * @param num: the number of actors to be added
        * @param system: the actor system
  */
  private def addingActor(num: Int)(implicit system: ActorSystem): Unit = {
    // LocalTask.synchronized{
        for (i <- 1 to num) {
            val actor = system.actorOf(Worker.props())
            actorPool_Workers = actor :: actorPool_Workers
        }
        println(s"After adding $num actors, the current length of the actor pool is: ${actorPool_Workers.length}")
    }
//   }




}




class LocalTask[T](creator: ActorRef, prev_worker: ActorRef = null, cur_worker: ActorRef = null, canAwait: Boolean = false)
                 (implicit context: ActorContext) extends Task[T] {

    import Worker._


    final private var isStopped: Boolean = false
    final private var result: Option[T] = None
    final private var isDone: Boolean = false
    final private var canWait: Boolean = canAwait

    // Create a temp actor to get the result from the current actor
    final private var tempActor = if (canWait && cur_worker != null) context.actorOf(TempActor.props(cur_worker)) else null

    
    
    // ------------------------------------------------------------------------------------------------

    // Initialize the actor pool
    def ActorPool_Init(Num_actor: Int, Min: Int): Unit = {

        println(s"The system is: ${context.system}")
        LocalTask.init(Num_actor, Min)(context.system)
    }

    // Get the current number of workers in the actor pool
    def getPoolSize: Int = {
        LocalTask.getPoolSize
    }


    def createWorker(num: Int): Unit = {
        LocalTask.addingActor(num)(context.system)
    }

    // ------------------------------------------------------------------------------------------------

    def Start(op: () => T): LocalTask[T] = {

        // Get a worker from the actor pool
        if (prev_worker == null && cur_worker == null) {
            // println(s"LocalTask [$this]: Start the task")
            val worker = LocalTask.getActor(context.system)

            // change the behavior of the worker, and start the task
            worker ! FirstWorker
            worker ! Worker.Start(op.asInstanceOf[() => Any])

            new LocalTask[T](creator, null, worker)

        }else{
            throw new Exception("Start should be called only once")
        }
    }


    def Zip[U](that: LocalTask[U]): LocalTask[(T, U)] = {
        if (cur_worker == null) {
            throw new Exception("Cannot zip with an empty task")
        }
        else {

            // Get a worker from the actor pool
            val new_worker = LocalTask.getActor(context.system)

            // change the behavior of the worker, and start the task： Zip
            new_worker ! ZipWorker
            new_worker ! GetResults(cur_worker, that.GetCurrentWorker)


            new LocalTask[(T, U)](creator,null, new_worker)
        }
    }


    def Group[T](tasks: Seq[LocalTask[T]]): LocalTask[List[T]] = {
        if (prev_worker == null && cur_worker == null) {


            // Get the workers from the tasks
            val workers = for (task <- tasks) yield task.GetCurrentWorker
            // Get a worker from the actor pool            
            val groupActor = LocalTask.getActor(context.system)

            // change the behavior of the worker, and start the task： Group
            groupActor ! GroupWorker
            groupActor ! FromWorkers(workers.toList)

            new LocalTask[List[T]](creator, null, groupActor)
        }else{
            throw new Exception("Start should be called only once")
        }
    }



    def NextStep[U](op2: T => U): LocalTask[U] = {
        if (prev_worker == null && cur_worker == null) {
            throw new Exception("First task has not been created, Start() should be called first")
        }
        else{
            // Get a worker from the actor pool
            val new_worker = LocalTask.getActor(context.system)

            // change the behavior of the worker, ask for the previous result, and start the task： op2
            new_worker ! NextWorker
            new_worker ! GetPre_R(cur_worker)
            new_worker ! SubTask(op2.asInstanceOf[Any => Any])


            new LocalTask[U](creator,cur_worker, new_worker)
        }
    }

    def IsCompleted: Boolean = {
        isDone
    }

    def CanItWait: Boolean = {
        canWait
    }

    def GetResult: T = {
        if (cur_worker == null) {
            throw new Exception("No task has been created yet.")
        }
        else if (!isDone){
            throw new Exception("Task has not been completed yet.")
        }else{
            result.get
        }
    }


    def GetResultByActor: Unit = {
        if (cur_worker == null) {
            throw new Exception("No task has been created yet.")
        }else{
            this.SendToActor(creator)
        }
    }

    def Stop: Unit = {
        if (cur_worker != null && !isStopped) {
            context.stop(cur_worker)
            isStopped = true
        }
        else{
            throw new Exception("No task has been created yet.")
        }
    }

    def Recover(default_r: T): LocalTask[T] = {
        if (cur_worker == null) {
            throw new Exception("No task has been created yet.")
        }
        else{
            cur_worker ! Default_Result(default_r)
            this
        }

    }

    def RecoverWith(func: () => T): LocalTask[T] = {
        if (prev_worker != null) {
            throw new Exception("This RecoverWith should be only called for the first task")
        }
        else if (cur_worker == null) {
            throw new Exception("No task has been created yet.")
        }
        else{
            cur_worker ! Error_Handling_1(func)
            this
        }
    }

    def RecoverWith(func: Any => T): LocalTask[T] = {
        if (prev_worker == null) {
            throw new Exception("This RecoverWith should be only called for the rest tasks")
        }
        else if (cur_worker == null) {
            throw new Exception("No task has been created yet.")
        }
        else{
            cur_worker ! Error_Handling_2(func)
            this
        }
    }

    def GetCurrentWorker: ActorRef = {
        if (cur_worker == null) {
            throw new Exception("No task has been created yet.")
        }
        else{
            cur_worker
        }
    }


    def SendToActor(actor: ActorRef): LocalTask[T] = {
        if (cur_worker == null) {
            throw new Exception("No task has been created yet.")
        }
        else{
            cur_worker ! AskResult(actor)
            this
        }
    }


    def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {

        this.synchronized{

            if (tempActor == null) {
                canWait = true
                tempActor = context.actorOf(TempActor.props(cur_worker))
            }
            if (!isDone) {
                try{
                    this.wait(atMost.toMillis)
                }catch{
                    case e: Exception =>
                        println(e)
                
                }
            }
        }
        if (isDone) {
            this
        }else{
            println(s"Cannot get the result in $atMost time")
            this
        }
    }


    def result(atMost: Duration)(implicit permit: CanAwait): T = {

        this.synchronized{

            if (tempActor == null) {
                canWait = true
                tempActor = context.actorOf(TempActor.props(cur_worker))
            }
            if (!isDone) {
                try{
                    this.wait(atMost.toMillis)
                }catch{
                    case e: Exception =>
                        println(e)
                
                }
            }
        }
        if (isDone) {
            result.get
        }else{
            println(s"Cannot get the result in $atMost time")
            None.asInstanceOf[T]
        }
    }


    private [this] def setresult(r: T): Unit = {
        this.synchronized {
            result = Some(r)
            isDone = true
            this.notifyAll() // or this.notify() wake up a single thread
        }
    }



    object TempActor{
        def props(wa: ActorRef): Props = Props(new TempActor(wa)).withDispatcher("FJ-dispatcher")
    }


    class TempActor(worker: ActorRef) extends Actor with ActorLogging{

        override def preStart(): Unit = {
            if (worker != null) {
                worker ! AskResult(self)
            }
        }
        def receive = {
            case out =>
                // log.info(this.context.parent.path.name)
                out match
                    case x: Exception =>
                        isDone = true
                        tempActor = null
                    case _ =>
                        setresult(out.asInstanceOf[T])

                this.context.stop(self)
        }



    }
}