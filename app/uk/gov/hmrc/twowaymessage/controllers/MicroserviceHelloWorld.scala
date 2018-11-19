package uk.gov.hmrc.twowaymessage.controllers

import javax.inject.Singleton
import uk.gov.hmrc.play.bootstrap.controller.{BackendController, BaseController}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import play.api.mvc._

import scala.concurrent.Future

@Singleton()
class MicroserviceHelloWorld extends BackendController {

	def hello() = Action.async { implicit request =>
		Future.successful(Ok("Hello world"))
	}

}