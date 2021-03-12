package controllers

import play.api.mvc.BaseController
import support.json.{JsonSupport, PlayJsonExtension}

abstract class ApiController extends BaseController with JsonSupport with PlayJsonExtension
