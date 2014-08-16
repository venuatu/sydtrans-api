package controllers

import java.io.File

import models.Types.Coord
import models.{PointEliminator, Polyline, Upstream}
import models.Upstream._
import play.api.libs.iteratee.{Execution, Iteratee}
import play.api._
import play.api.libs.json._
import play.api.mvc._
import com.bizo.mighty.csv.{CSVReader, CSVDictReader}
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONArray, BSONDocument}
import scala.collection.mutable.ArrayBuffer

import scala.concurrent.{Future, Promise}

object Application extends Controller with MongoController {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def loadAllTheThings() = {
    val agency = Upstream.fromCsv[Agency]("agency.txt", "agency")
    val stops = Upstream.fromCsv[Stop]("stops.txt", "stop")
    val calendar = Upstream.fromCsv[Stop]("calendar.txt", "calendar")
    val calendarDates = Upstream.fromCsv[CalendarDates]("calendar_dates.txt", "")
  }


  case class ShapeFormat(id: String, enc: String, path: Seq[ShapeBit])
  implicit val outFmt = Json.format[ShapeFormat]

  def fromcsv = Action.async {
    implicit val ec = Execution.trampoline
    var items = 0
    val prom = Promise[Int]()
    var buffer = ArrayBuffer[ShapeFormat]()

    //val agency = Upstream.fromCsv[Agency]("agency.txt", "agency").foreach(agency => {
/*    val stops = db.collection[BSONCollection]("stops")
    Upstream.fromCsv[Stop]("../stops.txt", "stop").foreach(stop => {
      stops.save(BSONDocument(
        "_id" -> stop.stop_id,
        "stop_id" -> stop.stop_id,
        "code" -> stop.code,
        "name" -> stop.name,
        "lat" -> stop.lat,
        "lon" -> stop.lon,
        "coord" -> Vector(stop.lon, stop.lat),
        "location_type" -> stop.location_type,
        "parent_station" -> stop.parent_station,
        "wheelchair_boarding" -> stop.wheelchair_boarding,
        "platform_code" -> stop.platform_code
      ))
    })
    //val calendar = Upstream.fromCsv[Stop]("calendar.txt", "calendar")
*/
    Upstream.parseShapes("../shapes.txt").foreach{
      case (id, path) =>
        val simplePath = PointEliminator(path.map{bit => Coord(bit.lat.toFloat, bit.lon.toFloat)}, 1e-10)
        val encoded = Polyline.encode(simplePath)
        db.collection[JSONCollection]("shapes").save(
          Json.obj(
            "_id" -> id,
            "id" -> id,
            "id_bits" -> id.split('-').flatMap{a => 1 to a.length map{a.substring(0, _)}}.toSet[String],
            "encoded" -> encoded,
            "encoded_length" -> encoded.length,
            "path" -> simplePath.map { bit => Vector(bit.lng, bit.lat)},
            "path_distances" -> path.map { bit => bit.totalDistance}
          )
        )
    }
    Future {
      println(items)
      Ok(Json.toJson(buffer.toVector))
    }
  }
}
