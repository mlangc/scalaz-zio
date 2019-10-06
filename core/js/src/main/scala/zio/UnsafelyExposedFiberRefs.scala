package zio

import java.util.{ Map => JMap }

import com.github.ghik.silencer.silent

private[zio] object UnsafelyExposedFiberRefs {
  @volatile
  private[this] var inUse: Boolean = false

  def register(@silent("never used") fiberRef: FiberRef[_]): Unit = {
    inUse = true
    ()
  }

  def foreach(fiberRefs: JMap[FiberRef[_], Any])(f: (FiberRef[_], Any) => Unit): Unit =
    if (!inUse || fiberRefs.isEmpty) ()
    else {
      val iter = fiberRefs.entrySet().iterator()
      while (iter.hasNext) {
        val entry    = iter.next()
        val fiberRef = entry.getKey

        if (fiberRef.maybeThreadLocal.isDefined) {
          val value = entry.getValue
          f(fiberRef, value)
        }
      }
    }
}
