package edu.brown.cs.engine

import chess.Move
import edu.brown.cs.chessgame.GameState

import scala.collection.mutable

class MinimaxEngine(gameState: GameState, isWhite: Boolean) {
  private var depth = 2

  val whiteOrdering: Ordering[(Int, Move)] = new Ordering[(Int, Move)]{
    def compare(a: (Int, Move), b: (Int, Move)) = a._1 compare b._1
    override def toString = "whiteOrdering"
  }
  val blackOrdering: Ordering[(Int, Move)] = new Ordering[(Int, Move)]{
    def compare(a: (Int, Move), b: (Int, Move)) = b._1 compare a._1
    override def toString = "blackOrdering"
  }

  def makeMove(): Unit ={
    val moveList: List[Move] = gameState.getLegal().collect(pair => pair._2).foldLeft(List[Move]())(_.appendedAll(_))
    if(moveList.isEmpty){
      Console.println("Good game!")
    } else { //TODO we can multithread here later
      val moveQ = new mutable.PriorityQueue[(Int, Move)]()(if(isWhite) whiteOrdering else blackOrdering)
      moveList.foreach(m => moveQ.enqueue((MiniMax.minimax(0, gameState.getGame().apply(m), isWhite, depth), m)))
      val bestPair = moveQ.dequeue()
      val bestMove = bestPair._2
      println(s"best move was $bestPair at depth $depth")
      var movestr = ""
      while(moveQ.nonEmpty){
        movestr += s", ${moveQ.dequeue().toString()}"
      }
      println(s"other moves in order: $movestr")
      println(s"ordering used was ${moveQ.ord.toString()}")
      gameState.makeMove(bestMove.orig.toString, bestMove.dest.toString, "") //TODO fix promo
    }
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