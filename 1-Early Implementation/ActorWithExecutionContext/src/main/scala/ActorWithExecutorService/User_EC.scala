package ActorWithExecutorService

import scala.collection.immutable._
import akka.actor._


object User_EC{
  
  case object Increment
  case object SendTask

  def props(TM: ActorRef): Props = Props(new User_EC(TM))
}

class User_EC(TM: ActorRef) extends Actor{

    // import TaskManager._
    import User_EC._
    import TaskManager._

    var tid: Int = -1
    var result: Any = null
    var  counter_i = 0

    def receive: Receive = {

        case Increment =>

            counter_i += 100

        case TaskNumber(tn) => 

            tid = tn
            println(s"Task number: $tid")            
        
        case SendTask =>

            // val cur_counter = counter_i
            // TM ! Task(() => {
                
            //     Thread.sleep(2000)
            //     val new_counter = cur_counter + 1
            //     println(s"current counter: $counter_i -> $new_counter (new counter)")
            // })

            TM ! TaskWithParam((i: Int) => {
                
                Thread.sleep(2000)
                val new_counter = i + 1
                println(s"current counter: $counter_i, new counter: $new_counter")
                new_counter
            }, counter_i)


        
        case Task(f) =>

            TM ! Task(f)

        case TaskWithParam(f, param) =>

            TM ! TaskWithParam(f, param)

        case Result(out) =>

            result = out
            println(s"Task $tid result: $result")

        case KillTask(tn) =>

            if (tid != -1) {
                TM ! KillTask(tid)}
            else {
                self ! KillTask(tn)}

        case msg =>

            TM ! msg  

    }

}