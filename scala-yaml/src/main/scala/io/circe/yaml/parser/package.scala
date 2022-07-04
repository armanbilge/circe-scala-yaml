/*
 * Copyright 2022 circe-yaml contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe.yaml

import cats.syntax.either._
import io.circe._
// import java.io.{ Reader, StringReader }
import org.virtuslab.yaml._, Node._
import scala.collection.JavaConverters._
import scala.util.control.NoStackTrace

package object parser {

  final case class WrappedYamlError(error: YamlError) extends Exception with NoStackTrace {
    override def getMessage(): String = error.msg
  }

  // /**
  //  * Parse YAML from the given [[Reader]], returning either [[ParsingFailure]] or [[Json]]
  //  * @param yaml
  //  * @return
  //  */
  // def parse(yaml: Reader): Either[ParsingFailure, Json] = for {
  //   parsed <- parseSingle(yaml)
  //   json <- yamlToJson(parsed)
  // } yield json

  def parse(yaml: String): Either[ParsingFailure, Json] =
    asNode(yaml).leftMap(e => ParsingFailure(e.msg, WrappedYamlError(e))).flatMap(yamlToJson(_))

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

  private[this] def yamlToJson(node: Node): Either[ParsingFailure, Json] = {

    def convertScalarNode(node: ScalarNode): Either[ParsingFailure, Json] = node.tag match {
      // case Tag.int if node.value.startsWith("0x") || node.value.contains("_") =>
      // TODO
      case Tag.int | Tag.float =>
        JsonNumber.fromString(node.value).map(Json.fromJsonNumber).toRight {
          val msg = s"Invalid numeric string ${node.value}"
          ParsingFailure(msg, new NumberFormatException(msg))
        }
      case Tag.boolean =>
        YamlDecoder.given_YamlDecoder_Boolean
          .construct(node)
          .leftMap(e => ParsingFailure(e.msg, WrappedYamlError(e)))
          .map(Json.fromBoolean(_))
      case Tag.nullTag => Right(Json.Null)
      case CustomTag(other) =>
        Right(Json.fromJsonObject(JsonObject.singleton(other.stripPrefix("!"), Json.fromString(node.value))))
      case other => Right(Json.fromString(node.value))
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
          mapping.mappings
            .foldLeft(
              Either.right[ParsingFailure, JsonObject](JsonObject.empty)
            ) { case (objEither, (keyNode, valueNode)) =>
              for {
                obj <- objEither
                key <- convertKeyNode(keyNode)
                value <- yamlToJson(valueNode)
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
