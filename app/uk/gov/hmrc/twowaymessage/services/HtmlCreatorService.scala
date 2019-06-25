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
import play.twirl.api.Html
import uk.gov.hmrc.twowaymessage.model.ConversationItem

@ImplementedBy(classOf[HtmlCreatorServiceImpl])
trait HtmlCreatorService {
  def createConversation(latestMessageId: String, messages: List[ConversationItem], replyType: ReplyType):Html

}

sealed trait ReplyType

case object Customer extends ReplyType

case object Advisor  extends ReplyType
