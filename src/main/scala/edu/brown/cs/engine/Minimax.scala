package edu.brown.cs.engine

import chess.{Game, Move}

object MiniMax {
  val MAXIMIZE = true
  val MINIMIZE = false
  def minimax(currDepth: Int, game: Game, shouldMaximize: Boolean, maxDepth: Int): Double ={
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
        var ret = Double.PositiveInfinity
        moveList.foreach(m => ret = Math.min(MiniMax.minimax(currDepth + 1, game(m), MINIMIZE, maxDepth), ret))
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
        var ret = Double.NegativeInfinity
        moveList.foreach(m => ret = Math.max(MiniMax.minimax(currDepth + 1, game(m), MAXIMIZE, maxDepth), ret))
//        println(s"recursive call to MINIMIZE at depth $currDepth returned $ret")
        ret
      }
    }
  }
}
