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
  private[this] val refs: JSet[FiberRef[Any]] =
    JCollections.synchronizedSet {
      JCollections.newSetFromMap {
        new JWeakHashMap[FiberRef[Any], JBoolean]()
      }
    }

  def register(fiberRef: FiberRef[Any]): Unit = {
    refs.add(fiberRef)
    ()
  }

  def foreach(fiberRefs: JMap[FiberRef[Any], Any])(f: (FiberRef[Any], Any) => Unit): Unit =
    if (!refs.isEmpty && !fiberRefs.isEmpty) {
      val refsOfInterest = new JHashSet[FiberRef[Any]](refs)
      refsOfInterest.retainAll(fiberRefs.keySet())

      val iter = refsOfInterest.iterator()
      while (iter.hasNext) {
        val fiberRef = iter.next()
        val value    = fiberRefs.get(fiberRef)
        f(fiberRef, value)
      }
    }
}
