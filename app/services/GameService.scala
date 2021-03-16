package services

import java.time.ZonedDateTime

import cats.implicits._
import conf.AppConfigProvider
import javax.inject.{Inject, Singleton}
import models.GameActions._
import models._
import play.api.Logging
import repositories.{CellRepository, GameRepository}
import scalikejdbc.DBSession

import scala.annotation.tailrec

@Singleton
class GameService @Inject()(val appConfigProvider: AppConfigProvider,
                            val playerService: PlayerService,
                            val gameRepository: GameRepository,
                            val cellRepository: CellRepository) extends Logging {

  import GameService._

  def create(userId: Long, cmd: GameCreationCommand)(implicit session: DBSession): AppResult[Game] = {
    for {
      _ <- validateGameCreationCommand(cmd)
      game <- gameRepository.create(userId, cmd, GameState.Created)
    } yield game
  }

  def validateGameByUser(id: Long, userId: Long)(implicit session: DBSession): AppResult[Game] = {
    gameRepository.findByIdAndPlayer(id, userId)
  }

  def findById(id: Long)(implicit session: DBSession): AppResult[Game] = {
    gameRepository.findById(id)
  }

  def updateGameState(id: Long, newGameState: GameState)(implicit session: DBSession): AppResult[Game] = {
    for {
      game <- gameRepository.findById(id)
      newState <- game.state.transition(newGameState)
      _ <- gameRepository.save(game.copy(state = newState))
      updatedGame <- gameRepository.findById(game.id)
    } yield updatedGame
  }

  def updateCellState(game: Game, cmd: SetCellStateCommand)(implicit session: DBSession): AppResult[Game] = {
    cmd.action match {
      case CellAction.Reveal => revealCell(game, cmd.position)
      case CellAction.SetRedFlag => updateFlag(game, cmd, CellState.RedFlag)
      case CellAction.SetQuestionFlag => updateFlag(game, cmd, CellState.QuestionFlag)
      case CellAction.Clean => updateFlag(game, cmd, CellState.Covered)
    }
  }

  def revealCell(initialGame: Game, position: Position)(implicit session: DBSession): AppResult[Game] = {
    for {
      startedGame <- checkGameState(initialGame, position)
      cell <- validateCellPosition(startedGame, position)
      _ <- cell.state.transition(CellState.Uncovered)
      boardWalker = doReveal(startedGame, position)
      _ <- boardWalker.updatedCells.toList.traverse(cell => cellRepository.save(cell))
      _ <- updateGame(boardWalker)
      updatedGame <- gameRepository.findById(initialGame.id)
    } yield updatedGame
  }

  def updateFlag(initialGame: Game, cmd: SetCellStateCommand, newCellState: CellState)(implicit session: DBSession): AppResult[Game] = {
    for {
      startedGame <- checkGameState(initialGame, cmd.position)
      cell <- validateCellPosition(startedGame, cmd.position)
      _ <- cell.state.transition(newCellState)
      _ <- cellRepository.save(cell.copy(state = newCellState))
      updatedGame <- gameRepository.findById(initialGame.id)
    } yield updatedGame
  }

  private def validateGameCreationCommand(cmd: GameCreationCommand): AppResult[Unit] = {
    if (cmd.height > appConfigProvider.app.game.maxHeight || cmd.width > appConfigProvider.app.game.maxWidth)
      Left(InvalidParametersError(s"Invalid game creation parameters. Max height: " +
        s"${appConfigProvider.app.game.maxHeight} and Max width: ${appConfigProvider.app.game.maxHeight}"))
    else if (cmd.mines > (cmd.height * cmd.width - 1))
      Left(InvalidParametersError("Invalid game creation parameters. Total mines can not be greater than or equal to the total cells"))
    else
      Right(())
  }

  private def updateGame(boardWalker: BoardWalker)(implicit session: DBSession): AppResult[Unit] = {
    boardWalker.gameProgress match {
      case GameProgressValues.GameLostAndOver =>
        val updatedGame = boardWalker.game.copy(state = GameState.Lost)
        gameRepository.save(updatedGame)
      case GameProgressValues.GameWonAndOver =>
        val updatedGame = boardWalker.game.copy(state = GameState.Won)
        gameRepository.save(updatedGame)
      case _ =>
        Right(())
    }
  }

  private def validateCellPosition(game: Game, position: Position): AppResult[Cell] = {
    game.cellByPosition
      .get(position)
      .toRight(ClientError(s"invalid position. Game limit: [x: ${game.width}, y: ${game.height}]"))
  }

  private def checkGameState(game: Game, position: Position)(implicit session: DBSession): AppResult[Game] = game.state match {
    case GameState.Created => initGame(game, position)
    case GameState.Running => Right(game)
    case GameState.Paused => resumeGame(game)
    case GameState.Lost | GameState.Won => Left(ClientError("try to perform an action against a finished game"))
  }

  private def initGame(game: Game, firstCellUncovered: Position)(implicit session: DBSession): AppResult[Game] = {
    val cells = generateCells(game, firstCellUncovered)
    for {
      _ <- cellRepository.create(cells)
      _ <- gameRepository.save(game.copy(state = GameState.Running, startTime = ZonedDateTime.now()))
      updatedGame <- gameRepository.findById(game.id)
    } yield updatedGame
  }

  private def resumeGame(game: Game)(implicit session: DBSession): AppResult[Game] = {
    for {
      newState <- game.state.transition(GameState.Running)
      _ <- gameRepository.save(game.copy(state = newState))
      game <- gameRepository.findById(game.id)
    } yield game
  }

  private def doReveal(game: Game, position: Position): BoardWalker = {
    val cell = game.cellByPosition(position)
    val walker = BoardWalker(
      game = game,
      nextCell = Some(cell),
      visitedCells = Map(cell.position -> cell),
      visitableNeighbours = Map(),
      updatedCells = Seq(),
      gameProgress = GameProgressValues.GameContinue,
    )
    val finalWalker = traverseBoard(walker)
    if (
      finalWalker.updatedCells.size + finalWalker.game.cells.count(c => !c.hasMine && c.state == CellState.Uncovered) ==
        (game.width * game.height - game.mines))
      finalWalker.copy(gameProgress = GameProgressValues.GameWonAndOver)
    else finalWalker
  }

  private def neighboursFrom(game: Game, cellPosition: Position): Seq[Cell] = {
    neighboursPosition(cellPosition) flatMap game.cellByPosition.get
  }

  @tailrec
  final def traverseBoard(boardWalker: BoardWalker): BoardWalker = {
    boardWalker.nextCell match {
      case None => boardWalker
      case Some(cell) =>
        if (cell.hasMine) boardWalker.copy(gameProgress = GameProgressValues.GameLostAndOver)
        else {
          val myVisitableNeighbours = neighboursFrom(boardWalker.game, Position(cell))
            .filterNot(cell => {
              boardWalker.visitedCells.contains(cell.position) || cell.state == CellState.Uncovered
            })
          val nextVisitableNeighbours: Map[Position, Cell] = if (myVisitableNeighbours.forall(!_.hasMine)) {
            boardWalker.visitableNeighbours ++ myVisitableNeighbours.map(c => c.position -> c) - cell.position
          } else {
            boardWalker.visitableNeighbours - cell.position
          }
          val nextCell: Option[Cell] = nextVisitableNeighbours.headOption.map(_._2)
          val newBoard = boardWalker.copy(
            nextCell = nextCell,
            visitableNeighbours = nextCell.map(cell => nextVisitableNeighbours - cell.position).getOrElse(nextVisitableNeighbours),
            visitedCells = nextCell.map(cell => boardWalker.visitedCells + (cell.position -> cell)).getOrElse(boardWalker.visitedCells),
            updatedCells = boardWalker.updatedCells :+ cell.copy(state = CellState.Uncovered)
          )
          traverseBoard(newBoard)
        }
    }
  }

  private def generateCells(game: Game, firstCellUncovered: Position): Seq[CellCreationCommand] = {
    val minePositions = distributeMines(game, firstCellUncovered)
    for {
      x <- 1 to game.width
      y <- 1 to game.height
    } yield {
      val position = Position(x, y)
      CellCreationCommand(
        gameId = game.id,
        x = x,
        y = y,
        hasMine = minePositions.contains(position),
        adjacentMines = countAdjacentMines(minePositions, position),
      )
    }
  }

  private def distributeMines(game: Game, firstCellUncovered: Position): Set[Position] = {
    val rnd = new scala.util.Random

    @tailrec
    def putMine(pickedPos: Set[Position]): Set[Position] = {
      val candidate = Position(x = rnd.nextInt(game.width) + 1, y = rnd.nextInt(game.height) + 1)
      if (candidate == firstCellUncovered) putMine(pickedPos)
      else if (pickedPos.contains(candidate)) putMine(pickedPos) // if the position is already taken try again
      else pickedPos + candidate
    }

    (1 to game.mines).foldLeft(Set.empty[Position])((minePositions, _) => putMine(minePositions))
  }

  private def countAdjacentMines(mines: Set[Position], cellPosition: Position): Int = {
    neighboursPosition(cellPosition) count mines.contains
  }

  private def neighboursPosition(cellPosition: Position) = {
    Seq(
      cellPosition.nextX,
      cellPosition.nextX.nextY,
      cellPosition.nextY,
      cellPosition.nextY.previousX,
      cellPosition.previousX,
      cellPosition.previousX.previousY,
      cellPosition.previousY,
      cellPosition.previousY.nextX,
    )
  }
}

object GameService {

  case class BoardWalker(game: Game, nextCell: Option[Cell],
                         visitedCells: Map[Position, Cell],
                         visitableNeighbours: Map[Position, Cell],
                         updatedCells: Seq[Cell],
                         gameProgress: GameProgress)

  sealed trait GameProgress

  object GameProgressValues {

    case object GameWonAndOver extends GameProgress

    case object GameLostAndOver extends GameProgress

    case object GameContinue extends GameProgress

  }

}
