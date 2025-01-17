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

import javax.inject.{ Inject, Singleton }

import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve.{ Name, Retrievals, ~ }
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.gform.dms.DmsMetadata
import uk.gov.hmrc.gform.gformbackend.GformConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.controller.WithJsonBody
import uk.gov.hmrc.twowaymessage.enquiries.Enquiry
import uk.gov.hmrc.twowaymessage.model._
import uk.gov.hmrc.twowaymessage.model.MessageMetadataFormat._
import uk.gov.hmrc.twowaymessage.model.TwoWayMessageFormat._
import uk.gov.hmrc.twowaymessage.services.TwoWayMessageService

import scala.concurrent.{ ExecutionContext, Future }
import uk.gov.hmrc.twowaymessage.model.MessageFormat._

@Singleton
class TwoWayMessageController @Inject()(
  twms: TwoWayMessageService,
  val authConnector: AuthConnector,
  val gformConnector: GformConnector)(implicit ec: ExecutionContext)
    extends InjectedController with WithJsonBody with AuthorisedFunctions {

  // Customer creating a two-way message
  def createMessage(queueId: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
    authorised(Enrolment("HMRC-NI")).retrieve(Retrievals.nino and Retrievals.name) {
      case Some(ninoId) ~ name => validateAndPostMessage(queueId, Nino(ninoId), request.body, name)
      case None ~ name =>
        Logger.info("No nino found for user")
        Future.successful(Forbidden(Json.toJson("Not authenticated")))
    } recover handleError
  }

  def getRecipientMetadata(messageId: String): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
    twms.getMessageMetadata(messageId).map {
      case Some(m) => Ok(Json.toJson(m))
      case None    => NotFound
    }
  }

  def getRecipientMessageContentBy(messageId: String): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
    twms.getMessageContentBy(messageId).map {
      case Some(m) => Ok(m)
      case None    => NotFound
    }
  }

  def getMessagesListBy(messageId: String): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
    twms.findMessagesBy(messageId).map {
      case Left(messages) => Ok(Json.toJson(messages))
      case Right(errors)  => BadRequest(Json.obj("error" -> 400, "message" -> errors))
    }
  }

  def getMessagesListSizeBy(messagesId: String): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
    twms.findMessagesBy(messagesId).map {
      case Left(messages) => Ok(JsNumber(messages.size))
      case Right(errors)  => BadRequest(Json.obj("error" -> 400, "message" -> errors))
    }
  }

  def handleError(): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession =>
      Logger.debug("Request did not have an Active Session, returning Unauthorised - Unauthenticated Error")
      Unauthorized(Json.toJson("Not authenticated"))
    case _: AuthorisationException =>
      Logger.debug("Request has an active session but was not authorised, returning Forbidden - Not Authorised Error")
      Forbidden(Json.toJson("Not authorised"))
    case e: Exception =>
      Logger.error(s"Unknown error: ${e.toString}")
      InternalServerError
  }

  // Validates the customer's message payload and then posts the message
  def validateAndPostMessage(queueId: String, nino: Nino, requestBody: JsValue, name: Name)(
    implicit hc: HeaderCarrier): Future[Result] =
    requestBody.validate[TwoWayMessage] match {
      case _: JsSuccess[_] =>
        Enquiry(queueId) match {
          case Some(enquiryId) =>
            val dmsMetaData =
              DmsMetadata(enquiryId.dmsFormId, nino.nino, enquiryId.classificationType, enquiryId.businessArea)
            twms.post(queueId, nino, requestBody.as[TwoWayMessage], dmsMetaData, name)
          case None =>
            Future.successful(BadRequest(Json.obj("error" -> 400, "message" -> s"Invalid EnquityId: $queueId")))
        }

      case e: JsError => Future.successful(BadRequest(Json.obj("error" -> 400, "message" -> JsError.toJson(e))))
    }

  // Adviser replying to a customer message
  def createAdviserResponse(replyTo: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    {
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
      authorised(AuthProviders(PrivilegedApplication)) {
        validateAndPostAdviserResponse(request.body, replyTo)
      }.recoverWith {
        case _ => Future.successful(Forbidden)
      }
    }
  }

  // Validates the adviser response payload and then posts the reply
  def validateAndPostAdviserResponse(requestBody: JsValue, replyTo: String)(
    implicit hc: HeaderCarrier): Future[Result] =
    requestBody.validate[TwoWayMessageReply] match {
      case _: JsSuccess[_] => twms.postAdviserReply(requestBody.as[TwoWayMessageReply], replyTo)
      case e: JsError      => Future.successful(BadRequest(Json.obj("error" -> 400, "message" -> JsError.toJson(e))))
    }

  // Customer replying to an adviser's message
  // TODO: queueId is redundant, as it's currently fetched from the original message metadata (i.e. postCustomerReply/getMessageMetadata) - you can safely remove
  def createCustomerResponse(queueId: String, replyTo: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
      authorised(Enrolment("HMRC-NI")) {
        validateAndPostCustomerResponse(request.body, replyTo)
      } recover handleError
  }

  // Validates the customer's response payload and then posts the reply
  def validateAndPostCustomerResponse(requestBody: JsValue, replyTo: String)(
    implicit hc: HeaderCarrier): Future[Result] =
    requestBody.validate[TwoWayMessageReply] match {
      case _: JsSuccess[_] => twms.postCustomerReply(requestBody.as[TwoWayMessageReply], replyTo)
      case e: JsError      => Future.successful(BadRequest(Json.obj("error" -> 400, "message" -> JsError.toJson(e))))
    }

  def getCurrentResponseTime(formType: String): Action[AnyContent] = Action.async { implicit request =>
    Enquiry(formType) match {
      case Some(form) => Future.successful(Ok(Json.obj("responseTime" -> form.responseTime)))
      case _          => Future.successful(NotFound)
    }
  }
}
