package edu.brown.cs.chessgame

import chess.Status.{Draw, Mate, Stalemate, VariantEnd}
import chess.format.Forsyth
import chess.{Game, Move, MoveMetrics, Pos, Role, Setup, variant}
import edu.brown.cs.io.lichess.LichessEndpoint
import scalaz.{Failure, Success}

class GameState(fen: String = "None", endpoint: Option[LichessEndpoint] = None) {
  private var game = Setup(variant.Standard)
  if(!fen.equals("None")){
    val sit = Forsyth <<@(variant.Standard, fen)
    if(sit.nonEmpty) game = game.copy(situation = sit.get)
  }

  printBoard()

  def printBoard(): Unit = println(game.board.toString)

  def makeMove(start: String, dest: String, promo: String): Option[Move] ={
    if(isEnd()){
      Console.println("'startgame' to start another game.")
      None
      } else {
      val startSquare = Pos.posAt(start.toLowerCase())
      val destSquare = Pos.posAt(dest.toLowerCase())
      val promote = Role.promotable(promo)
      startSquare match {
        case None => Console.println("Invalid starting square"); None
        case Some(s) =>
          destSquare match {
            case None => Console.println("Invalid destination square"); None
            case Some(d) =>
              Console.println(s"${game.moveString} ${start} to ${dest}")
              game(s, d, promote, MoveMetrics()) match {
                case Success(a) =>
                  game = a._1
                  Console.println(game.board.toString + "\n" + displayMoves())
                  isEnd()
                  if(endpoint.nonEmpty){
                    println("sending move to ext")
                    endpoint.get.sendMove(a._2)
                  } else {
                    println("no ext endpt")
                  }
                  Some(a._2)
                case Failure(e) => Console.println(s"Invalid or illegal move: ${e}"); None
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

  def isSyzygy(): Boolean = this.game.situation.board.pieces.size <= 7

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
