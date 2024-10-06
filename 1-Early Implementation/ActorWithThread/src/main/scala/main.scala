import akka.actor.{ Actor, Props, ActorRef, ActorSystem }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


// Test 1: send a task to the taskManager, and get the result back
object Test1 extends App{

    import TaskManager._

    val system = ActorSystem("T1_System")

    val taskManager = system.actorOf(Props[TaskManager], "taskManager")


    val user = system.actorOf(User.props(taskManager), "user")

    user ! Task((s: String) => 
            {
                Thread.sleep(5000)
                s.appended('!')}, 
            "Hello, World!")
    
    user ! Pause
    user ! ResumeTask

}

// Test 2: send a task to the taskDistribute, and get the statusMap back
object Test2 extends App{

    import TaskManager._
    import TaskDistribute._

    val system = ActorSystem("T2_System")

    val taskDistribute = system.actorOf(Props[TaskDistribute], "taskDistribute")

    val user = system.actorOf(User.props(taskDistribute), "user") 

    val task = (s: String) => 
            {
                Thread.sleep(5000)
                s.appended('!')
            }
    
    user ! Task(task, "1. Hello, World")
    user ! Task(task, "2. Hello, World")
    user ! Task(task, "3. Hello, World")
    user ! Submitted_Task


    import scala.concurrent.duration._

    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.duration._

    system.scheduler.scheduleOnce(6.seconds) {
        user ! Submitted_Task
    }

}

// Test 3: Doing the same test in PinnedDispatcher implementation
object Test3 extends App{

    import TaskManager._
    import TaskDistribute._

    val system = ActorSystem("T3_System")

    val taskDistribute = system.actorOf(Props[TaskDistribute], "taskDistribute")

    val user = system.actorOf(User_t.props(taskDistribute), "user") 

    val task = (s: Int) => s
    
    for (i <- 1 to 1){
        user ! Task(task, i)
    }

    system.scheduler.scheduleOnce(3.seconds) {
        // user ! Submitted_Task
        system.terminate()
    }



}

