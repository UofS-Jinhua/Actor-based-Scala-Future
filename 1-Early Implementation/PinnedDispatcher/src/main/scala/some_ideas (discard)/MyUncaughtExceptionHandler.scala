// class MyUncaughtExceptionHandler extends Thread.UncaughtExceptionHandler {
//   def uncaughtException(thread: Thread, throwable: Throwable): Unit = {
//     println(s"Uncaught exception in thread ${thread.getName}")
//     throwable match {
//       case td: ThreadDeath =>
//         println("Caught ThreadDeath. Do nothing.")
//         // ...
//       case ex: Exception =>
//         println("Caught an exception.")
//         // ...
//       case _=>
//         println("Caught something else.")
//         // ... 
//     }
//   }
// }
