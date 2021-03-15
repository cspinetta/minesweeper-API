package models


sealed trait AppError extends RuntimeException {
  val reason: String

}

case class UnexpectedError(reason: String) extends AppError

case class ResourceNotFound(reason: String) extends AppError

case class DataSourceError(reason: String, cause: Option[Throwable] = None) extends AppError

case class InvalidParametersError(reason: String) extends AppError

case class NotUniqueError(reason: String) extends AppError

case class InvalidStateTransitionError(reason: String) extends AppError

/**
 * Model representation for a HTTP client error
 *
 * @param reason brief error description
 */
case class ClientError(reason: String) extends AppError
