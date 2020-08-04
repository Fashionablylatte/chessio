package edu.brown.cs.engine

import chess.{Game, Move}
import edu.brown.cs.chessgame.{GameHash, PosHash}

import scala.collection.mutable

class AlphaBeta(posMap: mutable.HashMap[PosHash, Double]) {
  val MAXIMIZE = true
  val MINIMIZE = false
  def alphabeta(currDepth: Int, game: Game, shouldMaximize: Boolean, maxDepth: Int, alpha: Double, beta: Double): Double ={
    if(currDepth == maxDepth){
      val hash = GameHash.hashGame(game)
      val ret = posMap.get(hash) match {
        case Some(r) => println("found"); r
        case None => val r = Evaluation.evalPos(game); posMap.put(hash, r); r
      }
//      println(s"recursive call to ${if(shouldMaximize) "MAXIMIZE" else "MINIMIZE"} at depth $currDepth returned $ret")
      ret
    } else if(shouldMaximize) {
      val moveList: List[Move] = game.situation.moves.collect(pair => pair._2).foldLeft(List[Move]())(_.appendedAll(_))
      if(moveList.isEmpty){
        val hash = GameHash.hashGame(game)
        val ret = posMap.get(hash) match {
          case Some(r) => println("found"); r
          case None => val r = Evaluation.evalPos(game); posMap.put(hash, r); r
        }
//        println(s"recursive call to MAXIMIZE at depth $currDepth returned $ret")
        ret
      } else {
        var ret = Double.PositiveInfinity
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
        val hash = GameHash.hashGame(game)
        val ret = posMap.get(hash) match {
          case Some(r) => println("found"); r
          case None => val r = Evaluation.evalPos(game); posMap.put(hash, r); r
        }
//        println(s"recursive call to MINIMIZE at depth $currDepth returned $ret")
        ret
      } else {
        var ret = Double.NegativeInfinity
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
