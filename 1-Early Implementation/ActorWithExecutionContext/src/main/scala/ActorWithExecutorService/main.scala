package ActorWithExecutorService

import akka.actor.{ Actor, Props, ActorRef, ActorSystem }
import java.time.Duration
import scala.concurrent.duration._
import akka.pattern.after
import scala.concurrent.ExecutionContext.Implicits.global



object Test1 extends App{


    object User{
    
    case object Increment
    case object SendTask

    def props(TM: ActorRef): Props = Props(new User(TM))
    }

    class User(TM: ActorRef) extends Actor{

        // import TaskManager._
        import User._
        import TaskManager._

        var counter_i = 0
        var startT:Long = _
        var endT:Long = _
        val task = (i: Int) => i

        def receive: Receive = {
            case "Start" =>
                startT = System.currentTimeMillis()
                
                for (i <- 1 to 1) TM ! TaskWithParam(task, i)

            case Result(out) =>

                counter_i += 1
                if (counter_i == 1){
                    endT = System.currentTimeMillis()
                    println(s"time:  + ${endT - startT} ms")
                }
            case msg =>
                println(s"Unknown message: $msg")
        }
    }


    val system = ActorSystem("T1_System")

    val taskDeliverer = system.actorOf(TaskManager.props(), "taskDeliverer")

    val user = system.actorOf(User.props(taskDeliverer), "User")

    user ! "Start"

    system.scheduler.scheduleOnce(2.second){
        system.terminate()
    }




}


