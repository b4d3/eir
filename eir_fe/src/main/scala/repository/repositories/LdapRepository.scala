package repository.repositories

import pureconfig.generic.auto._
import com.typesafe.scalalogging.Logger
import com.unboundid.ldap.sdk.{Filter, LDAPConnection, LDAPException, SearchScope}
import config.EirFeConfig
import messages._
import pureconfig.generic.ProductHint
import pureconfig.{CamelCase, ConfigFieldMapping}
import repository.EirRepository
import repository.alarms.RepositoryAlarms

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

trait LdapRepository extends EirRepository {

  private val logger = Logger(classOf[LdapRepository])
  implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))
  private val config = pureconfig.loadConfigOrThrow[EirFeConfig]

  private val IMEI_KEY = "imei"
  private val IMSI_KEY = "imsi"
  private val baseDn = config.ldap.baseDn
  private val retryPeriod = config.ldap.retryConnectPeriod
  private val connection = acquireConnection(config.ldap.host, config.ldap.port)

  override def getResponseColor(checkImeiMessage: CheckImeiMessage): String = {

    val searchFilter = createImeiSearchFilter(checkImeiMessage)

    try {
      val searchResult = connection.search(baseDn, SearchScope.ONE, searchFilter)
      searchResult.getSearchEntries.asScala match {
        // TODO implement proper logic.
        // TODO Where to keep the color information? As value in LDAP database for each IMEI or
        // TODO have specific LDAP branch?
        case entries if entries.isEmpty => "BLACK"
        case entries if entries.head.getAttributeValue(IMSI_KEY).startsWith("098") => "BLACK"
        case _ => "WHITE"
      }
    } catch {
      case e: LDAPException =>
        logger.error(s"Error when executing LDAP search on $checkImeiMessage")
        faultManager.raiseAlarm(RepositoryAlarms.REPOSITORY_UNREACHABLE)
        "WHITE"
    }
  }


  private def createImeiSearchFilter(checkImeiMessage: CheckImeiMessage): Filter = {

    checkImeiMessage match {

      case CheckImeiWithImsi(imei, imsi) => Filter.createANDFilter(Filter
        .createEqualityFilter(IMEI_KEY, imei.value), Filter.createEqualityFilter(IMSI_KEY, imsi.value))

      case CheckImei(imei) => Filter.createEqualityFilter(IMEI_KEY, imei.value)
    }
  }


  //TODO Handle reconnection in Connection pool (good even for a single connection because it
  //TODO handles connection management
  private def acquireConnection(host: String, port: Int): LDAPConnection =

    hostAvailabilityCheck(host, port) match {
      case Success(ldapConnection) =>
        logger.info(s"LDAP Connection to $host:$port established")
        ldapConnection
      case Failure(f) =>
        logger.error(s"Error when connecting to LDAP repository: ", f)
        faultManager.raiseAlarm(RepositoryAlarms.REPOSITORY_UNREACHABLE)

        logger.info(s"Retry to connect in a $retryPeriod ms")
        Thread.sleep(retryPeriod)
        acquireConnection(host, port)
    }

  def hostAvailabilityCheck(host: String, port: Int): Try[LDAPConnection] = {
    Try(new LDAPConnection(host, port))
  }
}
