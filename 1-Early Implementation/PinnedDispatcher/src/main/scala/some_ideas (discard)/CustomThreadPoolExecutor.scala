// import java.util.concurrent.{ThreadPoolExecutor, ThreadFactory, LinkedBlockingQueue, TimeUnit}
// import java.util.concurrent.atomic.AtomicInteger

// class CustomThreadPoolExecutor extends ThreadPoolExecutor(
//   10, // corePoolSize
//   50, // maximumPoolSize
//   60L, // keepAliveTime
//   TimeUnit.SECONDS, // unit for keepAliveTime
//   new LinkedBlockingQueueRunnable, // workQueue
//   new ThreadFactory { // custom ThreadFactory
//     private val counter = new AtomicInteger(0)
//     def newThread(r: Runnable) = {
//       val t = new Thread(r)
//       t.setName("custom-thread-" + counter.getAndIncrement())
//       t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
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
//       t
//     }
//   }
// )