package edu.brown.cs.engine

import chess.Move
import chess.format.Forsyth
import edu.brown.cs.chessgame.{GameState, PosHash}
import edu.brown.cs.io.endgames.TablebaseEndpoint
import edu.brown.cs.io.openings.OpeningEndpoint

import scala.collection.mutable

class AlphaBetaEngine(gameState: GameState, isWhite: Boolean) {
  private var depth = 5
  private val table = new mutable.HashMap[PosHash, Double](120000, 0.9)
  private val ab = new AlphaBeta(table)
  private var book = true
  private var syzygy = gameState.isSyzygy()
  private var lockSyzygy = false;

  val whiteOrdering: Ordering[(Double, Move)] = new Ordering[(Double, Move)]{
    def compare(a: (Double, Move), b: (Double, Move)) = a._1 compare b._1
    override def toString = "whiteOrdering"
  }
  val blackOrdering: Ordering[(Double, Move)] = new Ordering[(Double, Move)]{
    def compare(a: (Double, Move), b: (Double, Move)) = b._1 compare a._1
    override def toString = "blackOrdering"
  }

  def makeMove(): Unit ={
    if(book){
      OpeningEndpoint.openingQuery(Forsyth >> gameState.getGame()) match {
        case Some(move) => println("Found opening move"); gameState.makeMove(move.orig.toString, move.dest.toString);
        case None => println("No ODB respoonse."); book = false; makeMove()
      }
    } else if(syzygy) {
      TablebaseEndpoint.mainlineQuery(Forsyth >> gameState.getGame()) match {
        case Some(move) => println("Found tablebase move"); gameState.makeMove(move.orig.toString, move.dest.toString);
        case None => println("No TBL respoonse."); syzygy = false; lockSyzygy = true; makeMove()
      }
    } else {
        val moveList: List[Move] = gameState.getLegal().collect(pair => pair._2).foldLeft(List[Move]())(_.appendedAll(_))
        if (moveList.isEmpty) {
          Console.println("Good game!")
        } else { //TODO we can multithread here later
          val moveQ = new mutable.PriorityQueue[(Double, Move)]()(if (isWhite) whiteOrdering else blackOrdering)
          moveList.foreach(m => moveQ.enqueue((ab.alphabeta(0, gameState.getGame().apply(m), isWhite, depth, Double.NegativeInfinity, Double.PositiveInfinity), m)))
          val bestPair = moveQ.dequeue()
          val bestMove = bestPair._2
          println(s"best move was $bestPair at depth $depth")
          var movestr = ""
          while (moveQ.nonEmpty) {
            movestr += s", ${moveQ.dequeue().toString()}"
          }
          println(s"other moves in order: $movestr")
          println(s"ordering used was ${moveQ.ord.toString()}")
          gameState.makeMove(bestMove.orig.toString, bestMove.dest.toString)
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