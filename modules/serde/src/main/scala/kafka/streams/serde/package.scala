package kafka.streams

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url

package object serde {

  final type UrlString = String Refined Url
}
