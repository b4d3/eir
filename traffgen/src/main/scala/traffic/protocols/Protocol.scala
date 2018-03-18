package traffgen.protocols

trait Protocol {

  protected def send(message: String): String
}
