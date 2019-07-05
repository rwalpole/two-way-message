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

import javax.inject.Inject
import javax.swing.text.html.HTML
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.twowaymessage.model.{ConversationItem, MessageType}

import scala.concurrent.ExecutionContext
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.XML

class HtmlCreatorServiceImpl @Inject()()
                                      (implicit ec: ExecutionContext) extends HtmlCreatorService {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def createConversation(latestMessageId: String, messages: List[ConversationItem], replyType: RenderType.ReplyType)
                                 (implicit ec: ExecutionContext):Future[Either[String,Html]] = {

    Future.successful(Right(HtmlFormat.fill(createConversationList(messages,replyType))))
  }

  override def createSingleMessageHtml(conversationItem: ConversationItem)(implicit ec: ExecutionContext): Future[Either[String,Html]] = {
    Future.successful(Right(format2wsMessageForCustomer(conversationItem,true)))
  }

  override def sortConversation(latestMessageId: String, messages: List[ConversationItem]): List[ConversationItem] = {

    def sortConversation(messageId: String, messages: List[ConversationItem], orderedMsgList: List[ConversationItem]): List[ConversationItem] = {
      val message = messages.find(message => message.id == messageId).get
      message.body.flatMap(_.replyTo) match {
        case None => orderedMsgList :+ message
        case Some(replyToId) => sortConversation(replyToId, messages, orderedMsgList :+ message)
      }
    }
    sortConversation(latestMessageId, messages, List())
  }

  private def createConversationList(messages: List[ConversationItem], replyType: RenderType.ReplyType):List[Html] = {
    if(replyType == RenderType.Customer) {
      val latestMessage = format2wsMessageForCustomer(messages.head, true)
      val restOfList = for {
        msg <- messages.tail
      } yield format2wsMessageForCustomer(msg, false)
      val listOfMessage = restOfList :+ latestMessage
      listOfMessage.reverse
    } else {
      for {
        msg <- messages
      } yield format2wsMessageForAdviser(msg)
    }
  }

    private def format2wsMessageForAdviser(conversationItem: ConversationItem): Html = {
        val message =
        <p class="message_time faded-text--small">
          {getAdviserDatesText(conversationItem)}
        </p>
        <p>{val content = conversationItem.content.getOrElse("")
        XML.loadString("<root>" + content.replaceAllLiterally("<br>","<br/>") + "</root>").child}</p><hr/>
      Html.apply(message.mkString)
    }

    private def format2wsMessageForCustomer(conversationItem: ConversationItem, isLatestMessage: Boolean): Html = {
      val headingClass = "govuk-heading-xl margin-top-small margin-bottom-small"
      val header = if (isLatestMessage) {
        <h1 class={headingClass}>
          {conversationItem.subject}
        </h1>
      } else {
        <h2 class={headingClass}>
          {conversationItem.subject}
        </h2>
      }
      val replyForm = if (isLatestMessage) {
        val enquiryType = conversationItem.body.flatMap {
          _.enquiryType
        }.getOrElse("")
        val formActionUrl = s"/two-way-message-frontend/message/customer/$enquiryType/" + conversationItem.id + "/reply"
        conversationItem.body.map(_.`type`) match {
          case Some(msgType) => msgType match {
            case MessageType.Adviser => s"""<a href="$formActionUrl#reply-input-label">Send another message about this</a>"""
            case _ => ""
          }
        }
      } else {
        ""
      }
      val xml = header ++ <p class="message_time faded-text--small">
        {getCustomerDateText(conversationItem)}
      </p>
        <p>
          {
          val content = conversationItem.content.getOrElse("")
          XML.loadString("<root>" + content.replaceAllLiterally("<br>","<br/>") + "</root>").child
          }
        </p> ++ replyForm ++ <hr/>


      Html.apply(xml.mkString)

    }

    private def getCustomerDateText(message: ConversationItem): String = {
      val messageDate = extractMessageDate(message)
      message.body match {
        case Some(conversationItemDetails) => conversationItemDetails.`type` match {
          case MessageType.Customer => s"You sent this message on $messageDate"
          case MessageType.Adviser => s"This message was sent to you on $messageDate"
          case _ => defaultDateText(messageDate)
        }
        case _ => defaultDateText(messageDate)
      }
    }

  def getAdviserDatesText(message: ConversationItem): String = {
    val messageDate = extractMessageDate(message)
    message.body match {
      case Some(conversationItemDetails) => conversationItemDetails.`type` match {
        case MessageType.Adviser => s"$messageDate by HMRC"
        case MessageType.Customer=> s"$messageDate by the customer"
        case _ => defaultDateText(messageDate)
      }
      case _ => defaultDateText(messageDate)
    }
  }

    private def defaultDateText(dateStr: String) = s"This message was sent on $dateStr"


    private def extractMessageDate(message: ConversationItem): String = {
      message.body.flatMap(_.issueDate) match {
        case Some(issueDate) => formatter(issueDate)
        case None => formatter(message.validFrom)
      }
    }

    val dateFormatter = DateTimeFormat.forPattern("dd MMMM yyyy")

    private def formatter(date: LocalDate): String = date.toString(dateFormatter)

}
