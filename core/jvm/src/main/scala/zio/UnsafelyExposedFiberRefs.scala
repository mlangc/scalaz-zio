package zio

import java.lang.{ Boolean => JBoolean }
import java.util.{ Collections => JCollections, Set => JSet, WeakHashMap => JWeakHashMap }

private[zio] object UnsafelyExposedFiberRefs {
  private[this] val handles: JSet[FiberRef.UnsafeHandle[_]] =
    JCollections.synchronizedSet {
      JCollections.newSetFromMap {
        new JWeakHashMap[FiberRef.UnsafeHandle[_], JBoolean]()
      }
    }

  def register(handle: FiberRef.UnsafeHandle[_]): Unit = {
    handles.add(handle)
    ()
  }

  def used: Boolean = !handles.isEmpty
}
