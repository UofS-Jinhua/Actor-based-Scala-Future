import scala.collection.immutable._
import akka.actor._


object User_t{
  def props(TM: ActorRef): Props = Props(new User_t(TM))
}

class User_t(TM: ActorRef) extends Actor{

    import TaskManager._
    import TaskDistribute._

    var counter = 0
    var start = System.currentTimeMillis()
    var end = System.currentTimeMillis()


    def receive: Receive = {

        case TResult(result, ta) => 

            // println(s"Get result [$result] from [$ta]")
            counter += 1

            if (counter == 1){
                end = System.currentTimeMillis()
                println(s"Time elapsed: ${end - start} ms")
            }
        
        case Task(f, p) =>

            if (counter == 0){
                start = System.currentTimeMillis()
            }

            TM ! Task(f, p)
        
        case Submitted_Task =>

            TM ! Submitted_Task

        case msg =>

            println(s"User: get unknown message [$msg]")

        


    }

}