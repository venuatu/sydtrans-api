package controllers

import org.joda.time.{DateTimeZone, DateTime}
import play.api.libs.json.Json
import play.api.mvc.{Result, Controller}
import play.modules.reactivemongo.MongoController

import scala.concurrent.duration.Duration

class BaseController extends Controller with MongoController {

  implicit class CacheResponse(resp: Result) {
    def cacheFor(time: Duration) = {
      resp.withHeaders(
        "Cache-Control" -> s"max-age=${time.toSeconds}",
        "Expires" -> (new DateTime().plusSeconds(time.toSeconds.toInt)
          .withZone(DateTimeZone.UTC).toString("EEE, dd MMM yyyy HH:mm:ss") + " GMT")
      )
    }
  }

  def parseBounds(bounds: String): Vector[Vector[Double]] = {
    try {
      val arr = bounds.split(',').map{_.toDouble}.grouped(2).toVector.map{_.toVector}

      if (arr.length == 2 && arr(1).length != 2) {
        null
      } else {
        arr
      }
    } catch {
      case e: Exception => println(e.toString); null
    }
  }

  def boundsError() = {
    Status(400)(Json.obj("error" ->
      Json.obj("bounds" -> "should be in the form of: 150.86827,-34.413601,150.892989,-34.402642")))
  }
}
