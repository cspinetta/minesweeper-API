package object models {
  type Cells = Map[Position, Cell]
  type AppResult[T] = Either[AppError, T]
}
