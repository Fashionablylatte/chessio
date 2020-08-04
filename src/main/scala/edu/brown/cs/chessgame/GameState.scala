package edu.brown.cs.chessgame

import chess.Status.{Draw, Mate, Stalemate, VariantEnd}
import chess.format.Forsyth
import chess.{Game, Move, MoveMetrics, Pos, Setup, variant}
import scalaz.{Failure, Success}

class GameState(fen: String = "None") {
  private var game = Setup(variant.Standard)
  if(!fen.equals("None")){
    val sit = Forsyth <<@(variant.Standard, fen)
    if(sit.nonEmpty) game = game.copy(situation = sit.get)
  }

  printBoard()

  def printBoard(): Unit = println(game.board.toString)

  def makeMove(start: String, dest: String): Unit ={
    if(isEnd()){
      Console.println("'startgame' to start another game.")
      } else {
      val startSquare = Pos.posAt(start.toLowerCase())
      val destSquare = Pos.posAt(dest.toLowerCase())
      startSquare match {
        case None => Console.println("Invalid starting square")
        case Some(s) =>
          destSquare match {
            case None => Console.println("Invalid destination square")
            case Some(d) =>
              Console.println(s"${game.moveString} ${start} to ${dest}")
              game(s, d, None, MoveMetrics()) match {
                case Success(a) =>
                  game = a._1
                  Console.println(game.board.toString + "\n" + displayMoves())
                  isEnd()
                case Failure(e) => Console.println(s"Invalid or illegal move: ${e}")
              }
          }
      }
    }
  }

  def displayMoves(): String ={
    val gl = game.pgnMoves
    var gameString = ""
    for(i <- 0 to gl.length - 1){
      if(i % 2 == 0){
        gameString += s"${(i+2)/2}. ${gl(i)}"
      } else {
        gameString += s" ${gl(i)} "
      }
    }
    gameString
  }

  def isEnd(): Boolean ={
    val status = game.situation.status
    status match {
      case None => false
      case Some(s) =>
        s match {
          case Mate => Console.println(s"Checkmate, ${game.situation.winner.getOrElse("error")} wins!"); true
          case VariantEnd => Console.println(s"Variant ended, ${game.situation.winner.getOrElse("error")} wins!"); true
          case Stalemate => Console.println(s"Stalemate."); true
          case Draw => Console.println(s"A draw was declared automatically."); true
        }
    }
  }

  def getLegal(): Map[Pos, List[Move]] = game.situation.moves

  def getGame(): Game = game.copy()

  def getTurn(): Int = game.turns % 2
}
