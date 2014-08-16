package controllers

import controllers.Application._
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.libs.json.{JsValue, Json, JsObject}
import play.api.mvc.{Controller, Action}
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.ExecutionContext.Implicits.global

object Stops extends BaseController {

  def bounded(boundsStr: String) = Action.async {
    val bounds = parseBounds(boundsStr)

    if (bounds == null) {
      Future{
        Status(400)(Json.obj("error" ->
          Json.obj("bounds" -> "should be in the form of: 150.86827,-34.413601,150.892989,-34.402642")
      ))}
    } else {
      db.collection[JSONCollection]("stops")
        .find(Json.obj("coord" -> Json.obj("$geoWithin" -> Json.obj("$box" -> bounds))))
        .cursor[JsValue].collect[Vector](1000)
        .map { a => Ok(Json.toJson(a)).as("application/json").cacheFor(1.minute)}
    }
  }
}
