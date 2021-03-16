package services

import conf.AppConfigProvider
import javax.inject.{Inject, Singleton}
import models._
import play.api.Logging

@Singleton
class AsciiPrinterService @Inject()(val appConfigProvider: AppConfigProvider) extends Logging {

  def getBoardInAscii(game: Game, showMines: Boolean): String = {
    game.state match {
      case GameState.Created =>
        generateBoardInUninitializedGame(game)
      case _ =>
        val printer = if (showMines) printForDebugging _ else printForUser _
        generateBoardInInitializedGame(game, printer)
    }
  }

  def generateBoardInUninitializedGame(game: Game): String = {
    val matrix = Array.ofDim[Cell](game.width, game.height)
    val ascii = new StringBuilder("")
    for {
      (row, rowIndex) <- matrix.zipWithIndex
      (_, collIndex) <- row.zipWithIndex
    } {
      ascii ++= AsciiSymbols.covered
      if (collIndex == game.height - 1 && rowIndex < game.width - 1)
        ascii ++= System.lineSeparator
    }
    ascii.toString()
  }

  def generateBoardInInitializedGame(game: Game, printer: Cell => String): String = {
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
  val mine: String = s" ${Character.toString(128163)}" // 💣
}


