package config

final case class FeEndpoint(protocol: String,
                      address: String,
                      port: Int)

final case class Ldap(host: String,
                port: Int,
                baseDn: String,
                retryConnectPeriod: Int)

final case class EirFeConfig(feEndpoint: FeEndpoint, ldap: Ldap)