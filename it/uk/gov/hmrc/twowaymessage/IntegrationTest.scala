package uk.gov.hmrc.twowaymessage

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.google.common.io.BaseEncoding
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsObject, Json, Reads}
import play.api.libs.ws.ahc.{AhcWSClient, AhcWSClientConfig, StandaloneAhcWSClient}
import uk.gov.hmrc.integration.ServiceSpec

import scala.concurrent.duration.{Duration, FiniteDuration}

class IntegrationTest extends WordSpec with Matchers with ServiceSpec  {

  def externalServices: Seq[String] = Seq("datastream", "auth-login-api")

  implicit val defaultTimeout: FiniteDuration = Duration(15, TimeUnit.SECONDS)

  implicit val system = ActorSystem()
  implicit val materializer = akka.stream.ActorMaterializer()
  def httpClient = new AhcWSClient(StandaloneAhcWSClient(AhcWSClientConfig()))

  override def additionalConfig: Map[String, _] = Map("auditing.consumer.baseUri.port" -> externalServicePorts("datastream"))

  "User creating a message" should {

    "be successful given valid json" in {
      val message = MessageUtil.buildValidCustomerMessage()

      val response = httpClient.url(resource("/two-way-message/message/customer/0/submit"))
        .withHeaders(AuthUtil.buildNinoUserToken())
        .post(message)
        .futureValue

      response.status shouldBe 201
    }

    "fail given invalid json" in {
      val message = MessageUtil.buildInvalidCustomerMessage

      val response = httpClient
       .url(resource("/two-way-message/message/customer/0/submit"))
       .withHeaders(AuthUtil.buildNinoUserToken())
       .post(message)
       .futureValue

      response.status shouldBe 400
    }

    "return Forbidden (403) when valid bearer token with SaUtr credentials and valid JSON payload but no Nino" in {
      val message = MessageUtil.buildValidReplyMessage()

      val response = httpClient.url(resource("/two-way-message/message/customer/0/submit"))
        .withHeaders(AuthUtil.buildSaUserToken())
        .post(message).futureValue

      response.status shouldBe 403
    }
  }

  "Advisor responding" should {

    "Forbidden when no access token" in {
      val message = MessageUtil.buildValidReplyMessage()
      val validMessageId = MessageUtil.getValidNinoMessageId()

      val response = httpClient
        .url(resource(s"/two-way-message/message/advisor/$validMessageId/reply"))
        .post(message)
        .futureValue

      response.status shouldBe 403
    }

    "Forbidden when using user access token" in {
      val message = MessageUtil.buildValidReplyMessage()
      val validMessageId = MessageUtil.getValidNinoMessageId()

      val response = httpClient
        .url(resource(s"/two-way-message/message/advisor/$validMessageId/reply"))
        .withHttpHeaders(AuthUtil.buildNinoUserToken())
        .post(message)
        .futureValue

      response.status shouldBe 403
    }

    "Access when access token" in {
      val message = MessageUtil.buildValidReplyMessage()
      val validMessageId = MessageUtil.getValidNinoMessageId()

      val response = httpClient
        .url(resource(s"/two-way-message/message/advisor/$validMessageId/reply"))
        .withHeaders(AuthUtil.buildStrideToken())
        .post(message)
        .futureValue

      response.status shouldBe 201
    }
  }

  "User responding to an advisor's message" should {

    "return Created (201) when valid bearer token with Nino credentials and valid JSON payload" in {
      val message = MessageUtil.buildValidReplyMessage()
      val replyToMessageId = MessageUtil.getValidNinoMessageId()

      val response = httpClient
        .url(resource(s"/two-way-message/message/customer/0/$replyToMessageId/reply"))
        .withHttpHeaders(AuthUtil.buildNinoUserToken())
        .post(message)
        .futureValue

      response.status shouldBe 201
    }

    "return Unauthorized (401) when missing a valid bearer token" in {
      val message = MessageUtil.buildValidReplyMessage()
      val replyToMessageId = MessageUtil.getValidNinoMessageId()

      val response = httpClient
        .url(resource(s"/two-way-message/message/customer/0/$replyToMessageId/reply"))
        .post(message)
        .futureValue

      response.status shouldBe 401
    }

    "return Bad Request (400) when providing an invalid payload" in {
      val message = MessageUtil.buildInvalidReplyMessage()
      val replyToMessageId = MessageUtil.getValidNinoMessageId()

      val response = httpClient
        .url(resource(s"/two-way-message/message/customer/0/$replyToMessageId/reply"))
        .withHttpHeaders(AuthUtil.buildNinoUserToken())
        .post(message)
        .futureValue

      response.status shouldBe 400
    }
  }

