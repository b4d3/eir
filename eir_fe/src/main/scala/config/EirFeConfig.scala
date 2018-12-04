package config

import pureconfig.generic.auto._

case class FeEndpoint(protocol: String,
                       address: String,
                       port: Int)

case class Ldap(host: String,
                port: Int,
                baseDn: String,
                retryConnectPeriod: Int)

case class EirFeConfig(feEndpoint: FeEndpoint, ldap: Ldap)