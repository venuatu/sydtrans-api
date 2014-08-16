package models

object Types {

  private def coordeq(l: Double, r: Double): Boolean = {
    Math.abs(l - r) < 0.000001
  }

  case class Coord(lat: Double, lng: Double) {
    override def equals(other: Any): Boolean = other match {
      case other: Coord => coordeq(lat, other.lat) && coordeq(lng, other.lng)
      case _ => false
    }
  }

}


