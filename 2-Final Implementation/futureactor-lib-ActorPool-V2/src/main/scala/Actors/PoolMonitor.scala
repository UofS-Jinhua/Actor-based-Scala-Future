package Actors

import akka.actor._
import concurrent.duration._

import Tasks._


object PoolMonitor {

    // Messages
    // - CheckPoolSize: signal to check the pool size
    case object CheckPoolSize

    // Props:
    // - poolsize: the size of the pool
    // - min_num: the minimum number of workers required in the pool
    def props(poolsize: Int, min_num: Int) = Props(new PoolMonitor(poolsize, min_num))
}


class PoolMonitor(Poolsize: Int, Min_num: Int) extends Actor with ActorLogging{
  import PoolMonitor._

  // LocalTask API: to interact with the actor pool for LocalTask
  final var LT_api: LocalTask[_] = _


  // After the actor is started,
  // - create a new LocalTask API
  // - send a message to check the pool size
  override def preStart(): Unit = {
    log.info("PoolMonitor started")
    LT_api = new LocalTask(self)

    // Schedule a message to check the pool size every 0.1 seconds
    context.system.scheduler.scheduleAtFixedRate(0.milli, 1.milli, self, CheckPoolSize)(context.system.dispatcher, self)

  }

  def receive: Receive = {

    // Check the pool size
    case CheckPoolSize =>

        LT_api.getPoolSize match {

            // If the pool size is less than the minimum number of workers required,
            // - create more workers
            case size if size < Min_num =>
                log.info(s"PoolMonitor: Min Worker = [$Min_num], Current Pool size = [$size], creating more workers")
                LT_api.createWorker(Min_num)

            case size =>
                // If the pool size is greater than the minimum number of workers required, do nothing

                // log.info(s"Pool size is $size, no need to create new task")
        }

  }
}

