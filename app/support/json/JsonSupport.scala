package support.json

import java.text.SimpleDateFormat

import com.github.tototoshi.play2.json4s.Json4s
import enumeratum.Json4s
import models.{CellState, GameState}
import org.json4s.ext.JavaTimeSerializers
import org.json4s.{DefaultFormats, Extraction, Formats, JValue}
import play.api.http.{ContentTypeOf, Writeable}
import play.api.mvc.Codec

object JsonValues {
  val DateTimeFormat: String = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
}

trait JsonSerializer {
  def extract[T](value: JValue)(implicit mf: Manifest[T]): T

  def decompose(obj: Any): JValue
}

trait Json4SSerializer extends JsonSerializer {

  val formats: Formats = (new DefaultFormats {
    override val dateFormatter: SimpleDateFormat = new SimpleDateFormat(JsonValues.DateTimeFormat)
  } ++ JavaTimeSerializers.all
    + Json4s.serializer(GameState)
    + Json4s.serializer(CellState))
    .strict
    .withTypeHintFieldName("type")

  def extract[T](value: JValue)(implicit mf: Manifest[T]): T = {
    value.camelizeKeys.extract[T](formats, mf)
  }

  def decompose(obj: Any): JValue = {
    Extraction.decompose(obj)(formats).snakizeKeys
  }
}

object Json4SSerializer extends Json4SSerializer

trait JsonSupport {

  val jsonSerializer: JsonSerializer = Json4SSerializer

  implicit class JValueParser(value: JValue) {
    def extract[T](implicit mf: Manifest[T]): T = jsonSerializer.extract(value)
  }

  implicit class ObjParser(obj: Any) {
    def asJson: JValue = jsonSerializer.decompose(obj)
  }

}

trait PlayJsonExtension {

  def json4s: Json4s

  implicit def writeableOf_JValue(implicit codec: Codec): Writeable[JValue] =
    json4s.implicits.writeableOf_JValue

  implicit def contentTypeOf_JValue(implicit codec: Codec): ContentTypeOf[JValue] =
    json4s.implicits.contentTypeOf_JValue
}
