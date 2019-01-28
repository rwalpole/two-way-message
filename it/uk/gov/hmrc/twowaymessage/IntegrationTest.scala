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

  def externalServices: Seq[String] = Seq("datastream", "message", "auth-login-api")

  implicit val defaultTimeout: FiniteDuration = Duration(15, TimeUnit.SECONDS)

  implicit val system = ActorSystem()
  implicit val materializer = akka.stream.ActorMaterializer()
  def httpClient = new AhcWSClient(StandaloneAhcWSClient(AhcWSClientConfig()))

  override def additionalConfig: Map[String, _] = Map("auditing.consumer.baseUri.port" -> externalServicePorts("datastream"))

  "User creating a message" should {
    "be successful given valid json" in {
      val message = MessageUtil.buildValidCustomerMessage()

      val response = httpClient.url(resource("/two-way-message/message/customer/0/submit"))
        .withHeaders(AuthUtil.buildUserToken())
        .post(message)
        .futureValue

      response.status shouldBe 201
    }

   "fail given invalid json" in {
     val message = MessageUtil.buildInvalidCustomerMessage

     val response = httpClient
       .url(resource("/two-way-message/message/customer/0/submit"))
       .withHeaders(AuthUtil.buildUserToken())
       .post(message)
       .futureValue

     response.status shouldBe 400
   }
 }

 "Advisor responding" should {
   "Forbidden when no access token" in {
     val message = MessageUtil.buildValidAdviserResponse()
     val validMessageId = MessageUtil.getValidMessageId()

     val response = httpClient
       .url(resource(s"/two-way-message/message/advisor/$validMessageId/reply"))
       .post(message)
       .futureValue

     response.status shouldBe 403
   }

   "Forbidden when using user access token" in {
     val message = MessageUtil.buildValidAdviserResponse()
     val validMessageId = MessageUtil.getValidMessageId()

     val response = httpClient.url(resource(s"/two-way-message/message/advisor/$validMessageId/reply"))
       .withHttpHeaders(AuthUtil.buildUserToken())
       .post(message)
       .futureValue

     response.status shouldBe 403
   }

 "Access when access token" in {
   val message = MessageUtil.buildValidAdviserResponse()
   val validMessageId = MessageUtil.getValidMessageId()

   val response = httpClient.url(resource(s"/two-way-message/message/advisor/$validMessageId/reply"))
     .withHeaders(AuthUtil.buildStrideToken())
     .post(message)
     .futureValue

    response.status shouldBe 201
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

    private val GG_USER_PAYLOAD =
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

    def buildUserToken(): (String, String) = {
      val response = httpClient.url(s"http://localhost:$ggAuthPort/government-gateway/session/login")
        .withHttpHeaders(("Content-Type", "application/json"))
        .post(GG_USER_PAYLOAD).futureValue

      ("Authorization", response.header("Authorization").get)
    }

    def buildStrideToken(): (String, String) = {
      val response = httpClient.url(s"http://localhost:$authPort/auth/sessions")
        .withHttpHeaders(("Content-Type", "application/json"))
        .post(STRIDE_USER_PAYLOAD).futureValue

      ("Authorization", response.header("Authorization").get)
    }

  }

  object MessageUtil {
    import play.api.libs.json.{Json, Reads}

    implicit val deserialiser: Reads[MessageId] = Json.reads[MessageId]
    def generateContent() = BaseEncoding.base64().encode(s"Hello world! - ${System.currentTimeMillis()}".getBytes())

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

    def buildValidAdviserResponse(): JsObject = {
      val jsonString =
        s"""
           | {
           |   "content": "$generateContent"
           | }
      """.stripMargin
      Json.parse(jsonString).as[JsObject]
    }

    def getValidMessageId(): String = {
      val message = buildValidCustomerMessage()
      val response = httpClient.url(resource("/two-way-message/message/customer/0/submit")).withHeaders(AuthUtil.buildUserToken()).post(message).futureValue
      Json.parse(response.body).as[MessageId].id
    }
  }
}

