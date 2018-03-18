package repository

import messages.CheckImeiMessage

trait EirRepository {

  def getResponseColor(checkImeiMessage: CheckImeiMessage): String
}
