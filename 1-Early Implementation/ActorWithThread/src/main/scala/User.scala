import scala.collection.immutable._
import akka.actor._


object User{
  def props(TM: ActorRef): Props = Props(new User(TM))
}

class User(TM: ActorRef) extends Actor{

    import TaskManager._
    import TaskDistribute._

    def receive: Receive = {

        case TResult(result, ta) => 

            println(s"Get result [$result] from [$ta]")

        
        case map: Map[ActorRef, Any] =>

            println("Get map:")
            for ((k, v) <- map) {
                println(s"$k -> $v")
            }
        
        case Result(msg) =>

            println(s"User Get result: [$msg]")

        case msg =>

            TM ! msg  
        


    }

}