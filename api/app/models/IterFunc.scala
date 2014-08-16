package models

class IterFunc[T](getter: () => Option[T]) extends Iterator[T] {

  private var item: Option[T] = null

  override def hasNext: Boolean = {
    if (item == null) {
      item = getter()
    }
    item.isDefined
  }

  override def next(): T = {
    if (item == null) {
      item = getter()
    }
    val ret = item.get
    item = null
    ret
  }
}
