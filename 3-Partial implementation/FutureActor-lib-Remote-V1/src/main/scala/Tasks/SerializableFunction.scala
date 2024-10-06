package Tasks

trait SerializableFunction_1[T] extends Serializable {
  def apply(): T
}


trait SerializableFunction_2[T, U] extends Serializable {
  def apply(t: T): U
}

trait SerializableFunction_3[T] extends Serializable {
  def apply(t: Any): T
}

