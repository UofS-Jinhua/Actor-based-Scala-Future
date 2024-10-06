package Actors

import akka.actor._
import Tasks._


import java.io.Serial
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream


// import scala.reflect.runtime.universe
// import scala.tools.reflect.ToolBox


// Define the message for the actor: FirstWorker
object FirstWorker{

    case object Start

    // Message for the next actor
    case class AskResult(actor: ActorRef)
    case class NextActor(actor: ActorRef)
    case class RequiredValue[T](value: T)

    // Message for error handling

    // case class Error_Handling_1[T](func: SerializableFunction_1[T])
    case class Error_Handling_1[T](func: Array[Byte])
    case class Default_Result[T](r: T)


    // def props[T](creator: ActorRef, op: SerializableFunction_1[T]): Props = Props(new FirstWorker(creator, op))
    def props[T](creator: ActorRef, op: Array[Byte]): Props = Props(new FirstWorker(creator, op))

}

// class FirstWorker[T](creator: ActorRef, op: SerializableFunction_1[T]) extends Actor with Stash with ActorLogging {
class FirstWorker[T](creator: ActorRef, op_byte: Array[Byte]) extends Actor with Stash with ActorLogging {



    import FirstWorker._

    val byteStream = new ByteArrayInputStream(op_byte)
    val objectStream = new ObjectInputStream(byteStream)
    val op = objectStream.readObject().asInstanceOf[SerializableFunction_1[T]]
    // val tb = universe.runtimeMirror(getClass.getClassLoader).mkToolBox()





    override def preStart(): Unit = {
        // Start the task once the actor is created
        self ! Start
    }


    def init: Receive = {
        case Start =>
            try{

                val result = op().asInstanceOf[T]
                context.become(done(result))
                unstashAll()
            } catch{
                case e: Exception =>
                    val e_msg = e.getMessage
                    log.info(s"FirstWorker Actor [$self] caught an Error: [$e_msg]")
                    creator ! e_msg
                    context.become(error_handling(e_msg))
                    unstashAll()
            }

        case NextActor(actor) =>
            stash()
        case AskResult(actor) =>
            stash()
        case Error_Handling_1(func) => 
            stash()
        case Default_Result(r) =>
            stash()
        case _ =>
            // ignore other messages
    }


    def error_handling(e: String): Receive = {
        case Error_Handling_1(func) =>
            try{

                val e_byteStream = new ByteArrayInputStream(op_byte)
                val e_objectStream = new ObjectInputStream(e_byteStream)
                val e_op = e_objectStream.readObject().asInstanceOf[SerializableFunction_1[T]]

                val result = e_op().asInstanceOf[T]
                context.become(done(result))
                unstashAll()
            } catch{
                case e: Exception =>
                    val e_msg = e.getMessage
                    log.info(s"FirstWorker Actor [$self] caught an Error: [$e_msg] in error_handling")
                    creator ! e_msg
                    context.become(error_handling(e_msg))
            }
        case Default_Result(r) =>
            context.become(done(r.asInstanceOf[T]))
            unstashAll()

        case NextActor(actor) =>
            stash()
        case AskResult(actor) =>
            stash()
        case _ =>
            // ignore other messages

    }

    /*
    / The done behavior of the actor    
    / The actor will send the result to the next actor
    / @param result: T => the result of the task
    */
    def done(result: T): Receive = {
        case NextActor(actor) =>
            actor ! RequiredValue(result)
        case AskResult(actor) => 
            log.info(s"FirstWorker Actor [$self] send [$result] to [$actor]")
            actor ! result
    }

    def receive = init
}

