// import java.util.concurrent.{ThreadPoolExecutor, TimeUnit, LinkedBlockingQueue}
// import akka.dispatch.{ExecutorServiceConfigurator, ExecutorServiceFactory, ForkJoinExecutorConfigurator}
// import com.typesafe.config.Config


// class MyExecutorServiceConfigurator(config: Config) extends ExecutorServiceConfigurator(config) {
//   class MyExecutorServiceFactory extends ExecutorServiceFactory {
//     def createExecutorService: ExecutorService = {
//       new ThreadPoolExecutor(16, 16, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue[Runnable]) {
//         override def afterExecute(r: Runnable, t: Throwable): Unit = {
//           super.afterExecute(r, t)
//           if (t != null) {
//             // Handle the exception...
//             println("Exception caught!")
//           }else{
//             println("No exception caught!")
//           }
//         }
//       }
//     }
//   }

//   def createExecutorServiceFactory(id: String, threadFactory: ThreadFactory): ExecutorServiceFactory =
//     new MyExecutorServiceFactory
// }

