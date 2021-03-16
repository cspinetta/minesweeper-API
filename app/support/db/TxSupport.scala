package support.db

import cats.implicits._
import models.AppError
import play.api.Logging
import scalikejdbc._

import scala.util.control.Exception.ignoring

trait TxSupport extends Logging {

  /**
   * wrap method for executing a block within the same transaction.
   * Provided `thunk` function is responsible to propagate and use on every DB operation the same DBSession supplied.
   *
   * @param thunk : block to be executed within a new transaction
   * @tparam E : error type
   * @tparam A : value type on the happy path
   * @return the same either returned by the thunk block
   */
  def withinTx[E <: AppError, A](thunk: DBSession ⇒ Either[E, A]): Either[E, A] = {
    using(connection()) { db ⇒
      db.localTx(session ⇒ {
        thunk(session)
          .leftMap(error => {
            logger.debug(s"Rollback transaction - cause by ", error)
            session.connection.rollback()
            error
          })
      })
    }
  }

  /**
   * wrap method for executing a block within a read-only session.
   * Provided `thunk` function is responsible to propagate and use on every DB operation the same DBSession supplied.
   *
   * @param thunk : block to be executed within a read-only session
   * @tparam E : error type
   * @tparam A : value type on the happy path
   * @return the same either returned by the thunk block
   */
  def withinReadOnlyTx[E, A](thunk: DBSession ⇒ Either[E, A]): Either[E, A] = {
    using(connection()) { db ⇒
      db.readOnly(session ⇒ {
        thunk(session)
          .leftMap(error => {
            session.connection.rollback()
            error
          })
      })
    }
  }

  private def connection(): DB = {
    try {
      DB(ConnectionPool.borrow())
    } catch {
      case e: Throwable ⇒ logger.error("Error getting connection", e); throw e
    }
  }

  private def using[R <: AutoCloseable, A](resource: R)(f: R => A): A = {
    try {
      f(resource)
    } finally {
      ignoring(classOf[Throwable]) apply {
        resource.close()
      }
    }
  }
}

object TxSupport extends TxSupport
