package controllers

import controllers.Application._
import controllers.Stops._
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.libs.json.{JsValue, Json, JsObject}
import play.api.mvc.{Controller, Action}
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.ExecutionContext.Implicits.global

object Shapes extends BaseController {
  def all(filter: String, boundsStr: String) = Action.async {
    val collection = db.collection[JSONCollection]("shapes")
    val filterQuery = if (filter.isEmpty) Json.obj() else Json.obj("id_bits" -> filter)
    val bounds = if (boundsStr.isEmpty) Json.obj() else Json.obj("path" -> Json.obj("$geoWithin" ->
      Json.obj("$box" -> parseBounds(boundsStr))))

    if (bounds == null) {
      Future { boundsError() }
    } else {
      collection.find(filterQuery ++ bounds, Json.obj("encoded" -> 1)).sort(Json.obj("encoded_length" -> 1))
        .cursor[JsValue].collect[Vector](2000)
        .map{a =>
          Ok(Json.toJson(uniqueValues(a.map{obj =>
            (obj.\("_id").as[String], obj.\("encoded").as[String]}))
          ).cacheFor(1.day).as("application/json")}
    }
  }

  def uniqueValues(vec: Vector[(String, String)]) = {
    val set = new mutable.TreeSet[(String, String)]()(Ordering.by[(String, String), String](_._2))
    set ++= vec
    set.toVector
  }
}
