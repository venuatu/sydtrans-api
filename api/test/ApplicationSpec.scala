import models.JsonFormat
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.libs.json._

import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Application" should {

    "send 404 on a bad request" in new WithApplication{
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the index page" in new WithApplication{
      val home = route(FakeRequest(GET, "/")).get

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain ("Your new application is ready.")
    }
  }

  case class SomeObject(somethingGreat: String, amusing: String, blEh: String)

  "transformJson" should {
    val testItem = SomeObject("sauce", "comedian", "wut?")
    val objectFmt = Json.format[SomeObject]

    "writes" should {
      "work in play" in {
        Json.toJson(testItem)(objectFmt) must equalTo(
          Json.obj(
            "somethingGreat" -> "sauce",
            "amusing" -> "comedian",
            "blEh" -> "wut?"
          ))
      }

      "underscoreize" in {
        implicit val keyFmt: Format[SomeObject] = JsonFormat.underscorize(objectFmt)
        Json.toJson(testItem) must equalTo(
          Json.obj(
            "something_great" -> "sauce",
            "amusing" -> "comedian",
            "bl_eh" -> "wut?"
        ))
      }
    }

    "reads" should {
      "work in play" in {
        testItem must equalTo(
          Json.parse("""{ "somethingGreat": "sauce", "amusing": "comedian", "blEh": "wut?" }""").as[SomeObject](objectFmt)
        )
      }

      "underscoreize" in {
        implicit val keyFmt: Format[SomeObject] = JsonFormat.underscorize(objectFmt)
        testItem must equalTo(
          Json.parse("""{ "something_great": "sauce", "amusing": "comedian", "bl_eh": "wut?" }""").as[SomeObject]
        )
      }
    }
  }
}
