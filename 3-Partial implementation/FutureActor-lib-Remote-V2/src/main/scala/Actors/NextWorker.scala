package Actors

import akka.actor._
import Tasks._

import java.io.Serial
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream


// Define the message for the actor: NextWorker
object NextWorker{
    case object GetPR                                
    case class Error_Handling_2[U](func: Array[Byte])   


    def props[T, U](creator: ActorRef, prev_wa: ActorRef, op_byte: Array[Byte]): Props = 
        Props(new NextWorker(creator, prev_wa, op_byte))

}



class NextWorker[T, U](creator: ActorRef, prev_wa: ActorRef, op_byte: Array[Byte]) 
extends Actor with Stash with ActorLogging {

    import FirstWorker._
    import NextWorker._


    val byteStream = new ByteArrayInputStream(op_byte)
    val objectStream = new ObjectInputStream(byteStream)
    val op = objectStream.readObject().asInstanceOf[SerializableFunction_2[T,U]]


    val recipient = creator
    val prev_worker = prev_wa

    override def preStart(): Unit = {
        // Get the previous result once the actor is created
        self ! GetPR
    }

    /*
    / The initial behavior of the actor
    / The actor will get the previous result, do the execution, and become the done state
    / If the task fails, the actor will become the error_handling state
    */
    def init: Receive = {
        case GetPR =>
            prev_worker ! NextActor(self)
        case NextActor(actor) =>
            stash()
        case AskResult(actor) => 
            stash()
        case Error_Handling_2(func) =>
            stash()
        case Default_Result(r) =>
            stash()
        case RequiredValue(value) => 
            val pre_value = value.asInstanceOf[T]
            try {
                val result = op(pre_value)
                context.become(done(result))
                unstashAll()
            } catch{
                case e: Exception =>
                    val e_msg = e.getMessage
                    log.info(s"NextWorker Actor [$self] caught an Error: [$e_msg]")
                    recipient ! e_msg
                    context.become(error_handling(e_msg, pre_value))
            }
            val result = op(pre_value)
            context.become(done(result))
            unstashAll()
    }

    /*
    / The error_handling behavior of the actor
    / The actor will try to recover from the error by calling the function func
    / If the task fails again, the actor will become the error_handling state again
    / If the task succeeds, the actor will become the done state
    / @param e: String => the error message
    / @param pre_value: T => the previous result
    */
    def error_handling(e: String, pre_value: T): Receive = {
        case NextActor(actor) => 
            stash()
        case Error_Handling_2(func) =>
            try{
                val e_byteStream = new ByteArrayInputStream(func)
                val e_objectStream = new ObjectInputStream(e_byteStream)
                val e_op = e_objectStream.readObject().asInstanceOf[SerializableFunction_3[U]]
                val result = e_op(pre_value).asInstanceOf[U]
                context.become(done(result))
                unstashAll()
            } catch{
                case e: Exception =>
                    val e_msg = e.getMessage
                    log.info(s"NextWorker Actor [$self] caught an Error: [$e_msg] in error_handling")
                    recipient ! e_msg
                    context.become(error_handling(e_msg, pre_value))
            }
        case Default_Result(r) =>
            context.become(done(r.asInstanceOf[U]))
            unstashAll()
    }

    /*
    / The done behavior of the actor
    / The actor will send the result to the next actor
    / @param result: U => the result of the task
    */
    def done(result: U): Receive = {
        case NextActor(actor) =>
            actor ! RequiredValue(result)
        case AskResult(actor) => 
            actor ! result
            log.info(s"NextWorker Actor [$self] send [$result] to [$actor]")
    }

    def receive = init
}
