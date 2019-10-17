package zio.blocking

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ExecutorService, Executors}

import zio.blocking.BlockingSpecUtil._
import zio.clock.Clock
import zio.duration.Duration
import zio.internal.Executor
import zio.test.Assertion._
import zio.test._
import zio.{Managed, Schedule, UIO, ZIO, ZIOBaseSpec}
import zio.duration._

import scala.annotation.tailrec
import scala.util.Random

object BlockingSpec
  extends ZIOBaseSpec(
    suite("BlockingSpec")(
      suite("Make a Blocking Service and verify that")(defaultTests: _*),
      suite("Make a single threaded blocking service and verify that")(
        (specialTest :: defaultTests): _*
      ).provideManagedShared(BlockingSpecUtil.singleThreadBlockingService)
    )
  )

object BlockingSpecUtil {
  val defaultTests = List(
    testM("effectBlocking completes successfully") {
      assertM(effectBlocking(()), isUnit)
    },
    testM("effectBlockingCancelable completes successfully") {
      assertM(effectBlockingCancelable(())(UIO.unit), isUnit)
    },
    testM("effectBlocking can be interrupted") {
      assertM(effectBlocking(Thread.sleep(50000)).timeout(Duration.Zero), isNone)
    },
    testM("effectBlockingCancelable can be interrupted") {
      val release = new AtomicBoolean(false)
      val cancel = UIO.effectTotal(release.set(true))
      assertM(effectBlockingCancelable(blockingAtomic(release))(cancel).timeout(Duration.Zero), isNone)
    }
  )

  val specialTest = testM("test") {
    def sleepDeeply(duration: Duration) = effectBlocking {
      @tailrec
      def loop(): Unit = {
        try {
          Thread.sleep(duration.toMillis)
        } catch {
          case _: InterruptedException =>
            if (Random.nextInt(10) != 0) loop
            else Thread.interrupted()
        }
      }

      loop()
    }

    for {
      sleepWalker <- sleepDeeply(2.milli).repeat(Schedule.spaced(1.milli)).fork
      eff1 = sleepDeeply(1.millis).as(1)
      eff2 = sleepDeeply(10.millis).timeout(1.milli).as(2)
      eff3 = sleepDeeply(5.milli).repeat(Schedule.once).timeout(1.milli).as(3)
      race = ZIO.raceAll(eff1, List(eff2, eff3))
      result <- assertM(race.ensuring(sleepWalker.interrupt), isWithin(1, 3))
    } yield result
  }

  def blockingAtomic(released: AtomicBoolean): Unit =
    while (!released.get()) {
      try {
        Thread.sleep(10L)
      } catch {
        case _: InterruptedException => ()
      }
    }

  trait CustomBlockingService extends Blocking {
    outer =>
    protected val blockingExecutor: Executor

    val blocking: Blocking.Service[Any] = new Blocking.Service[Any] {
      def blockingExecutor: ZIO[Any, Nothing, Executor] = ZIO.succeed(outer.blockingExecutor)
    }
  }

  def singleThreadExecutorService: Managed[Nothing, ExecutorService] =
    UIO(Executors.newSingleThreadExecutor()).toManaged(exec => UIO(exec.shutdown()))

  def singleThreadExecutor: Managed[Nothing, Executor] =
    singleThreadExecutorService.map(Executor.fromExecutorService(Int.MaxValue))

  def singleThreadBlockingService: Managed[Nothing, Blocking with Clock] =
    singleThreadExecutor.map { executor =>
      new CustomBlockingService with Clock.Live {
        protected val blockingExecutor = executor
      }
    }
}
