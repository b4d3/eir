import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import shapeless.{Witness => W}

package object messages {

  type Imei = String Refined MatchesRegex[W.`"[0-9]{14,16}"`.T]
  type Imsi = String Refined MatchesRegex[W.`"[0-9]{16}"`.T]

}