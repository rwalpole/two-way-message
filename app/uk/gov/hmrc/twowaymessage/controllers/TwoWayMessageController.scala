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

package uk.gov.hmrc.twowaymessage.controllers
import com.google.inject.Inject
import play.api.libs.json.{JsValue, Json, Reads}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.controller.{BackendBaseController, BackendController, BaseController}
import uk.gov.hmrc.twowaymessage.model.TwoWayMessage
import scala.concurrent.ExecutionContext.Implicits.global._
import scala.concurrent.{ExecutionContext, Future}

class TwoWayMessageController @Inject()(cc: ControllerComponents, executionContext: ExecutionContext) extends BackendController(cc){
  def createMessage(queueId: String):Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      withJsonBody[TwoWayMessage] { twoWayMessage => {
        Future {
          Created(Json.obj("id" -> "57bac7e90b0000490000b7cf"))
        }
      }

      }
  }

  override protected def withJsonBody[T](f: T => Future[Result])(implicit request: Request[JsValue], m: Manifest[T], reads: Reads[T]): Future[Result] = {
  ???
  }
}
