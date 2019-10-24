package zio

import com.github.ghik.silencer.silent

private[zio] object UnsafelyExposedFiberRefs {
  @volatile
  private[this] var inUse: Boolean = false

  def register(@silent("never used") fiberRef: FiberRef.UnsafeHandle[_]): Unit = {
    inUse = true
    ()
  }

  def used: Boolean = inUse
}
