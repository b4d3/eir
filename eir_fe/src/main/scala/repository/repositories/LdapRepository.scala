package repository.repositories

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import com.unboundid.ldap.sdk.{Filter, LDAPConnection, LDAPException, SearchScope}
import messages._
import repository.EirRepository

import scala.collection.JavaConverters._


trait LdapRepository extends EirRepository {

  private val logger = Logger(classOf[LdapRepository])

  private val config = ConfigFactory.load

  private val IMEI_KEY = "imei"
  private val IMSI_KEY = "imsi"
  private val baseDn = config.getString("baseDn")

  private val connection = new LDAPConnection(config.getString("ldap.host"), config.getInt("ldap" +
    ".port"))


  override def getResponseColor(checkImeiMessage: CheckImeiMessage): String = {

    val searchFilter = createImeiSearchFilter(checkImeiMessage)

    try {
      val searchResult = connection.search(baseDn, SearchScope.ONE, searchFilter)
      searchResult.getSearchEntries.asScala match {
        // TODO implement proper logic.
        // TODO Where to keep the color information? As value in LDAP database have specific
        // TODO LDAP branch?
        case entries if entries.isEmpty => "BLACK"
        case entries if entries.head.getAttributeValue(IMSI_KEY).startsWith("098") => "BLACK"
        case _ => "WHITE"
      }
    } catch {
      case e: LDAPException =>
        logger.error(s"Error when executing LDAP search on $checkImeiMessage")
        "WHITE"
    }
  }

  private def createImeiSearchFilter(checkImeiMessage: CheckImeiMessage): Filter = {

    checkImeiMessage match {

      case CheckImeiWithImsi(Imei(imei), Imsi(imsi)) => Filter.createANDFilter(Filter
        .createEqualityFilter(IMEI_KEY, imei), Filter.createEqualityFilter(IMSI_KEY, imsi))

      case CheckImei(Imei(imei)) => Filter.createEqualityFilter(IMEI_KEY, imei)
    }
  }
}
