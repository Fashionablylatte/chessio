package edu.brown.cs.engine

import chess.{Game, Move}
import edu.brown.cs.chessgame.{GameHash, PosHash}
import edu.brown.cs.io.ChessLogger

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class ParallelAB(posMap: mutable.HashMap[PosHash, Int]) {
  val MAXIMIZE = true
  val MINIMIZE = false

  val whiteOrdering: Ordering[(Int, Move)] = new Ordering[(Int, Move)]{
    def compare(a: (Int, Move), b: (Int, Move)) = a._1 compare b._1
    override def toString = "whiteOrdering"
  }
  val blackOrdering: Ordering[(Int, Move)] = new Ordering[(Int, Move)]{
    def compare(a: (Int, Move), b: (Int, Move)) = b._1 compare a._1
    override def toString = "blackOrdering"
  }

  private def moveFilter(move: Move): Boolean = {
//    move.orig.toString.equals("g8") && move.dest.toString.equals("g2")
    true
  }

  def alphabeta(game: Game, whiteMove: Boolean, maxDepth: Int): Option[(Int, Move)] ={
    val moveList: BlockingQueue[Move] = game.situation.moves.collect(pair => pair._2).foldLeft(List[Move]())(_.appendedAll(_)).filter(moveFilter)
    var bestMove: Option[(Int, Move)] = None
    if(moveList.nonEmpty){
      val moveQ = new mutable.PriorityQueue[(Int, Move)]()(if (whiteMove) whiteOrdering else blackOrdering)
      val futureList = new ArrayBuffer[(Future[Int], Move)](moveList.size)

      class CalcThread extends Runnable{
        override def run(): Unit ={

        }
      }

      moveList.foreach(mv => {
        val evalFuture = Future(abHelp(game(mv), !whiteMove, 1, maxDepth, Int.MinValue, Int.MaxValue))
        futureList.append((evalFuture, mv))
      })
      futureList.foreach(pair => {
        pair._1 onComplete {
          case Success(eval) =>
            moveQ.enqueue((eval, pair._2))
            ChessLogger.debug(s"Queued move ${pair._2}")
          case Failure(ex) => ChessLogger.error(s"Unable to evaluate move ${pair._2}")
        }
      })

      ChessLogger.info(s"Moves found in order: ${moveQ.clone.dequeueAll.mkString(", ")}")
      bestMove = Some(moveQ.dequeue())
    }
    bestMove
  }

  def abHelp(game: Game, shouldMaximize: Boolean, depth: Int, maxDepth: Int, alpha: Int, beta: Int): Int ={
    val moveList: List[Move] = game.situation.moves.collect(pair => pair._2).foldLeft(List[Move]())(_.appendedAll(_))
    lazy val hash = GameHash.hashGame(game)
    if(depth == maxDepth || moveList.isEmpty){
      posMap.get(hash) match {
        case Some(p) =>
          ChessLogger.trace(s"depth $depth eval: $p")
          ChessLogger.pos("", game)
          p
        case None =>
          val p = Evaluation.evalPos(game)
          ChessLogger.trace(s"depth $depth eval: $p")
          ChessLogger.pos("", game)
          posMap.put(hash, p)
          p
      }
    } else if (shouldMaximize){
      var maxEval = Int.MinValue
      var newAlpha = alpha
      moveList.foreach(
        mv =>
          if(beta > newAlpha){
            maxEval = Math.max(maxEval, abHelp(game(mv), !shouldMaximize, depth + 1, maxDepth, newAlpha, beta))
            newAlpha = Math.max(newAlpha, maxEval)
          }
      )
      maxEval
    } else {
      var minEval = Int.MaxValue
      var newBeta = beta
      moveList.foreach(
        mv =>
          if(newBeta > alpha){
            minEval = Math.min(minEval, abHelp(game(mv), !shouldMaximize, depth + 1, maxDepth, alpha, newBeta))
            newBeta = Math.min(newBeta, minEval)
          }
      )
      minEval
    }
  }

}
