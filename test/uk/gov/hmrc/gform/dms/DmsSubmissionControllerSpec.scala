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

package uk.gov.hmrc.gform.dms

import java.time._
import java.util.{Base64, UUID}

import com.codahale.metrics.SharedMetricRegistries
import com.google.inject.{AbstractModule, TypeLiteral}
import org.apache.pdfbox.pdmodel.PDDocument
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.gform.Spec
import uk.gov.hmrc.gform.fileupload.FileUploadService
import uk.gov.hmrc.gform.pdfgenerator.PdfGeneratorService
import uk.gov.hmrc.gform.sharedmodel.form.EnvelopeId
import uk.gov.hmrc.gform.sharedmodel.formtemplate.destinations.Destinations.DmsSubmission
import uk.gov.hmrc.gform.sharedmodel.formtemplate.{Constant, FormTemplateId, TextExpression}
import uk.gov.hmrc.gform.submission._
import uk.gov.hmrc.gform.typeclasses.Rnd
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.util.Random
// @DoNotDiscover
class DmsSubmissionControllerSpec extends Spec with GuiceOneAppPerSuite  {

  lazy val validSubmission = DmsHtmlSubmission("", DmsMetadata("some-form-id", "some-customer-id", "", ""))

  lazy val validPayload = Json.toJson(validSubmission)
  lazy val validRequest = FakeRequest().withBody[JsValue](validPayload)

  lazy val mockFileUpload = mock[FileUploadService]
  lazy val mockPdfGenerator = mock[PdfGeneratorService]
  lazy val mockDocumentLoader = mockFunction[Array[Byte], PDDocument]

  lazy val mockDocumentLoaderFunction = mock[Function1[Array[Byte], PDDocument]]
  lazy val stubPdfDocument = stub[PDDocument]
  private class TestGuiceModule extends AbstractModule {
    override def configure(): Unit = {
      bind(new TypeLiteral[Rnd[Random]] {}).toInstance(fixedRnd)
      bind(new TypeLiteral[Function1[Array[Byte], PDDocument]] {}).toInstance(mockDocumentLoader)
    }
  }

  lazy val fixedRnd = new Rnd[Random] {
    val notVeryRandom = stub[Random]
      (notVeryRandom.nextInt(_: Int)).when(*).returning(4)

    override def apply() = notVeryRandom
  }
  lazy val fixedTime = LocalDateTime.of(2018, 3, 2, 0, 0)
  lazy val clock = Clock.fixed(fixedTime.toInstant(ZoneOffset.UTC), ZoneId.systemDefault)

  val injector = new GuiceApplicationBuilder()
    .overrides(bind[FileUploadService].to(mockFileUpload))
    .overrides(bind[PdfGeneratorService].to(mockPdfGenerator))
    .overrides(bind[Clock].to(clock))
    .overrides(new TestGuiceModule())
    .injector()

    SharedMetricRegistries.clear()
  val testController = injector.instanceOf[DmsSubmissionController]
  "The DmsSubmissionController" should "create a file upload envelope" in {
    (mockFileUpload
      .createEnvelope(_: FormTemplateId)(_: HeaderCarrier))
      .expects(FormTemplateId(validSubmission.metadata.dmsFormId), *)
      .returning(Future.successful(EnvelopeId("some-envelope-id")))

    (mockPdfGenerator
      .generatePDF(_: String)(_: HeaderCarrier))
      .expects(*, *)
      .returning(Future.successful(Array.emptyByteArray))

    mockDocumentLoader
      .expects(*)
      .returning(stubPdfDocument)

    (mockFileUpload
      .submitEnvelope(_: Submission, _: PdfAndXmlSummaries, _: DmsSubmission, _: Int)(_: HeaderCarrier))
      .expects(*, *, *, *, *)
      .returning(Future.successful(()))

    val res = testController.submitToDms()(validRequest)
    status(res) shouldBe NO_CONTENT
    SharedMetricRegistries.clear()
  }

  it should "generate a PDF using the decoded HTML" in {
    val html = "<p>submission</p>"
    val encodedHtml = new String(Base64.getEncoder.encode(html.getBytes))
    val submissionWithHtml = validSubmission.copy(html = encodedHtml)
    val pdfBytes = "this is totally a pdf".getBytes

    (mockFileUpload
      .createEnvelope(_: FormTemplateId)(_: HeaderCarrier))
      .expects(*, *)
      .returning(Future.successful(EnvelopeId("some-envelope-id")))

    (mockPdfGenerator
      .generatePDF(_: String)(_: HeaderCarrier))
      .expects(html, *)
      .returning(Future.successful(pdfBytes))

    mockDocumentLoader
      .expects(*)
      .returning(stubPdfDocument)

    (mockFileUpload
      .submitEnvelope(_: Submission, _: PdfAndXmlSummaries, _: DmsSubmission, _: Int)(_: HeaderCarrier))
      .expects(*, *, *, *, *)
      .returning(Future.successful(()))

    val res = testController.submitToDms()(FakeRequest().withBody[JsValue](Json.toJson(submissionWithHtml)))
    status(res) shouldBe NO_CONTENT

    SharedMetricRegistries.clear()
  }

  it should "upload the PDF and XML metadata to the file upload envelope" in {
    val submissionRef = SubmissionRef.random(fixedRnd)

    val numberOfPages = 1
    val pdfContent = "totally a pdf".getBytes
    val expectedEnvId = EnvelopeId(UUID.randomUUID().toString)
    val expectedPdfAndXmlSummaries = PdfAndXmlSummaries(PdfSummary(numberOfPages.longValue, pdfContent))

    val expectedDmsSubmission = DmsSubmission(
      validSubmission.metadata.dmsFormId,
      TextExpression(Constant(validSubmission.metadata.customerId)),
      validSubmission.metadata.classificationType,
      validSubmission.metadata.businessArea
    )

    (mockFileUpload
      .createEnvelope(_: FormTemplateId)(_: HeaderCarrier))
      .expects(*, *)
      .returning(Future.successful(expectedEnvId))

    (mockPdfGenerator
      .generatePDF(_: String)(_: HeaderCarrier))
      .expects(*, *)
      .returning(Future.successful(pdfContent))

    mockDocumentLoader
      .expects(*)
      .returning(stubPdfDocument)

    (stubPdfDocument.getNumberOfPages _).when().returning(numberOfPages)

    (mockFileUpload
      .submitEnvelope(_: Submission, _: PdfAndXmlSummaries, _: DmsSubmission, _: Int)(_: HeaderCarrier))
      .expects(*, expectedPdfAndXmlSummaries, expectedDmsSubmission, *, *)
      .returning(Future.successful(()))

    val res = testController.submitToDms()(validRequest)
    status(res) shouldBe NO_CONTENT
    SharedMetricRegistries.clear()
  }

  it should "return a 400 Bad Request response when the JSON payload is invalid" in {
    val res = testController.submitToDms()(FakeRequest().withBody[JsValue](Json.obj()))
    status(res) shouldBe BAD_REQUEST
    SharedMetricRegistries.clear()
  }

}
