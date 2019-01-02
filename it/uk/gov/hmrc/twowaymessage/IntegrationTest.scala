package uk.gov.hmrc.twowaymessage

import java.util.UUID

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.integration.ServiceSpec

class IntegrationTest extends WordSpec with Matchers with ServiceSpec  {

  def externalServices: Seq[String] = Seq("datastream", "auth", "message")


  override def additionalConfig: Map[String, _] = Map("auditing.consumer.baseUri.port" -> externalServicePorts("datastream"))


  "Creating a message" should {
    "Should be successful given valid json" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val content = UUID.randomUUID().toString
      val jsonString =
        s"""
           | {
           |   "recipient": { "taxIdentifier" : { "name" : "sautr", "value" : "AB012345A"}, "email": "test@test.com"},
           |   "subject": "subject",
           |   "content": "$content",
           |   "replyTo": "replyTo"
           | }
    """.stripMargin

      val message = Json.parse(jsonString).as[JsObject]
      val response = wsClient.url(resource("/two-way-message/message/customer/0/submit")).post(message).futureValue

      response.status shouldBe 201
    }
  }

  "Should fail given invalid json" in {
    val wsClient = app.injector.instanceOf[WSClient]
    val content = UUID.randomUUID().toString
    val jsonString =
      s"""
         | {
         |   "recipient": { "taxIdentifier" : { "name" : "sautr", "value" : "AB012345A"}, "email": "test@test.com"},
         |   "content": "$content",
         |   "replyTo": "replyTo"
         | }
    """.stripMargin

    val message = Json.parse(jsonString).as[JsObject]
    val response = wsClient.url(resource("/two-way-message/message/customer/0/submit")).post(message).futureValue

    response.status shouldBe 400
  }

}

class IntegrationTestWithoutMessage extends WordSpec with Matchers with ServiceSpec  {

  def externalServices: Seq[String] = Seq("datastream", "auth")


  override def additionalConfig: Map[String, _] = Map("auditing.consumer.baseUri.port" -> externalServicePorts("datastream"))


  "Creating a message" should {
    "Should be successful given valid json" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val content = UUID.randomUUID().toString
      val jsonString =
        s"""
           | {
           |   "recipient": { "taxIdentifier" : { "name" : "sautr", "value" : "AB012345A"}, "email": "test@test.com"},
           |   "subject": "subject",
           |   "content": "$content",
           |   "replyTo": "replyTo"
           | }
    """.stripMargin

      val message = Json.parse(jsonString).as[JsObject]
      val response = wsClient.url(resource("/two-way-message/message/customer/0/submit")).post(message).futureValue

      response.status shouldBe 502
    }
  }

}
