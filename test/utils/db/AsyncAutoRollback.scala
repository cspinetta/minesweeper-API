package utils.db

import org.scalatest.{FutureOutcome, fixture}
import scalikejdbc.{ConnectionPool, DB, DBSession, LoanPattern, SettingsProvider}

/**
 * AsyncAutoRollback compatible with ScalaTest 3.x
 */
trait AsyncAutoRollback extends LoanPattern {
  self: fixture.AsyncTestSuite =>

  type FixtureParam = DBSession

  protected[this] def settingsProvider: SettingsProvider =
    SettingsProvider.default

  /**
   * Creates a [[scalikejdbc.DB]] instance.
   *
   * @return DB instance
   */
  def db(): DB =
    DB(conn = ConnectionPool.borrow(), settingsProvider = settingsProvider)

  /**
   * Prepares database for the test.
   *
   * @param session db session implicitly
   */
  def fixture(implicit session: DBSession): Unit = {}

  /**
   * Provides transactional block
   *
   * @param test one arg test
   */
  override def withFixture(test: OneArgAsyncTest): FutureOutcome = {
    val database = db()
    database.begin()
    database.withinTx { implicit session =>
      fixture(session)
    }
    withFixture(test.toNoArgAsyncTest(database.withinTxSession())).onCompletedThen { _ =>
      using(database) { d =>
        d.rollbackIfActive()
      }
    }
  }

}
