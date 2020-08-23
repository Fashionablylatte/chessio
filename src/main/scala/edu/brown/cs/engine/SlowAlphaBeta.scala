package edu.brown.cs.engine

import chess.{Game, Move}

object SlowAlphaBeta {
  val MAXIMIZE = true
  val MINIMIZE = false
  def alphabeta(currDepth: Int, game: Game, shouldMaximize: Boolean, maxDepth: Int, alpha: Int, beta: Int): Int ={
    if(currDepth == maxDepth){
      val ret = Evaluation.evalPos(game)
      //      println(s"recursive call to ${if(shouldMaximize) "MAXIMIZE" else "MINIMIZE"} at depth $currDepth returned $ret")
      ret
    } else if(shouldMaximize) {
      val moveList: List[Move] = game.situation.moves.collect(pair => pair._2).foldLeft(List[Move]())(_.appendedAll(_))
      if(moveList.isEmpty){
        val ret = Evaluation.evalPos(game)
        //        println(s"recursive call to MAXIMIZE at depth $currDepth returned $ret")
        ret
      } else {
        var ret = Int.MaxValue
        var b = beta
        val it = moveList.iterator
        while(it.hasNext && b > alpha){
          ret = Math.min(alphabeta(currDepth + 1, game(it.next), MINIMIZE, maxDepth, alpha, b), ret)
          b = Math.min(ret, b)
        }
        //        println(s"recursive call to MAXIMIZE at depth $currDepth returned $ret")
        ret
      }
    } else {
      val moveList: List[Move] = game.situation.moves.collect(pair => pair._2).foldLeft(List[Move]())(_.appendedAll(_))
      if(moveList.isEmpty){
        val ret = Evaluation.evalPos(game)
        //        println(s"recursive call to MINIMIZE at depth $currDepth returned $ret")
        ret
      } else {
        var ret = Int.MinValue
        var a = alpha
        val it = moveList.iterator
        while(it.hasNext && a < beta){
          ret = Math.max(alphabeta(currDepth + 1, game(it.next), MAXIMIZE, maxDepth, a, beta), ret)
          a = Math.max(ret, a)
        }
        //        println(s"recursive call to MINIMIZE at depth $currDepth returned $ret")
        ret
      }
    }
  }
}