  object AuthUtil {
    lazy val authPort = 8500
    lazy val ggAuthPort =  externalServicePorts.get("auth-login-api").get

    implicit val deserialiser: Reads[GatewayToken] = Json.reads[GatewayToken]

    case class GatewayToken(val gatewayToken: String)

    private val STRIDE_USER_PAYLOAD =
      """
        | {
        |  "clientId" : "id",
        |  "enrolments" : [],
        |  "ttl": 1200
        | }
      """.stripMargin

    private val GG_NINO_USER_PAYLOAD =
      """
        | {
        |  "credId": "1234",
        |  "affinityGroup": "Organisation",
        |  "confidenceLevel": 100,
        |  "credentialStrength": "none",
        |  "nino": "AA000108C",
        |  "enrolments": []
        |  }
     """.stripMargin

    private val GG_SA_USER_PAYLOAD =
      """
        | {
        |  "credId": "1235",
        |  "affinityGroup": "Organisation",
        |  "confidenceLevel": 100,
        |  "credentialStrength": "none",
        |  "enrolments": [
        |      {
        |        "key": "IR-SA",
        |        "identifiers": [
        |          {
        |            "key": "UTR",
        |            "value": "1234567890"
        |          }
        |        ],
        |        "state": "Activated"
        |      }
        |    ]
        |  }
     """.stripMargin

    private def buildUserToken(payload: String): (String, String) = {
      val response = httpClient.url(s"http://localhost:$ggAuthPort/government-gateway/session/login")
        .withHttpHeaders(("Content-Type", "application/json"))
        .post(payload)
        .futureValue

      ("Authorization", response.header("Authorization").get)
    }

    def buildNinoUserToken(): (String, String) = buildUserToken(GG_NINO_USER_PAYLOAD)

    def buildSaUserToken(): (String, String) = buildUserToken(GG_SA_USER_PAYLOAD)

    def buildStrideToken(): (String, String) = {
      val response = httpClient.url(s"http://localhost:$authPort/auth/sessions")
        .withHttpHeaders(("Content-Type", "application/json"))
        .post(STRIDE_USER_PAYLOAD).futureValue

      ("Authorization", response.header("Authorization").get)
    }

  }

  object MessageUtil {
    import play.api.libs.json.{Json, Reads}
    import scala.util.Random

    implicit val deserialiser: Reads[MessageId] = Json.reads[MessageId]
    def generateContent(): String = BaseEncoding.base64().encode(s"Hello world! - ${Random.nextString(20)}".getBytes())

    case class MessageId(val id: String)

    def buildValidCustomerMessage(): JsObject = {
      val jsonString =
        s"""
           | {
           |   "contactDetails":{
           |      "email": "someEmail@test.com"
           |   },
           |   "subject": "subject",
           |   "content": "$generateContent",
           |   "replyTo": "replyTo"
           | }
      """.stripMargin

      Json.parse(jsonString).as[JsObject]
    }

    def buildInvalidCustomerMessage: JsObject = {
      val jsonString =
        s"""
           | {
           |   "email": "test@test.com",
           |   "content": "$generateContent",
           |   "replyTo": "replyTo"
           | }
    """.stripMargin

      Json.parse(jsonString).as[JsObject]
    }

    def buildValidReplyMessage(): JsObject = {
      val jsonString =
        s"""
           | {
           |   "content": "$generateContent"
           | }
      """.stripMargin
      Json.parse(jsonString).as[JsObject]
    }

    def buildInvalidReplyMessage(): JsObject = {
      val jsonString =
        s"""
           | {
           |   "c": "$generateContent"
           | }
      """.stripMargin
      Json.parse(jsonString).as[JsObject]
    }

    def getValidNinoMessageId(): String = {
      val message = buildValidCustomerMessage()
      val response = httpClient.url(resource("/two-way-message/message/customer/0/submit"))
        .withHeaders(AuthUtil.buildNinoUserToken())
        .post(message).futureValue
      Json.parse(response.body).as[MessageId].id
    }
  }
}

