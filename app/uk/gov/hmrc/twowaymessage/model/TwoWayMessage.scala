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

package uk.gov.hmrc.twowaymessage.model

import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID

case class TwoWayMessage(recipient: Recipient,
                         subject: String,
                         content: Option[String] = None
                        ) {

  //{
  // "recipient":{
  //   "taxIdentifier":{
  //     "name":"HMRC_ID",
  //     "value":"AB123456C"
  //   },
  //   "email":"someEmail@test.com"
  // },
  // "subject":"QUESTION",
  // "content":"Some base64-encoded HTML",
  //}
}
