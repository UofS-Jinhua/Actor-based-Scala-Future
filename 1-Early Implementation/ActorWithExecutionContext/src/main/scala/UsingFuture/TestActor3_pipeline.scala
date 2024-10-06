package UsingFuture

import akka.actor._
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.util.Success
import scala.concurrent.Promise
import scala.concurrent.duration._


case class ProcessData(data: String, response: Promise[String])


trait DataProcessor {
  def processData(data: String): String = {
    Thread.sleep(10000)
    data.appended('a')
    }
}


class Stage1() extends Actor with DataProcessor {

  val nextStage = context.actorOf(Props(new Stage2()))

  override def receive: Receive = {
    case ProcessData(data, response) =>
      val processedData = processData(data)
      implicit val timeout = Timeout(5.seconds)
      nextStage ! ProcessData(processedData, response)

      context.system.scheduler.scheduleOnce(5.seconds) {
        println(s"Processed data after 5 seconds: $response")
      }(context.dispatcher)
  }
}

class Stage2 extends Actor with DataProcessor {

  val nextStage = context.actorOf(Props(new Stage3()))

  override def receive: Receive = {
    case ProcessData(data, response) =>
      val processedData = processData(data)
      implicit val timeout = Timeout(5.seconds)
      nextStage ! ProcessData(processedData, response)
  }
}

class Stage3 extends Actor with DataProcessor{

  override def receive: Receive = {
    case ProcessData(data, response) =>
      val processedData = processData(data)
      println(processedData)
      response.complete(Success(processedData))
  }
}

object TestActors_3 extends App {

  val MyActorSystem3 = ActorSystem("MyActorSystem3")
  
  val stage1 = MyActorSystem3.actorOf(Props(new Stage1()))

  stage1 ! ProcessData("Hello World", Promise[String]())


}