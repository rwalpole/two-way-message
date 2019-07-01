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

package uk.gov.hmrc.twowaymessage.controllers

import java.util.{Base64, UUID}

import com.codahale.metrics.SharedMetricRegistries
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.Created
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import play.mvc.Http
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, InsufficientEnrolments, MissingBearerToken}
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.{Name, Retrievals, ~}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.gform.dms.DmsMetadata
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.twowaymessage.assets.TestUtil
import uk.gov.hmrc.twowaymessage.connector.mocks.MockAuthConnector
import uk.gov.hmrc.twowaymessage.connectors.MessageConnector
import uk.gov.hmrc.twowaymessage.model._
import uk.gov.hmrc.twowaymessage.services.TwoWayMessageService
import uk.gov.hmrc.twowaymessage.model.MessageFormat._

import scala.concurrent.Future

class HtmlCreationSpec extends TestUtil with MockAuthConnector {

  val mockMessageService = mock[TwoWayMessageService]
  val mockMessageConnector = mock[MessageConnector]

  override lazy val injector = new GuiceApplicationBuilder()
    .overrides(bind[AuthConnector].to(mockAuthConnector))
    .overrides(bind[MessageConnector].to(mockMessageConnector))
    .injector()

  val testTwoWayMessageController = injector.instanceOf[TwoWayMessageController]

  val authPredicate: Predicate = EmptyPredicate

  val twoWayMessageGood = Json.parse("""
                                       |    {
                                       |      "contactDetails": {
                                       |         "email":"someEmail@test.com"
                                       |      },
                                       |      "subject":"QUESTION",
                                       |      "content":"SGVsbG8gV29ybGQ="
                                       |    }""".stripMargin)

  val fakeRequest1 = FakeRequest(
    Helpers.POST,
    routes.TwoWayMessageController.createMessage("queueName").url,
    FakeHeaders(),
    twoWayMessageGood)


  val listOfConversationItems = List(
    ConversationItem(
      id = "5d02201b5b0000360151779e",
      "Matt Test 1",
      Some(ConversationItemDetails(MessageType.Adviser,
        FormId.Reply,
        Some(LocalDate.parse("2019-06-13")),
        Some("5d021fbe5b0000200151779c"),
        Some("P800"))),
      LocalDate.parse("2019-06-13"),
      Some(Base64.getEncoder.encodeToString("Dear TestUser Thank you for your message of 13 June 2019.<br/>To recap your question, I think you're asking for help with<br/>I believe this answers your question and hope you are satisfied with the response. There's no need to send a reply. But if you think there's something important missing, just ask another question about this below. <br/>Regards<br/>Matthew Groom<br/>HMRC digital team.".getBytes))
    ),ConversationItem(
      "5d021fbe5b0000200151779c",
      "Matt Test 1",
      Some(ConversationItemDetails(MessageType.Customer,
        FormId.Question,
        Some(LocalDate.parse("2019-06-13")),
        None,
        Some("p800"))),
      LocalDate.parse("2019-06-13"),
      Some(Base64.getEncoder.encodeToString("Hello, my friend!".getBytes))))

  "The TwoWayMessageController.getContentBy method" should {
    "return 200 (OK) with the content of the conversation in html" in {
      val nino = Nino("AB123456C")
      mockAuthorise(Enrolment("HMRC-NI"))(Future.successful(Some(nino.value)))
      when(
        mockMessageService.postCustomerReply(any[TwoWayMessageReply], ArgumentMatchers.eq("replyTo"))(
          any[HeaderCarrier]))
        .thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))

      when(
        mockMessageConnector
          .getMessages(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(
          HttpResponse(Http.Status.OK, Some(Json.toJson(listOfConversationItems)))))

      val result = await(testTwoWayMessageController.getContentBy("5d02201b5b0000360151779e", "Adviser")(fakeRequest1).run() )
      status(result) shouldBe Status.OK
      bodyOf(result) shouldBe
        """<p class="message_time faded-text--small">
          |          <span>13 June 2019 by HMRC</span>
          |        </p><p>Dear TestUser Thank you for your message of 13 June 2019.<br/>To recap your question, I think you're asking for help with<br/>I believe this answers your question and hope you are satisfied with the response. There's no need to send a reply. But if you think there's something important missing, just ask another question about this below. <br/>Regards<br/>Matthew Groom<br/>HMRC digital team.</p><p class="message_time faded-text--small">
          |          <span>13 June 2019 by the customer</span>
          |        </p><p>Hello, my friend!</p>""".stripMargin
    }

    "return 200 dwq with the content of the conversation in html" in {
      val nino = Nino("AB123456C")
      mockAuthorise(Enrolment("HMRC-NI"))(Future.successful(Some(nino.value)))
      when(
        mockMessageService.postCustomerReply(any[TwoWayMessageReply], ArgumentMatchers.eq("replyTo"))(
          any[HeaderCarrier]))
        .thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))

      when(
        mockMessageConnector
          .getMessages(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(
          HttpResponse(Http.Status.OK, Some(Json.toJson(listOfConversationItems)))))

      val result = await(testTwoWayMessageController.getContentBy("5d02201b5b0000360151779e", "Customer")(fakeRequest1).run() )
      status(result) shouldBe Status.OK
      bodyOf(result) shouldBe
        """<h1 class="govuk-heading-xl margin-top-small margin-bottom-small">
          |          Matt Test 1
          |        </h1><p class="message_time faded-text--small">
          |        This message was sent to you on 13 June 2019
          |      </p><p>
          |          Dear TestUser Thank you for your message of 13 June 2019.<br/>To recap your question, I think you're asking for help with<br/>I believe this answers your question and hope you are satisfied with the response. There's no need to send a reply. But if you think there's something important missing, just ask another question about this below. <br/>Regards<br/>Matthew Groom<br/>HMRC digital team.
          |        </p><a href="/two-way-message-frontend/message/customer/P800/5d02201b5b0000360151779e/reply#reply-input-label">Send another message about this</a><h2 class="govuk-heading-xl margin-top-small margin-bottom-small">
          |          Matt Test 1
          |        </h2><p class="message_time faded-text--small">
          |        You sent this message on 13 June 2019
          |      </p><p>
          |          Hello, my friend!
          |        </p>""".stripMargin
    }



    "return 400 (bad request)  with no content in body" in {
      val nino = Nino("AB123456C")
      mockAuthorise(Enrolment("HMRC-NI"))(Future.successful(Some(nino.value)))
      when(
        mockMessageService.postCustomerReply(any[TwoWayMessageReply], ArgumentMatchers.eq("replyTo"))(
          any[HeaderCarrier]))
        .thenReturn(Future.successful(Created(Json.toJson("id" -> UUID.randomUUID().toString))))
      val result = await(testTwoWayMessageController.getContentBy("1", "nfejwk")(fakeRequest1).run() )
      status(result) shouldBe Status.BAD_REQUEST
    }


    SharedMetricRegistries.clear
  }

}

