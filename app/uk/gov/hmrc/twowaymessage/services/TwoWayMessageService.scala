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

import com.google.inject.ImplementedBy
import org.apache.commons.codec.binary.Base64
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.gform.dms.DmsMetadata
import uk.gov.hmrc.http._
import uk.gov.hmrc.twowaymessage.model.{Error, _}
import uk.gov.hmrc.twowaymessage.model.Error._
import uk.gov.hmrc.twowaymessage.model.FormId.FormId
import uk.gov.hmrc.twowaymessage.model.MessageType.MessageType

import scala.concurrent.Future
import scala.language.implicitConversions

@ImplementedBy(classOf[TwoWayMessageServiceImpl])
trait TwoWayMessageService {

  type ErrorFunction = (Int,String) => Result

  val errorResponse: ErrorFunction = (status: Int, message: String) => BadGateway(Json.toJson(Error(status, message)))

  def getMessageMetadata(messageId: String)(implicit hc: HeaderCarrier): Future[Option[MessageMetadata]]

  def post(queueId: String, nino: Nino, twoWayMessage: TwoWayMessage, dmsMetaData: DmsMetadata)(implicit hc: HeaderCarrier): Future[Result]

  def postAdvisorReply(twoWayMessageReply: TwoWayMessageReply, replyTo: String)(implicit hc: HeaderCarrier): Future[Result]

  def postCustomerReply(twoWayMessageReply: TwoWayMessageReply, replyTo: String)(implicit hc: HeaderCarrier): Future[Result]

  def createDmsSubmission(html: String, response: HttpResponse, dmsMetaData: DmsMetadata)(implicit hc: HeaderCarrier): Future[Result]

  def createHtmlMessage(messageId: String, nino: Nino, messageContent: String, subject: String)(implicit hc: HeaderCarrier): Future[Option[String]]

  def getMessageContentBy(messageId: String)(implicit hc: HeaderCarrier): Future[Option[String]]

  def createJsonForMessage(refId: String, twoWayMessage: TwoWayMessage, nino: Nino, queueId: String): Message = {
    val recipient = Recipient(TaxIdentifier(nino.name, nino.value), twoWayMessage.contactDetails.email)
    Message(
      ExternalRef(refId, "2WSM"),
      recipient,
      MessageType.Customer,
      twoWayMessage.subject,
      twoWayMessage.content,
      Details(FormId.Question, None, None, enquiryType = Some(queueId))
    )
  }

  def createJsonForReply(refId: String, messageType: MessageType, formId: FormId, metadata: MessageMetadata,
                         reply: TwoWayMessageReply, replyTo: String): Message =
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

  def encodeToBase64String(text: String): String =
    Base64.encodeBase64String(text.getBytes("UTF-8"))

  protected def getContent(response: HttpResponse): Option[String] = {
    response.status match {
      case OK => Some(response.body)
      case _ => None
    }
  }

  protected def handleResponse(response: HttpResponse): Result = response.status match {
    case CREATED => Created(Json.parse(response.body))
    case _       => errorResponse(response.status, response.body)
  }

  protected def handleError(): PartialFunction[Throwable, Result] = {
    case e: Upstream4xxResponse => errorResponse(e.upstreamResponseCode, e.message)
    case e: Upstream5xxResponse => errorResponse(e.upstreamResponseCode, e.message)
    case e: HttpException       => errorResponse(e.responseCode, e.message)
  }

}
