package edu.brown.cs.engine

import chess.Move
import chess.format.Forsyth
import edu.brown.cs.chessgame.{GameState, PosHash}
import edu.brown.cs.io.ChessLogger
import edu.brown.cs.io.endgames.TablebaseEndpoint
import edu.brown.cs.io.openings.OpeningEndpoint

import scala.collection.mutable

class AlphaBetaEngine(gameState: GameState, isWhite: Boolean) {
  private var depth = 4
  private val table = new mutable.HashMap[PosHash, Int](120000, 0.9)
  private val ab = new AlphaBeta(table)
  private var book = true
  private var syzygy = gameState.isSyzygy()
  private var lockSyzygy = false;

  val whiteOrdering: Ordering[(Int, Move)] = new Ordering[(Int, Move)]{
    def compare(a: (Int, Move), b: (Int, Move)) = a._1 compare b._1
    override def toString = "whiteOrdering"
  }
  val blackOrdering: Ordering[(Int, Move)] = new Ordering[(Int, Move)]{
    def compare(a: (Int, Move), b: (Int, Move)) = b._1 compare a._1
    override def toString = "blackOrdering"
  }

  def makeMove(): Unit ={
    if(book){
      OpeningEndpoint.openingQuery(Forsyth >> gameState.getGame()) match {
        case Some(move) =>
          ChessLogger.info("Valid ODB response.")
          val prom = if(move.promotion.nonEmpty) move.promotion.get.forsyth.toString() else ""
          gameState.makeMove(move.orig.toString, move.dest.toString, prom)
        case None =>
          ChessLogger.warn("Failed ODB response, closing book.")
          book = false
          makeMove()
      }
    } else if(syzygy) {
      TablebaseEndpoint.mainlineQuery(Forsyth >> gameState.getGame()) match {
        case Some(move) =>
          ChessLogger.info("Valid TB response.")
          val prom = if(move.promotion.nonEmpty) move.promotion.get.forsyth.toString() else ""
          gameState.makeMove(move.orig.toString, move.dest.toString, prom)
        case None =>
          ChessLogger.warn("Failed TB response, closing table.")
          syzygy = false
          lockSyzygy = true
          makeMove()
      }
    } else {
        val bestPair = ab.alphabeta(gameState.getGame(), isWhite, depth)
        bestPair match {
          case Some(m) =>
            val bestMove = m._2
            println(s"best move was $m at depth $depth")
            val prom = if(bestMove.promotion.nonEmpty) bestMove.promotion.get.forsyth.toString() else ""
            gameState.makeMove(bestMove.orig.toString, bestMove.dest.toString, prom)
          case None =>
            ChessLogger.info(s"Good game!")
        }
      }
      if(!lockSyzygy) syzygy = gameState.isSyzygy()
    }

  def setDepth(d: Int): Unit = depth = d
}


















//def makeMove(): Unit ={
//  gameState.getLegal().iterator.nextOption() match {
//  case Some(t) =>
//  val move = t._2
//  Thread.sleep(500)
//  gameState.makeMove(move.last.orig.toString, move.last.dest.toString)
//  case None => Console.println("Good game!")
//}
//}