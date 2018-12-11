package repository

import faultManagement.FaultManagerImpl
import messages.CheckImeiMessage

trait EirRepository {

  def getResponseColor(checkImeiMessage: CheckImeiMessage): String
  val faultManager: FaultManagerImpl
}
