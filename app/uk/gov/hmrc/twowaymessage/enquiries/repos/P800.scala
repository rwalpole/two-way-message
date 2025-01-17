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

package uk.gov.hmrc.twowaymessage.enquiries.repos

import play.api.Play
import uk.gov.hmrc.twowaymessage.enquiries.Enquiry.EnquiryTemplate

object P800 extends EnquiryTemplate {

  val title: String = "P800"

  val dmsFormId:String = "P800"
  val classificationType: String = "PSA-DFS Secure Messaging SA"
  val businessArea: String = "PT Operations"
  lazy val responseTime: String = Play.current.configuration.getString("forms.p800.reponseTime").get


}
