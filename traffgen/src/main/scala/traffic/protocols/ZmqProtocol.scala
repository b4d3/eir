package traffic.protocols

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.zeromq.ZMQ

trait ZmqProtocol extends Protocol {

  private val logger = Logger(classOf[ZmqProtocol])
  private val config = ConfigFactory.load()

  private val context: ZMQ.Context = ZMQ.context(1)
  private val socket: ZMQ.Socket = context.socket(ZMQ.REQ)

  logger.info("Connecting to EIR")

  {
    val protocol = config.getString("fe_endpoint.protocol")
    val address = config.getString("fe_endpoint.address")
    val port = config.getString("fe_endpoint.port")
    socket.connect(s"$protocol$address:$port")
  }

  override protected def send(message: String): String = {

    // Send the message
    logger.debug(s"Sending request $message")
    socket.send(message.getBytes(), 0)

    // Get the reply.
    val reply = socket.recv(0)

    new String(s"$message=${new String(reply)}")
  }
}