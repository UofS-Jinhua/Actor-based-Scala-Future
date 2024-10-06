package TestingFeature

import akka.actor._


// ------------------------------------------------------------------------------------

// This file is used to test if each actor could be regarded as a future
//     - Code block of Future is executed by some thread in the future
//            - Can it be execute by some actor in the future?
//     - Future is completed with a value or failed with an exception
//            - Can it be completed with a sucessful message or failed message?
//     - Callbacks can be registered on Future
//            - Can it be registered on actor?
//                    - Can messages be regarded as callbacks?

// ------------------------------------------------------------------------------------

trait MyFunctions{
    def addone(x: Int): Int = x + 1

    def multiplybytwo(x: Int): Int = x * 2
}


object ActorAsFuture{

    case object Start1
    case object Start2
    case object Start3
    case class Compute1(f: Int => Int, param: Int)
    case class Compute2(f: Int => Int, param: Int)
    case class Compute3(f: Int => Int, param: Int)

    case class Done1(out: Int)
    case class Done2(out: Int)
    case class Done3(out: Int)


    def props():Props = Props(new ActorAsFuture())

}

class ActorAsFuture extends Actor with ActorLogging with MyFunctions{
    import ActorAsFuture._

    var result = 0

    def receive = {

        case Start1 =>

            context.actorOf(Temp_actor.props()) ! Compute1(addone, result)

        case Start2 =>

            context.actorOf(Temp_actor.props()) ! Compute2(multiplybytwo, result)

        case Start3 =>

            context.actorOf(Temp_actor.props()) ! Compute3(addone, result)
            
        case Done1(out) =>

            log.info(s"received output: [$out]")

            result = out
            
            self ! Start2
        
        case Done2(out) =>

            log.info(s"received output: [$out]")

            result = out
            
            self ! Start3
        
        case Done3(out) =>

            log.info(s"received output: [$out]")
            result = out
            log.info(s"result = [$result]")

    }
}

object Temp_actor{

    def props():Props = Props(new Temp_actor())

}

class Temp_actor extends Actor with ActorLogging with MyFunctions{

    
    import ActorAsFuture._

    def receive = {
        case Compute1(f, x) =>

            sender() ! Done1(f(x))
        
        case Compute2(f, x) =>
                
            sender() ! Done2(f(x))

        case Compute3(f, x) =>
                    
            sender() ! Done3(f(x))

    }
}



object main extends App{

    import ActorAsFuture._

    val system = ActorSystem("ActorAsFuture")
    val actor = system.actorOf(ActorAsFuture.props(), "ActorAsFuture")

    actor ! Start1

    // actor receive Start1
    //
    //     - actor send Compute(addone, result) to Temp_actor       // create a future (temp actor)
    //
    //     Something like:
    //          
    //          - case Start1 =>
    //              future{addone(result)}.onCompelte{
    //                  case Success(value) => result = value
    //                  case Failure(exception) => println(exception)
    //              }

    // To do Something like:

    //     - future1.map(addone).map(multiplybytwo).map(addone).map(x => Done(x)).pipeTo(self)
    //
    //     - actor receive Start1
    //         - actor creates a Temp_actor1, and send Compute(addone, result) to Temp_actor1
    //
    //     - actor receive Done1(out)
    //         - actor change its mutable state result to out
    //         - actor send Start2 to self
    //         
    //     - actor receive Start2
    //         - actor cretes a Temp_actor2, and send Compute(multiplybytwo, result) to Temp_actor2
    //
    //     - actor receive Done2(out)
    //         - actor change its mutable state result to out
    //         - actor send Start3 to self
    //
    //     - actor receive Start3
    //         - actor send Compute(addone, out) to Temp_actor3
    //
    //     - actor receive Done3(out)
    //         - Finally, actor change its mutable state result to out



    Thread.sleep(1000)

    system.terminate()
}



// ------------------------------------------------------------------------------------

import scala.concurrent.Future
import scala.util._
import akka.pattern._

object ActorMixingFuture{

    case object Start1
    case object Start2
    case object Start3
    case class Done(out: Int)

    def props():Props = Props(new ActorMixingFuture())
}

class ActorMixingFuture extends Actor with ActorLogging with MyFunctions{

    import ActorMixingFuture._
    import context.dispatcher

    var result = 0
    var intermediate: Future[Int] = null

    def receive = {

        case Start1 =>

            intermediate = Future{addone(result)}

            intermediate.onComplete{
                case Success(value) => println(s"Start1 Finished, value = [$value]")
                case Failure(exception) => println(exception)
            }

        case Start2 =>

            intermediate = intermediate.map(multiplybytwo)
            intermediate.onComplete{
                case Success(value) => println(s"Start2 Finished, value = [$value]")
                case Failure(exception) => println(exception)
            }

        case Start3 =>

            intermediate = intermediate.map(addone)
            intermediate.onComplete{
                case Success(value) => println(s"Start3 Finished, value = [$value]")
                case Failure(exception) => println(exception)
            }
            intermediate.map(x => Done(x)).pipeTo(self)
        
        case Done(out) =>
            result = out
            println(s"Done, result = [$result]")

            
        
    }
}

object main2 extends App{

    import ActorMixingFuture._

    val system = ActorSystem("ActorMixingFuture")
    val actor = system.actorOf(ActorMixingFuture.props(), "ActorMixingFuture")

    actor ! Start1
    actor ! Start2
    actor ! Start3

    Thread.sleep(1000)

    system.terminate()
}