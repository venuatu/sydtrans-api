package controllers

import controllers.Application._
import play.api.libs.json.{JsValue, Json, JsObject}
import play.api.mvc.{Controller, Action}
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.ExecutionContext.Implicits.global

object Shapes extends Controller with MongoController {
  def all(filter: String) = Action.async {
    val collection = db.collection[JSONCollection]("shapes")
    val filterQuery = if (filter.isEmpty) Json.obj() else Json.obj("_id" -> Json.obj("$regex" -> s".*-$filter.*"))

    collection.find(filterQuery, Json.obj("encoded" -> 1)).cursor[JsValue].collect[Vector]()
      .map{a => Ok(Json.toJson(a.map{obj => (obj.\("_id").as[String], obj.\("encoded").as[String])}.toMap))}
  }
}
