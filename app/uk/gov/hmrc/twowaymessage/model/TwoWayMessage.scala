package uk.gov.hmrc.twowaymessage.model

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
