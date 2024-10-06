object TestingThing extends App{
    
  val thread = new Thread {
    override def run(): Unit = {
        def fib(n: Int): Int = n match {
            case 0 | 1 => 
              println(n)
              n
            case _ => fib(n - 1) + fib(n - 2)
        }
        println(s"Final result for fib(30) is ${fib(30)}")
    }
  }

  thread.start()
  Thread.sleep(3000)
  thread.suspend()
  Thread.sleep(5000)
  thread.resume()

}