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

import Printer._
import io.circe._
import java.io.StringWriter
import org.virtuslab.yaml._, Node._
import scala.collection.JavaConverters._

final case class Printer(
  preserveOrder: Boolean = false,
  dropNullKeys: Boolean = false,
  indent: Int = 2,
  maxScalarWidth: Int = 80,
  splitLines: Boolean = true,
  indicatorIndent: Int = 0,
  tags: Map[String, String] = Map.empty,
  sequenceStyle: FlowStyle = FlowStyle.Block,
  mappingStyle: FlowStyle = FlowStyle.Block,
  stringStyle: StringStyle = StringStyle.Plain,
  lineBreak: LineBreak = LineBreak.Unix,
  explicitStart: Boolean = false,
  explicitEnd: Boolean = false,
  version: YamlVersion = YamlVersion.Auto
) {

  def pretty(json: Json): String =
    asYaml(jsonToYaml(json))

  private def isBad(s: String): Boolean = s.indexOf('\u0085') >= 0 || s.indexOf('\ufeff') >= 0
  private def hasNewline(s: String): Boolean = s.indexOf('\n') >= 0

  private def scalarNode(tag: Tag, value: String) = ScalarNode(value)
  private def stringNode(value: String) = ScalarNode(value)
  private def keyNode(value: String) = ScalarNode(value)

  private def jsonToYaml(json: Json): Node = {

    def convertObject(obj: JsonObject) = {
      val fields = if (preserveOrder) obj.keys else obj.keys.toSet
      val m = obj.toMap
      val childNodes: Map[Node, Node] = fields.flatMap { key =>
        val value = m(key)
        if (!dropNullKeys || !value.isNull) Some((keyNode(key) -> jsonToYaml(value)))
        else None
      }.toMap
      MappingNode(childNodes)
    }

    json.fold(
      scalarNode(Tag.nullTag, null),
      bool => scalarNode(Tag.str, bool.toString),
      number => scalarNode(numberTag(number), number.toString),
      str => stringNode(str),
      arr =>
        SequenceNode(
          arr.map(jsonToYaml): _*
        ),
      obj => convertObject(obj)
    )
  }
}

object Printer {

  val spaces2 = Printer()
  val spaces4 = Printer(indent = 4)

  sealed trait FlowStyle
  object FlowStyle {
    case object Flow extends FlowStyle
    case object Block extends FlowStyle
  }

  sealed trait StringStyle
  object StringStyle {
    case object Plain extends StringStyle
    case object DoubleQuoted extends StringStyle
    case object SingleQuoted extends StringStyle
    case object Literal extends StringStyle
    case object Folded extends StringStyle
  }

  sealed trait LineBreak
  object LineBreak {
    case object Unix extends LineBreak
    case object Windows extends LineBreak
    case object Mac extends LineBreak
  }

  sealed trait YamlVersion
  object YamlVersion {
    case object Yaml1_0 extends YamlVersion
    case object Yaml1_1 extends YamlVersion
    case object Auto extends YamlVersion
  }

  private def yamlTag(json: Json) = json.fold(
    Tag.nullTag,
    _ => Tag.boolean,
    number => numberTag(number),
    _ => Tag.str,
    _ => Tag.seq,
    _ => Tag.map
  )

  private def numberTag(number: JsonNumber): Tag =
    if (number.toString.contains(".")) Tag.float else Tag.int
}
