package repository.repositories

import cats.effect.{Sync, Timer}
import cats.implicits._
import com.typesafe.scalalogging.Logger
import com.unboundid.ldap.sdk.{Filter, LDAPConnection, SearchScope}
import config.EirFeConfig
import faultManagement.FaultManager
import messages._
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._
import pureconfig.{CamelCase, ConfigFieldMapping}
import repository.EirRepository
import repository.alarms.RepositoryAlarms
import utils.logging.Logging

import scala.collection.JavaConverters._
import scala.concurrent.duration._


object LdapRepository {

  private implicit val logger: Logger = Logger("LdapRepository")

  private val IMEI_KEY = "imei"
  private val IMSI_KEY = "imsi"
  Logger

  def apply[F[_] : Sync : Logging](faultManager: FaultManager[F])(implicit timer: Timer[F]): F[EirRepository[F]] = {

    implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

    val config = pureconfig.loadConfigOrThrow[EirFeConfig]
    val baseDn = config.ldap.baseDn
    val retryPeriod = config.ldap.retryConnectPeriod
    val host = config.ldap.host
    val port = config.ldap.port


    def notifyError(e: Throwable): F[Unit] = {
      Logging[F].error("Error when connecting to LDAP repository: " + e.getMessage) >>
        faultManager.raiseAlarm(RepositoryAlarms.REPOSITORY_UNREACHABLE) >>
        Logging[F].info(s"Retry to connect in a $retryPeriod ms") >>
        timer.sleep(retryPeriod.milliseconds)
    }

    def retryConstruction(): F[LDAPConnection] =
      Sync[F].delay(new LDAPConnection(host, port)).handleErrorWith { e =>
        notifyError(e) >> retryConstruction()
      }

    retryConstruction().map { c =>
      new EirRepository[F] {

        override def getResponseColor(checkImeiMessage: CheckImeiMessage): F[String] = {

          for {
            connection <- acquireConnection(host, port)
            responseColors <- doSearch(connection, checkImeiMessage)
          } yield responseColors
        }

        private def acquireConnection(host: String, port: Int): F[LDAPConnection] = {

          def retryConnection(connection: LDAPConnection): F[Unit] = {

            Sync[F].delay(connection.connect(host, port)).handleErrorWith { e =>
              notifyError(e) >> retryConnection(connection)
            }
          }

          for {
            isConnected <- Sync[F].delay(c.isConnected)
            connection <- if (isConnected)
              Logging[F].info(s"LDAP Connection to $host:$port established").as(c)
            else retryConnection(c).as(c)

          } yield connection

        }

        private def doSearch(connection: LDAPConnection, checkImeiMessage: CheckImeiMessage): F[String] = {

          val searchFilter = createImeiSearchFilter(checkImeiMessage)

          for {
            maybeSearchResult <- Sync[F].delay(connection.search(baseDn, SearchScope.ONE, searchFilter)).attempt

            searchResult <- maybeSearchResult match {
              case Right(sr) => sr.getSearchEntries.asScala match {
                // TODO implement proper logic.
                // TODO Where to keep the color information? As value in LDAP database for each IMEI or
                //  have specific LDAP branch?
                case entries if entries.isEmpty => "BLACK".pure[F]
                case entries if entries.head.getAttributeValue(IMSI_KEY).startsWith("098") => "BLACK".pure[F]
                case _ => "WHITE".pure[F]
              }

              case Left(e) => for {
                _ <- Logging[F].error(s"Error when executing LDAP search on " +
                  s"$checkImeiMessage: " + e.getMessage)
                _ <- faultManager.raiseAlarm(RepositoryAlarms.REPOSITORY_UNREACHABLE)
              } yield "WHITE"
            }
          } yield searchResult
        }

        private def createImeiSearchFilter(checkImeiMessage: CheckImeiMessage): Filter = {

          checkImeiMessage match {
            case CheckImeiWithImsi(imei, imsi) => Filter.createANDFilter(Filter
              .createEqualityFilter(IMEI_KEY, imei.value), Filter.createEqualityFilter(IMSI_KEY, imsi.value))

            case CheckImei(imei) => Filter.createEqualityFilter(IMEI_KEY, imei.value)
          }
        }
      }
    }
  }
}