package model

import org.json4s.DefaultFormats

package object api {
  implicit val formats: DefaultFormats.type = DefaultFormats
}

