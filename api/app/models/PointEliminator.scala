package models

import models.Types.Coord

import collection.mutable.ArrayBuffer
import scala.annotation.tailrec

/**
 * A point eliminator based on https://mourner.github.io/simplify-js
 */
object PointEliminator {
  def apply(path: Vector[Coord], tolerance: Double): Vector[Coord] = {
    val ret = simplifyRadialDist(path, tolerance)

    println("eliminated", path.length.toDouble / ret.length, ret.length.toDouble / path.length, path.length, ret.length)

    ret
  }

  def squareDistance(l: Coord, r: Coord): Double = {
    val dx = l.lat - r.lat
    val dy = l.lng - r.lng

    dx * dx + dy * dy
  }

  def squareDistanceSegment(p: Coord, p1: Coord, p2: Coord): Double = {
    var x = p1.lat
    var y = p1.lng
    var dx = p2.lat - x
    var dy = p2.lng - y

    if (dx != 0 || dy != 0) {
      val t = ((p.lat - x) * dx + (p.lng - y) * dy) / (dx * dx + dy * dy)

      if (t > 1) {
        x = p2.lat
        y = p2.lng

      } else if (t > 0) {
        x += dx * t
        y += dy * t
      }
    }

    dx = p.lat - x
    dy = p.lng - y

    dx * dx + dy * dy
  }

  def simplifyRadialDist(points: Vector[Coord], sqTolerance: Double): Vector[Coord] = {
    var prevPoint = points.head

    val newPoints = (for (point <- points if squareDistance(point, prevPoint) > sqTolerance) yield {
      prevPoint = point
      point
    }).toVector

    if (newPoints.isEmpty || newPoints(newPoints.length -1) != prevPoint)
      newPoints :+ prevPoint
    else
      newPoints
  }

  def simplifyDouglasPeucker(points: Vector[Coord], tolerance: Double): Vector[Coord] = {
    // Find the point with the maximum distance
    var dmax = 0.0
    var index = 0
    val end = points.length -1
    for (i <- 1 to end) {
      val d = squareDistanceSegment(points(i), points(0), points(end))
      if ( d > dmax ) {
        index = i
        dmax = d
      }
    }
    // If max distance is greater than epsilon, recursively simplify
    if ( dmax <= tolerance ) {
      points
    } else {
      // Build the result list
      simplifyDouglasPeucker(points.slice(0, index), tolerance) ++
        simplifyDouglasPeucker(points.slice(index, end), tolerance)
    }
  }
}
