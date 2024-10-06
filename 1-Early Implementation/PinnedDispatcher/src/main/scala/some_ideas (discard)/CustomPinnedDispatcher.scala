// import akka.dispatch._
// import akka.actor._
// import java.util.concurrent.ThreadFactory
// import akka.dispatch.ThreadPoolConfig
// import scala.concurrent.duration.FiniteDuration

// class CustomPinnedDispatcher(
//   _configurator: MessageDispatcherConfigurator,
//   _actor: ActorCell,
//   _id: String,
//   _shutdownTimeout: FiniteDuration,
//   _threadPoolConfig: ThreadPoolConfig)
// extends PinnedDispatcher(_configurator, _actor, _id, _shutdownTimeout, _threadPoolConfig) {

//   override protected def createThreadFactory: ThreadFactory = new ThreadFactory {
//     def newThread(runnable: Runnable): Thread = {
//       val thread = new Thread(runnable)
//       thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
//         def uncaughtException(thread: Thread, cause: Throwable): Unit = {
//           cause match {
//             case td: ThreadDeath =>
//               // handle ThreadDeath here
//               throw td // rethrow to allow the ThreadDeath to propagate
//             case _ =>
//               // handle other exceptions here
//           }
//         }
//       })
//       thread
//     }
//   }
// }



