package models

import com.bizo.mighty.csv.CSVReader
import org.joda.time.LocalDate
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import scala.collection.mutable.{Map => MutMap, ArrayBuffer}
import scala.concurrent.{ExecutionContext, Future, Promise}

object Upstream {
  /**
   * agency: agency_id,agency_name,agency_url,agency_timezone,agency_lang,agency_phone
   *
   * calendar: service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date
   * calendar_dates: service_id,date,exception_type
   *
   * routes: route_id,agency_id,route_short_name,route_long_name,route_desc,route_type,route_color,route_text_color
   *
   * shapes: shape_id,shape_pt_lat,shape_pt_lon,shape_pt_sequence,shape_dist_traveled
   *
   * stop_times: trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type,drop_off_type,shape_dist_traveled
   *
   * stops: stop_id,stop_code,stop_name,stop_lat,stop_lon,location_type,parent_station,wheelchair_boarding,platform_code
   *
   * trips: route_id,service_id,trip_id,shape_id,trip_headsign,direction_id,block_id,wheelchair_accessible
   */

  case class Agency(agency_id: String, name: String, url: String, timezone: String, lang: String, phone: String)

  case class Stop(stop_id: String, code: Option[String], name: String, lat: Double, lon: Double, location_type: Option[String], parent_station: Option[String], wheelchair_boarding: Boolean, platform_code: Option[String])

  case class Calendar(service_id: String, monday: Boolean, tuesday: Boolean, wednesday: Boolean, thursday: Boolean, friday: Boolean, saturday: Boolean, sunday: Boolean, start_date: LocalDate, end_date: LocalDate)
  case class CalendarDates(service_id: String, date: LocalDate, exception_type: String)

  case class Routes(route_id: String, agency_id: String, short_name: Option[String], long_name: String, desc: String, color: String, text_color: String)

  case class ShapeBit(lat: Double, lon: Double, totalDistance: Double)
  case class Shape(shape_id: String, path: Seq[ShapeBit])

  implicit val agencyFormat = Json.format[Agency]
  implicit val calendarFormat = Json.format[Calendar]
  implicit val calendarDatesFormat = Json.format[CalendarDates]
  implicit val routesFormat = Json.format[Routes]
  implicit val shapeBitFormat = new Format[ShapeBit] {
    override def reads(json: JsValue): JsResult[ShapeBit] = {
      val arr = json.as[Seq[Double]]
      JsSuccess(ShapeBit(arr(1), arr(0), arr(2)))
    }
    override def writes(o: ShapeBit): JsValue = JsArray(Seq(o.lon, o.lat, o.totalDistance).map{JsNumber(_)})
  }
  implicit val shapeFormat = Json.format[Shape]
  implicit val stopFormat = Json.format[Stop]

  def getReader(filename: String, entity: String): Iterator[Map[String, String]] = {
    val reader = CSVReader(filename)
    val headings = reader.next().toVector.map{dropUnicode}.map{col =>
      if (col.startsWith(entity +"_") && col != s"${entity}_id") col.replace(entity +"_", "")
      else col
    }
    reader.map{headings.zip(_)}.map{_.filter{!_._2.isEmpty}.toMap}
  }

  def parseShapes(filename: String)(implicit ec: ExecutionContext) = {
    val reader = getReader(filename, "shape")
    var id = ""
    var path = ArrayBuffer[ShapeBit]()
    Enumerator.fromCallback1[(String, Seq[ShapeBit])] {bool => Future {
      var result: Option[(String, Seq[ShapeBit])] = None
      do {
        val item = reader.next()
        if (id != item("shape_id")) {
          if (!id.isEmpty)
            result = Some((id, path.toVector.sortBy(_.totalDistance)))
          id = item("shape_id")
          path = ArrayBuffer[ShapeBit]()
        }
        path += ShapeBit(item("pt_lat").toDouble, item("pt_lon").toDouble, item("dist_traveled").toDouble)
      } while (reader.hasNext && !result.isDefined)
      result
    }}
  }

  def fromCsv[T: Reads](filename: String, entity: String): Seq[T] = {
    val reader = getReader(filename, entity)
    val items = reader.map{_.toSeq.map{convertColumns}}.map(JsObject).map(_.as[T]).toVector

    items
  }

  val FLOAT_COLUMNS = Set("lat", "lon")
  val BOOL_COLUMNS = Set("wheelchair_boarding",
    "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

  def convertColumns(kv: (String, String)): (String, JsValue) = {
    val (key, value) = kv
    key -> (
      if (FLOAT_COLUMNS.contains(key)) {
        JsNumber(value.toDouble)
      } else if (BOOL_COLUMNS.contains(key)) {
        JsBoolean(value == "1")
      } else if (key.endsWith("_date") || key == "date") {
        val (year, rest) = value.splitAt(4)
        val (month, day) = rest.splitAt(2)
        JsString(s"$year-$month-$day")
      } else {
        JsString(value)
      }
    )
  }
  def dropUnicode(str: String) = {
    str.filter{_ < '~'}
  }
}
