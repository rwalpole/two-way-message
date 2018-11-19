package uk.gov.hmrc.twowaymessage.controllers
import play.api.libs.json.{JsValue, Json, Reads}
import play.api.mvc.{Action, Request, Result}
import uk.gov.hmrc.play.bootstrap.controller.{BackendBaseController, BackendController, BaseController}
import uk.gov.hmrc.twowaymessage.model.TwoWayMessage

import scala.concurrent.Future

class TwoWayMessageController extends BackendController{
  def createMessage():Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      withJsonBody[TwoWayMessage] { twoWayMessage => {
        Future(Created(Json.obj("id" -> "57bac7e90b0000490000b7cf")))
      }

      }
  }

  override protected def withJsonBody[T](f: T => Future[Result])(implicit request: Request[JsValue], m: Manifest[T], reads: Reads[T]): Future[Result] = {

  }
}
