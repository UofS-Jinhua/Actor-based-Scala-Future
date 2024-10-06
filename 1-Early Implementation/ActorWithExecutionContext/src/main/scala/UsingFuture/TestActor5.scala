package UsingFuture

import akka.actor._
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


// Define the message types
case object GetPrice
case object ComparePrices
case class Price(value: Double)
case class ComparePrices(provider1: ActorRef, provider2: ActorRef)


class Customer(TicketService: ActorRef) extends Actor{

  implicit val timeout: Timeout = Timeout(5.seconds)

  def receive: Receive = {
    
    case GetPrice => 

      TicketService ! GetPrice

    case Price(price) =>

      println(s"The lowest price is: $price")
  }
}

class PriceComparator extends Actor {

  implicit val timeout: Timeout = Timeout(5.seconds)

  var Customer: Option[ActorRef] = None

  def receive = {

    case GetPrice =>

      Customer = Some(sender())

    case ComparePrices(provider1, provider2) =>

      val futurePrice1 = (provider1 ? GetPrice).mapTo[Price]
      val futurePrice2 = (provider2 ? GetPrice).mapTo[Price]

      for {
        Price(price1) <- futurePrice1
        Price(price2) <- futurePrice2
      } yield {
        val lowestPrice = Math.min(price1, price2)
        Customer.get ! Price(lowestPrice)
      }
  }
}

class TicketProvider1 extends Actor {
  def receive = {
    case GetPrice => sender() ! Price(100.0)
  }
}

class TicketProvider2 extends Actor {
  def receive = {
    case GetPrice => sender() ! Price(200.0)
  }
}

object TestActor5 extends App{
    val system = ActorSystem("MinPriceFinder")

    val ProviderA = system.actorOf(Props[TicketProvider1], "P1")
    val ProviderB = system.actorOf(Props[TicketProvider2], "P2")
    val comparator = system.actorOf(Props[PriceComparator], "comparator")
    val customer = system.actorOf(Props(new Customer(comparator)), "customer")

    customer ! GetPrice
    comparator ! ComparePrices(ProviderA, ProviderB)
}