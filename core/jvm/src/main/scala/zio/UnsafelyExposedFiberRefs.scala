package zio

import java.lang.{ Boolean => JBoolean }
import java.util.{
  Collections => JCollections,
  HashSet => JHashSet,
  Map => JMap,
  Set => JSet,
  WeakHashMap => JWeakHashMap
}

private[zio] object UnsafelyExposedFiberRefs {
  private[this] val refs: JSet[FiberRef[_]] =
    JCollections.synchronizedSet {
      JCollections.newSetFromMap {
        new JWeakHashMap[FiberRef[_], JBoolean]()
      }
    }

  def register(fiberRef: FiberRef[_]): Unit = {
    refs.add(fiberRef)
    ()
  }

  def foreach(fiberRefs: JMap[FiberRef[_], Any])(f: (FiberRef[_], Any) => Unit): Unit =
    if (!refs.isEmpty && !fiberRefs.isEmpty) {
      val refsOfInterest = new JHashSet[FiberRef[_]](refs)
      refsOfInterest.retainAll(fiberRefs.keySet())

      val iter = refsOfInterest.iterator()
      while (iter.hasNext) {
        val fiberRef = iter.next()
        val value    = fiberRefs.get(fiberRef)
        f(fiberRef, value)
      }
    }
}
