package io.circe.yaml

import cats.syntax.either._
import io.circe._
// import java.io.{ Reader, StringReader }
// import org.yaml.snakeyaml.Yaml
// import org.yaml.snakeyaml.constructor.SafeConstructor
// import org.yaml.snakeyaml.nodes._
import org.virtuslab.yaml._, Node._
import scala.collection.JavaConverters._

package object parser {

  // /**
  //  * Parse YAML from the given [[Reader]], returning either [[ParsingFailure]] or [[Json]]
  //  * @param yaml
  //  * @return
  //  */
  // def parse(yaml: Reader): Either[ParsingFailure, Json] = for {
  //   parsed <- parseSingle(yaml)
  //   json <- yamlToJson(parsed)
  // } yield json

  // def parse(yaml: String): Either[ParsingFailure, Json] = parse(new StringReader(yaml))

  // def parseDocuments(yaml: Reader): Stream[Either[ParsingFailure, Json]] = parseStream(yaml).map(yamlToJson)
  // def parseDocuments(yaml: String): Stream[Either[ParsingFailure, Json]] = parseDocuments(new StringReader(yaml))

  // private[this] def parseSingle(reader: Reader) =
  //   Either.catchNonFatal(new Yaml().compose(reader)).leftMap(err => ParsingFailure(err.getMessage, err))

  // private[this] def parseStream(reader: Reader) =
  //   new Yaml().composeAll(reader).asScala.toStream

  // private[this] object CustomTag {
  //   def unapply(tag: Tag): Option[String] = if (!tag.startsWith(Tag.PREFIX))
  //     Some(tag.getValue)
  //   else
  //     None
  // }

  private[this] class FlatteningConstructor extends SafeConstructor {
    def flatten(node: MappingNode): MappingNode = {
      flattenMapping(node)
      node
    }

    def construct(node: ScalarNode): Object =
      getConstructor(node).construct(node)
  }

  private[this] def yamlToJson(node: Node): Either[ParsingFailure, Json] = {
    // Isn't thread-safe internally, may hence not be shared
    val flattener: FlatteningConstructor = new FlatteningConstructor

    def convertScalarNode(node: ScalarNode) = Either
      .catchNonFatal(node.tag match {
        case Tag.int if node.value.startsWith("0x") || node.value.contains("_") =>
          Json.fromJsonNumber(flattener.construct(node) match {
            case int: Integer         => JsonLong(int.toLong)
            case long: java.lang.Long => JsonLong(long)
            case bigint: java.math.BigInteger =>
              JsonDecimal(bigint.toString)
            case other => throw new NumberFormatException(s"Unexpected number type: ${other.getClass}")
          })
        case Tag.int | Tag.float =>
          JsonNumber.fromString(node.value).map(Json.fromJsonNumber).getOrElse {
            throw new NumberFormatException(s"Invalid numeric string ${node.value}")
          }
        case Tag.boolean =>
          Json.fromBoolean(flattener.construct(node) match {
            case b: java.lang.Boolean => b
            case _                    => throw new IllegalArgumentException(s"Invalid boolean string ${node.value}")
          })
        case Tag.nullTag => Json.Null
        case CustomTag(other) =>
          Json.fromJsonObject(JsonObject.singleton(other.stripPrefix("!"), Json.fromString(node.value)))
        case other => Json.fromString(node.value)
      })
      .leftMap { err =>
        ParsingFailure(err.getMessage, err)
      }

    def convertKeyNode(node: Node) = node match {
      case scalar: ScalarNode => Right(scalar.value)
      case _                  => Left(ParsingFailure("Only string keys can be represented in JSON", null))
    }

    if (node == null) {
      Right(Json.False)
    } else {
      node match {
        case mapping: MappingNode =>
          flattener
            .flatten(mapping)
            .getValue
            .asScala
            .foldLeft(
              Either.right[ParsingFailure, JsonObject](JsonObject.empty)
            ) { (objEither, tup) =>
              for {
                obj <- objEither
                key <- convertKeyNode(tup.getKeyNode)
                value <- yamlToJson(tup.getValueNode)
              } yield obj.add(key, value)
            }
            .map(Json.fromJsonObject)
        case sequence: SequenceNode =>
          sequence.nodes
            .foldLeft(Either.right[ParsingFailure, List[Json]](List.empty[Json])) { (arrEither, node) =>
              for {
                arr <- arrEither
                value <- yamlToJson(node)
              } yield value :: arr
            }
            .map(arr => Json.fromValues(arr.reverse))
        case scalar: ScalarNode => convertScalarNode(scalar)
      }
    }
  }
}
