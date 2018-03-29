package repository

import faultManagement.FaultManager
import messages.CheckImeiMessage

trait EirRepository {

  def getResponseColor(checkImeiMessage: CheckImeiMessage): String
  val faultManager: FaultManager
}
