/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.twowaymessage.services

import scala.util.matching.Regex
import scala.xml.transform.RewriteRule
import scala.xml.{Elem, Node, NodeSeq, Text}

object XmlTransformService {

  val dateRegex: Regex = """[0-9]{1,2}\s[a-zA-Z]*,?\s[0-9]{4}""".r

  private def getStripElementRule(elementName: String): RewriteRule = {
    new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case elem: Elem if elem.label.eq(elementName) => NodeSeq.Empty
        case `n` => n
      }
    }
  }

  private def updateDatePara(): RewriteRule = {
    new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case elem: Elem if elem.attributes.exists(_.value.text.contains("message_time")) =>
          elem.copy(child = elem.child collect {
            case Text(str) => dateRegex.findFirstIn(str) match {
              case Some(date) => if(str.toLowerCase.contains("you sent")){
                boldSpan(date, text = "the customer")
              } else { boldSpan(date, text = "HMRC") }
              case None => Text(str)
            }
          })
        case `n` => n
      }
    }
  }

  private def boldSpan(date: String, text: String): Elem = {
    <span><span class="govuk-font-weight-bold">{date}</span> by {text}</span>
  }

  def stripH1(nodes: Seq[Node]): Seq[Node] = {
    val rule = getStripElementRule(elementName ="h1")
    nodes.flatMap(node => rule.transform(node))
  }

  def stripH2(nodes: Seq[Node]): Seq[Node] = {
    val rule = getStripElementRule(elementName = "h2")
    nodes.flatMap(node => rule.transform(node))
  }

  def updateDatePara(nodes: Seq[Node]): Seq[Node] = {
    nodes.flatMap(node => updateDatePara().transform(node))
  }

}