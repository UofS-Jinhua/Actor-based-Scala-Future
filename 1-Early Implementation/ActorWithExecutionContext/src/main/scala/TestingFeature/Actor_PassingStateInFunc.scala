package TestingFeature

import akka.actor._

object Actor1 {
    case object Start
    case object Increment
    case class MyStateChange(f: () => Unit) 

    def props(): Props = Props(new Actor1())
}

class Actor1 extends Actor with ActorLogging {
    import Actor1._

    var my_state = 0

    def receive = {
        case Start =>
            log.info(s"received Start")
            context.actorOf(Actor2.props()) ! MyStateChange(() => println(my_state += 100))
        case Increment =>
            log.info(s"received Increment")
            my_state += 1
    }

    def dosth: Unit = {
        println(my_state += 100)
    }
}

object Actor2 {

    def props(): Props = Props(new Actor2())
}

class Actor2 extends Actor with ActorLogging {
    import Actor1._

    def receive = {
        case MyStateChange(f) =>
            log.info(s"received MyStateChange")
            // Thread.sleep(1000)
            f()
    }
}

object Actor_PassingStateInFunc extends App {

    import Actor1._

    val system = ActorSystem("Actor_PassingStateInFunc")

    val actor1 = system.actorOf(Actor1.props(), "actor1")

    actor1 ! Start
    actor1 ! Increment
}