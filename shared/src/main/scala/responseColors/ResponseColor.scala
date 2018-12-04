package responseColors

sealed abstract class ResponseColor

final case class White() extends ResponseColor
final case class Black() extends ResponseColor