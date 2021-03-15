package services

import conf.AppConfigProvider
import javax.inject.{Inject, Singleton}
import models._
import play.api.Logging
import scalikejdbc.DBSession

@Singleton
class AsciiPrinterService @Inject()(val appConfigProvider: AppConfigProvider,
                                    val gameService: GameService) extends Logging {

  def getAsciiBoard(gameId: Long, showMines: Boolean)(implicit session: DBSession): AppResult[String] = {
    for {
      game <- gameService.findById(gameId)
      printer = if (showMines) printForDebugging _ else printForUser _
    } yield generateBoard(game, printer)
  }

  def generateBoard(game: Game, printer: Cell => String): String = {
    val matrix = Array.ofDim[Cell](game.width, game.height)
    game.cells.foreach(cell => matrix(cell.x - 1)(cell.y - 1) = cell)
    val ascii = new StringBuilder("")
    for {
      (row, i) <- matrix.zipWithIndex
      (cell, j) <- row.zipWithIndex
    } {
      ascii ++= printer(cell)
      if (j == game.height - 1 && i < game.width - 1)
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
  val redFlag: String = " F "
  val questionFlag: String = " ? "
  val mine: String = " * "
}


