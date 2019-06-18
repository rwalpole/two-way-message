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

import com.codahale.metrics.SharedMetricRegistries
import org.joda.time.LocalDate

import scala.concurrent.ExecutionContext
import org.mockito.Mockito._
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.twowaymessage.assets.Fixtures
import uk.gov.hmrc.twowaymessage.model
import uk.gov.hmrc.twowaymessage.model._

class HtmlCreatorServiceSpec extends WordSpec with Matchers with GuiceOneAppPerSuite with Fixtures with MockitoSugar {

  implicit val mockExecutionContext = mock[ExecutionContext]
  implicit val mockHeaderCarrier = mock[HeaderCarrier]

  lazy val mockhttpClient = mock[HttpClient]
  lazy val mockServiceConfig = mock[ServicesConfig]

  val injector = new GuiceApplicationBuilder()

  implicit val htmlCreatorService  = injector.injector()
    .instanceOf[HtmlCreatorService]

  val latestMessage = "5d02201b5b0000360151779e"

  val listOfConversationItems = List(
    ConversationItem(
      "5d02201b5b0000360151779e",
      "Matt Test 1",
      Some(ConversationItemDetails(MessageType.Adviser,
        FormId.Reply,
        Some(LocalDate.parse("2019-06-13")),
        Some("5d021fbe5b0000200151779c"),
        Some("P800"))),
      LocalDate.parse("2019-06-13"),
      Some("Dear TestUser Thank you for your message of 13 June 2019.</br>To recap your question, " +
        "I think you're asking for help with</br>I believe this answers your question and hope you are satisfied with the response. " +
        "There's no need to send a reply. " +
        "But if you think there's something important missing, just ask another question about this below." +
        "</br>Regards</br>Matthew Groom</br>HMRC digital team.")
  ),ConversationItem(
      "5d021fbe5b0000200151779c",
      "Matt Test 1",
      Some(ConversationItemDetails(MessageType.Customer,
        FormId.Question,
        Some(LocalDate.parse("2019-06-13")),
        None,
        Some("p800"))),
      LocalDate.parse("2019-06-13"),
      Some("Hello, my friend!")))

  "createConversation" should {
    "should create a long html string of an entire conversation" in {
      val result = htmlCreatorService.createConversation(latestMessage, listOfConversationItems)
      result.shouldBe("""<h1 class="govuk-heading-xl margin-top-small margin-bottom-small">
          Matt Test 1
        </h1><p class="message_time faded-text--small">
        This message was sent to you on 13 June 2019
      </p><p>
          Dear TestUser Thank you for your message of 13 June 2019.&lt;/br&gt;To recap your question, I think you're asking for help with&lt;/br&gt;I believe this answers your question and hope you are satisfied with the response. There's no need to send a reply. But if you think there's something important missing, just ask another question about this below.&lt;/br&gt;Regards&lt;/br&gt;Matthew Groom&lt;/br&gt;HMRC digital team.<br/>
        </p> <a href="/two-way-message-frontend/message/customer/P800/5d02201b5b0000360151779e/reply#reply-input-label">Send another message about this</a> <h2 class="govuk-heading-xl margin-top-small margin-bottom-small">
          Matt Test 1
        </h2><p class="message_time faded-text--small">
        You sent this message on 13 June 2019
      </p><p>
          Hello, my friend!<br/>
        </p>""")
    }
  SharedMetricRegistries.clear()}

  "sortConversation" should {
    "sort a conversation" in {
      val result = htmlCreatorService.sortConversation(latestMessage, listOfConversationItems)
      result.shouldBe(listOfConversationItems)
    }
  }
}
