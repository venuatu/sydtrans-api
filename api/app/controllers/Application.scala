package controllers

import java.io.File

import models.Polyline.Coord
import models.{Polyline, Upstream}
import models.Upstream._
import play.api.libs.iteratee.{Execution, Iteratee}
import play.api._
import play.api.libs.json._
import play.api.mvc._
import com.bizo.mighty.csv.{CSVReader, CSVDictReader}
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.collection.mutable.ArrayBuffer

import scala.concurrent.Promise

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
    val collection = db.collection[JSONCollection]("shapes")
    def flush(force: Boolean = false): Unit = {
      if (force || buffer.length > 100) {
        for (shape <- buffer) {
          collection.save(
            Json.obj(
              "_id" -> shape.id,
              "id" -> shape.id,
              "encoded" -> shape.enc,
              "path" -> Json.toJson(shape.path)
            )
          )
        }
        buffer = ArrayBuffer[ShapeFormat]()
      }
    }

    Upstream.parseShapes("../shapes.txt").onDoneEnumerating(prom.success(1)).run(Iteratee.foreach({
      case (id, path) =>
        items += 1
        buffer += ShapeFormat(id, Polyline.encode(path.map{bit => Coord(bit.lat.toFloat, bit.lon.toFloat)}), path)
        flush()
    }))
    prom.future.map{res =>
      flush(force = true)
      Ok(Json.toJson(buffer.toVector))
    }
  }
}