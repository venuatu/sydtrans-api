package models

import play.api.libs.json._

/**
 * Created by steve on 10/07/2014.
 */
object JsonFormat {

  def writesFunc[T](write: Writes[T], outOf: String => String) = {
    obj: T => transformKeys(write.writes(obj), outOf)
  }

  def readsFunc[T](read: Reads[T], into: String => String) = {
    json: JsValue => read.reads(transformKeys(json, into))
  }

  def transformReads[T](read: Reads[T], into: String => String) = {
    val reader = readsFunc(read, into)
    new Reads[T] {
      override def reads(json: JsValue): JsResult[T] = reader(json)
    }
  }

  def transformWrites[T](write: Writes[T], outOf: String => String) = {
    val writer = writesFunc(write, outOf)
    new Writes[T] {
      override def writes(obj: T): JsValue = writer(obj)
    }
  }

  def transformFormat[T](format: Format[T], into: String => String, outOf: String => String): Format[T] = {
    val reader = readsFunc(format, into)
    val writer = writesFunc(format, outOf)
    new Format[T] {
      override def reads(json: JsValue): JsResult[T] = reader(json)
      override def writes(obj: T): JsValue = writer(obj)
    }
  }

  def transformKeys(json: JsValue, transform: String => String): JsValue = {
    JsObject(json.asInstanceOf[JsObject].fields.map{case (key, value) => (transform(key), value)})
  }

  def underToCamel(str: String): String = {
    val parts = str.split('_')
    parts.head + parts.tail.map{_.capitalize}.mkString
  }

  def camelToUnder(str: String): String = {
    str.zipWithIndex.map{case (ch, i) =>
      if (ch.isUpper) (if (i != 0) "_" else "") + ch.toLower
      else ch
    }.mkString
  }

  def underscorize[T](format: Format[T]): Format[T] = {
    transformFormat(format, underToCamel, camelToUnder)
  }

}
