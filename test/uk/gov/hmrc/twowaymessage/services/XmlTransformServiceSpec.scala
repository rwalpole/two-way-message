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

import org.scalatest.{Matchers, WordSpec}
import org.scalatest.mockito.MockitoSugar

import scala.xml.NodeSeq

class XmlTransformServiceSpec extends WordSpec with Matchers with MockitoSugar {

  val sourceXml: NodeSeq = <h1>hello</h1> <h2>there</h2> <p>world</p>

  "XmlTransformService.stripH1" should {

    "remove all <h1/> elements" in {
      val expectedXml = <h2>there</h2> <p>world</p>
      val result = XmlTransformService.stripH1(sourceXml)
      result shouldBe expectedXml
    }
  }

  "XmlTranformService.stripH1" should {

    "remove all <h2/> elements" in {
      val expectedXml = <h1>hello</h1><p>world</p>
      val result = XmlTransformService.stripH2(sourceXml)
      result shouldBe expectedXml
    }
  }

  val xml1: NodeSeq = <p class="message_time faded-text--small">This message was sent to you on 12 March, 2019</p>
  val xml2: NodeSeq = <p class="message_time faded-text--small">You sent this message on 12 March, 2019</p>

  "XmlTransformService.updateDateText" should {
    "update the p tag 'message_time' text with 'from HMRC'" in {
      val expectedXml = List(<p class="message_time faded-text--small"><span><span class="govuk-font-weight-bold">12 March, 2019</span> from HMRC</span></p>)
      val result = XmlTransformService.updateDatePara(xml1)
      result.mkString shouldBe expectedXml.mkString
    }

    "update the p tag with class 'message_time' text with 'from Customer'" in {
      val expectedXml = List(<p class="message_time faded-text--small"><span><span class="govuk-font-weight-bold">12 March, 2019</span> from Customer</span></p>)
      val result = XmlTransformService.updateDatePara(xml2)
      result.mkString shouldBe expectedXml.mkString
    }

    "not update the p tag text when it is not of class 'message_time'" in {
      val xml3 = List(<p class="faded-text--small">This message was sent to you on 12 March, 2019</p>)
      val result = XmlTransformService.updateDatePara(xml3)
      result shouldBe xml3
    }

    "not update the p tag text when it does not contain a date" in {
      val xml4 = List(<p class="message_time faded-text--small">This message was sent to you</p>)
      val result = XmlTransformService.updateDatePara(xml4)
      result shouldBe xml4
    }

    "only update the p tag that contains a date" in {
      val xml5 = List(<p class="message_time faded-text--small">This message was sent to you on 12 March, 2019</p>,
        <p class="message_time faded-text--small">This message was sent to you</p>)
      val expectedXml =  List(<p class="message_time faded-text--small"><span><span class="govuk-font-weight-bold">12 March, 2019</span> from HMRC</span></p>,
        <p class="message_time faded-text--small">This message was sent to you</p>)
      val result = XmlTransformService.updateDatePara(xml5)
      result.mkString shouldBe expectedXml.mkString
    }
  }

}
