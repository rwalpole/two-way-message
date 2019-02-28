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

import java.util.UUID.randomUUID

import com.google.inject.Inject
import com.sun.security.jgss.InquireType
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.twowaymessage.connectors.MessageConnector
import uk.gov.hmrc.twowaymessage.model.Error._
import uk.gov.hmrc.twowaymessage.model.FormId.FormId
import uk.gov.hmrc.twowaymessage.model.MessageType.MessageType
import uk.gov.hmrc.twowaymessage.model.{ Error, _ }

import scala.concurrent.{ ExecutionContext, Future }

class TwoWayMessageService @Inject()(messageConnector: MessageConnector)(implicit ec: ExecutionContext) {

  implicit val hc = HeaderCarrier()

  def post(queueId: String, nino: Nino, twoWayMessage: TwoWayMessage): Future[Result] = {

    val body = createJsonForMessage(randomUUID.toString, twoWayMessage, nino, queueId)

    messageConnector.postMessage(body) map {
      handleResponse
    } recover handleError
  }

  def postReply(twoWayMessageReply: TwoWayMessageReply, replyTo: String, messageType: MessageType, formId: FormId)(
    implicit hc: HeaderCarrier): Future[Result] =
    (for {
      metadata <- messageConnector.getMessageMetadata(replyTo)
      body = createJsonForReply(randomUUID.toString, messageType, formId, metadata, twoWayMessageReply, replyTo)
      resp <- messageConnector.postMessage(body)
    } yield resp) map {
      handleResponse
    } recover handleError

  def postAdvisorReply(twoWayMessageReply: TwoWayMessageReply, replyTo: String)(
    implicit hc: HeaderCarrier): Future[Result] =
    postReply(twoWayMessageReply, replyTo, MessageType.Advisor, FormId.Reply)

  def postCustomerReply(twoWayMessageReply: TwoWayMessageReply, replyTo: String)(
    implicit hc: HeaderCarrier): Future[Result] =
    postReply(twoWayMessageReply, replyTo, MessageType.Customer, FormId.Question)

  val errorResponse = (status: Int, message: String) => BadGateway(Json.toJson(Error(status, message)))

  def handleResponse(response: HttpResponse): Result = response.status match {
    case CREATED => Created(Json.parse(response.body))
    case _       => errorResponse(response.status, response.body)
  }

  def handleError(): PartialFunction[Throwable, Result] = {
    case e: Upstream4xxResponse => errorResponse(e.upstreamResponseCode, e.message)
    case e: Upstream5xxResponse => errorResponse(e.upstreamResponseCode, e.message)
    case e: HttpException       => errorResponse(e.responseCode, e.message)
  }

  def createJsonForMessage(refId: String, twoWayMessage: TwoWayMessage, nino: Nino, queueId: String): Message = {
    val recipient = Recipient(TaxIdentifier(nino.name, nino.value), twoWayMessage.contactDetails.email)
    Message(
      ExternalRef(refId, "2WSM"),
      recipient,
      MessageType.Customer,
      twoWayMessage.subject,
      twoWayMessage.content,
      Details(FormId.Question, None, None, inquiryType = Some(queueId))
    )
  }

  def createJsonForReply(
    refId: String,
    messageType: MessageType,
    formId: FormId,
    metadata: MessageMetadata,
    reply: TwoWayMessageReply,
    replyTo: String): Message =
    Message(
      ExternalRef(refId, "2WSM"),
      Recipient(
        TaxIdentifier(metadata.recipient.identifier.name, metadata.recipient.identifier.value),
        metadata.recipient.email.getOrElse("")
      ),
      messageType,
      metadata.subject,
      reply.content,
      Details(formId, Some(replyTo), metadata.details.threadId, metadata.details.enquiryType, metadata.details.adviser)
    )
}
