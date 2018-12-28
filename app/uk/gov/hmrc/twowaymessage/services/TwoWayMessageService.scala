/*
 * Copyright 2018 HM Revenue & Customs
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

import java.util.UUID
import java.util.UUID.randomUUID
import java.util.concurrent.TimeUnit

import com.google.inject.Inject
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.{Result, Results}
import play.api.mvc.Results._
import uk.gov.hmrc.http._
import uk.gov.hmrc.twowaymessage.connectors.MessageConnector
import uk.gov.hmrc.twowaymessage.model.CommonFormats._
import uk.gov.hmrc.twowaymessage.model.Error
import uk.gov.hmrc.twowaymessage.model.FormId.FormId
import uk.gov.hmrc.twowaymessage.model.MessageType.MessageType
import uk.gov.hmrc.twowaymessage.model._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class TwoWayMessageService @Inject()(messageConnector: MessageConnector)(implicit ec: ExecutionContext) {

  implicit val hc = HeaderCarrier()

  def post(twoWayMessage: TwoWayMessage): Future[Result] = {
    val body = createJsonForMessage(randomUUID.toString, MessageType.Customer, FormId.Question, twoWayMessage)
    messageConnector.postMessage(body) map {
      handleResponse
    } recover {
      case e: Upstream4xxResponse => BadGateway(Json.obj("error" -> e.upstreamResponseCode, "message" -> e.message))
      case e: Upstream5xxResponse => BadGateway(Json.obj("error" -> e.upstreamResponseCode, "message" -> e.message))
      case e: HttpException => BadGateway(Json.obj("error" -> e.responseCode, "message" -> e.message))
    }
  }

  def postAdvisorReply(twoWayMessageReply: TwoWayMessageReply, replyTo: String): Future[Result] = {
    (for {
      metadata <- messageConnector.getMessageMetadata(replyTo)
      body = createJsonForReply(randomUUID.toString, MessageType.Advisor, FormId.Reply, metadata, twoWayMessageReply, replyTo)
      resp <- messageConnector.postMessage(body)
    } yield resp) map {
      handleResponse
    } recover {
      case e: Upstream4xxResponse => BadGateway(Json.obj("error" -> e.upstreamResponseCode, "message" -> e.message))
      case e: Upstream5xxResponse => BadGateway(Json.obj("error" -> e.upstreamResponseCode, "message" -> e.message))
      case e: HttpException => BadGateway(Json.obj("error" -> e.responseCode, "message" -> e.message))
    }
  }

  def postCustomerReply(twoWayMessageReply: TwoWayMessageReply, replyTo: String): Future[Result] = {
    (for {
      metadata <- messageConnector.getMessageMetadata(replyTo)
      body = createJsonForReply(randomUUID.toString, MessageType.Customer, FormId.Question, metadata, twoWayMessageReply, replyTo)
      resp <- messageConnector.postMessage(body)
    } yield resp) map {
      handleResponse
    } recover {
      case e: Upstream4xxResponse => BadGateway(Json.obj("error" -> e.upstreamResponseCode, "message" -> e.message))
      case e: Upstream5xxResponse => BadGateway(Json.obj("error" -> e.upstreamResponseCode, "message" -> e.message))
      case e: HttpException => BadGateway(Json.obj("error" -> e.responseCode, "message" -> e.message))
    }
  }

  def handleResponse(response: HttpResponse): Result = response.status match {
    case CREATED => Created(Json.parse(response.body))
    case _ => BadGateway(Json.toJson(Error(response.status, response.body)))
  }

  def createJsonForMessage(id: String,
                           messageType: MessageType,
                           formId: FormId,
                           twoWayMessage: TwoWayMessage): Message =
    Message(
      ExternalRef(id, "2WSM"),
      twoWayMessage.recipient,
      messageType,
      twoWayMessage.subject,
      twoWayMessage.content.getOrElse(""),
      Details(formId, None)
    )

  def createJsonForReply(id: String,
                         messageType: MessageType,
                         formId: FormId,
                         metadata: MessageMetadata,
                         reply: TwoWayMessageReply,
                         replyTo: String): Message = {
    Message(
      ExternalRef(id, "2WSM"),
      Recipient(
        TaxIdentifier(metadata.recipient.identifier.name, metadata.recipient.identifier.value),
        metadata.recipient.email.getOrElse("")
      ),
      messageType,
      s"RE: ${metadata.subject}",
      reply.content,
      Details(formId, Some(replyTo))
    )
  }
}
