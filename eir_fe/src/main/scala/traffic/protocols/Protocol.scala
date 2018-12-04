package traffic.protocols

import responseColors.ResponseColor

trait Protocol {

  def receiveMessage(): (String, String)

  def sendMessage(address: String, responseColor: ResponseColor)
}
