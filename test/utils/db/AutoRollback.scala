package utils.db

import org.scalatest.{Outcome, fixture}
import scalikejdbc.{ConnectionPool, DB, DBSession, LoanPattern, SettingsProvider}

/**
 * AutoRollback compatible with ScalaTest 3.x
 */
trait AutoRollback extends LoanPattern { self: fixture.TestSuite =>

  type FixtureParam = DBSession

  protected[this] def settingsProvider: SettingsProvider =
    SettingsProvider.default

  /**
   * Creates a [[scalikejdbc.DB]] instance.
   * @return DB instance
   */
  def db(): DB =
    DB(conn = ConnectionPool.borrow(), settingsProvider = settingsProvider)

  /**
   * Prepares database for the test.
   * @param session db session implicitly
   */
  def fixture(implicit session: DBSession): Unit = {}

  /**
   * Provides transactional block
   * @param test one arg test
   */
  override def withFixture(test: OneArgTest): Outcome = {
    using(db()) { db =>
      try {
        db.begin()
        db.withinTx { implicit session =>
          fixture(session)
        }
        withFixture(test.toNoArgTest(db.withinTxSession()))
      } finally {
        db.rollbackIfActive()
      }
    }
  }
}
