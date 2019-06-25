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

import com.google.inject.{ImplementedBy, Inject}
import play.twirl.api.Html
import uk.gov.hmrc.twowaymessage.model.MessageType.MessageType

import scala.concurrent.{ExecutionContext, Future}


@ImplementedBy(classOf[HtmlCreatorServiceImpl])
trait HtmlCreatorService {
  /** Returns either the HTML conversation for the message ID or an error message */
  def getConversation(messageId: String, messageType: MessageType)(implicit ec: ExecutionContext): Future[Either[String,Html]]
}


class HtmlCreatorServiceImpl @Inject()()(implicit ec: ExecutionContext) extends HtmlCreatorService{

  /** Returns either the HTML conversation for the message ID or an error message */
  override def getConversation(messageId: String, messageType: MessageType)(implicit ec: ExecutionContext): Future[Either[String,Html]] = {
    Future.successful[Either[String,Html]]{
     Right(Html("Hello <b>World</b>"))
    }
  }
}