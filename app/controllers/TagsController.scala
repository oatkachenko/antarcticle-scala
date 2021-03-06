package controllers

import play.api.mvc.{Action, Controller}
import play.api.libs.json.Json
import services.TagsServiceComponent

/**
 * Provides client with tag-related information
 */
trait TagsController  {
  this: Controller with TagsServiceComponent =>

  def listTags() = Action {
    implicit request =>
      Ok(Json.toJson(tagsService.listDistinctTags()))
  }
}
