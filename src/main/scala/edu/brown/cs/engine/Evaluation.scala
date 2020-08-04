package edu.brown.cs.engine

import chess.Game
import chess.Status.{Draw, Mate, Stalemate, VariantEnd}

object Evaluation {

  def evalPos(game: Game): Double = {
    val status = game.situation.status
    status match {
      case Some(s) =>
        s match {
          case Mate => if(game.situation.winner.get.white) Double.PositiveInfinity else Double.NegativeInfinity
          case VariantEnd => if(game.situation.winner.get.white) Double.PositiveInfinity else Double.NegativeInfinity
          case Stalemate => 0.0
          case Draw => 0.0
        }
      case None => heuristic(game)
    }
  }

  private def heuristic(game: Game): Double = {
    game.situation.board.materialImbalance
  }
}
