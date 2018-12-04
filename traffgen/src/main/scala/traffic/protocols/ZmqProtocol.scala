package traffic.protocols

import pureconfig.generic.auto._
import com.typesafe.scalalogging.Logger
import config.FeEndpoint
import org.zeromq.ZMQ

trait ZmqProtocol extends Protocol {

  private val logger = Logger(classOf[ZmqProtocol])
  private val config = pureconfig.loadConfigOrThrow[FeEndpoint]("fe_endpoint")

  private val context: ZMQ.Context = ZMQ.context(1)
  private val socket: ZMQ.Socket = context.socket(ZMQ.REQ)

  logger.info("Connecting to EIR")

  socket.connect(s"${config.protocol}${config.address}:${config.port}")

  override protected def send(message: String): String = {

    // Send the message
    logger.debug(s"Sending request $message")
    socket.send(message.getBytes(), 0)

    // Get the reply.
    val reply = socket.recv(0)

    new String(s"$message=${new String(reply)}")
  }
}