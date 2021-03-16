package services

import conf.AppConfigProvider
import javax.inject.{Inject, Singleton}
import models._
import play.api.Logging
import scalikejdbc.DBSession

@Singleton
class AsciiPrinterService @Inject()(val appConfigProvider: AppConfigProvider,
                                    val gameService: GameService) extends Logging {

  def getBoardInAscii(gameId: Long, showMines: Boolean)(implicit session: DBSession): AppResult[String] = {
    for {
      game <- gameService.findById(gameId)
      printer = if (showMines) printForDebugging _ else printForUser _
    } yield generateBoard(game, printer)
  }

  def generateBoard(game: Game, printer: Cell => String): String = {
    val matrix = Array.ofDim[Cell](game.width, game.height)
    game.cells.foreach(cell => matrix(cell.y - 1)(cell.x - 1) = cell)
    val ascii = new StringBuilder("")
    for {
      (row, rowIndex) <- matrix.zipWithIndex
      (cell, collIndex) <- row.zipWithIndex
    } {
      ascii ++= printer(cell)
      if (collIndex == game.height - 1 && rowIndex < game.width - 1)
        ascii ++= System.lineSeparator
    }
    ascii.toString()
  }

  def printForUser(cell: Cell): String = cell.state match {
    case CellState.Covered => AsciiSymbols.covered
    case CellState.Uncovered => AsciiSymbols.uncovered(cell.adjacentMines)
    case CellState.RedFlag => AsciiSymbols.redFlag
    case CellState.QuestionFlag => AsciiSymbols.questionFlag
  }

  def printForDebugging(cell: Cell): String = {
    if (cell.hasMine)
      AsciiSymbols.mine
    else {
      printForUser(cell)
    }
  }
}

object AsciiSymbols {
  def uncovered(adjacentMines: Int): String = s" $adjacentMines "

  val covered: String = " - "
  val redFlag: String = s" ${Character.toString(9873)} " // ⚑
  val questionFlag: String = " ? "
  val mine: String = s" ${Character.toString(128163)} " // 💣

  /* s must be an even-length string. *//* s must be an even-length string. */
  def hexStringToByteArray(s: String): Array[Byte] = {
    val len = s.length
    val data = new Array[Byte](len / 2)
    var i = 0
    while ( {
      i < len
    }) {
      data(i / 2) = ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16)).toByte

      i += 2
    }
    data
  }
}


