package config

case class FeEndpoint(protocol: String,
                       address: String,
                       port: Int)

case class Ldap(host: String,
                port: Int,
                baseDn: String,
                retryConnectPeriod: Int)

case class EirFeConfig(feEndpoint: FeEndpoint, ldap: Ldap)