package models

import models.Types.Coord

import collection.mutable.ArrayBuffer

object Polyline {
  def decode(encoded: String): Seq[Coord] = {
    val coords = getChunks(encoded).map{getCoord}

    var prev = Coord(0, 0)
    for (arr <- coords.grouped(2).toVector) yield {
      val ret = Coord(prev.lat + arr(0), prev.lng + arr(1))
      prev = ret
      ret
    }
  }

  def getChunks(encoded: String): Seq[Seq[Int]] = {
    var locs = ArrayBuffer[Seq[Int]]()
    var buf = ArrayBuffer[Int]()
    for (ch <- encoded.map{_.toInt - 63}) {
      buf += (ch & 0x1f)
      if ((ch & 0x20) == 0) {
        locs += buf.toList
        buf = ArrayBuffer[Int]()
      }
    }
    locs.toList
  }

  def getCoord(chunk: Seq[Int]): Double = {
    var coord: Long = 0
    for ((ch, i) <- chunk.zipWithIndex) {
      coord |= ch << (i * 5)
    }
    if ((coord & 0x1) != 0) coord = ~coord
    val out = (coord >> 1) / 100000.0
    out.toDouble
  }

  case class dc(lat: Double, dellat: Double, lng: Double, dellng: Double)

  def encode(coords: Seq[Coord]): String = {
    var prevLat = 0
    var prevLng = 0
    //val dcs = collection.mutable.ArrayBuffer[dc]()
    val ret = coords.map{coord =>
      val lat = (coord.lat * 1e5).toInt
      val lng = (coord.lng * 1e5).toInt
      //dcs.+=(dc(lat, lat - prevLat, lng, lng - prevLng))
      val ret = encodeCoord(lat - prevLat) ++ encodeCoord(lng - prevLng)
      prevLat = lat
      prevLng = lng

      ret
    }.flatten.mkString
    //println(dcs)
    ret
  }

  def encodeCoord(num: Int): Seq[Char] = {
    var res = num << 1
    if (res < 0) res = ~res

    val chunks = ArrayBuffer[Int]()
    while (res >= 0x20) {
      chunks += ((res & 0x1f) | 0x20)
      res >>= 5
    }
    chunks += res

    chunks.toVector.map{_ + 63}.map{_.toChar}
  }
}
