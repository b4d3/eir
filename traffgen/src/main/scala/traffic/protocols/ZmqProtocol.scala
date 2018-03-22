package traffgen.protocols

import com.typesafe.scalalogging.Logger
import org.zeromq.ZMQ

trait ZmqProtocol extends Protocol {

  val logger = Logger(classOf[ZmqProtocol])

  val context: ZMQ.Context = ZMQ.context(1)
  val socket: ZMQ.Socket = context.socket(ZMQ.REQ)

  logger.info("Connecting to EIR")
  socket.connect("tcp://localhost:5555")

  override protected def send(message: String): String = {

    //  Ensure that the last byte of message is 0 because EIR server is expecting a
    // 0-terminated string
    val request = message.getBytes()

    // Send the message
    logger.debug(s"Sending request $request")
    socket.send(request, 0)

    // Get the reply.
    val reply = socket.recv(0)

    new String(s"$message=${new String(reply)}")
  }
}