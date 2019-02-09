package responseColors

sealed abstract class ResponseColor

final case class White() extends ResponseColor {
  override def toString: String = "WHITE"
}

final case class Black() extends ResponseColor {
  override def toString: String = "BLACK"
}