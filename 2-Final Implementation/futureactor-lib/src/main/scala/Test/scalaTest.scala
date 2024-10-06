package Test

object scala_test_main extends App{
    val a: List[Int] = List(1,2,3,4,5,6)
    val b: Map[Int, Any] = (1 to a.length).map(i => (i, None) ).toMap
    println(b)
}