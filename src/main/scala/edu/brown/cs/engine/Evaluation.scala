package edu.brown.cs.engine

import chess.Game
import chess.Status.{Draw, Mate, Stalemate, VariantEnd}

object Evaluation {

  /**
   * Evaluates a game position, returning the value in centipawns.
   * @param game - a Scalachess game object.
   * @return the evaluation in centipawns.
   */
  def evalPos(game: Game): Int = {
    val status = game.situation.status
    status match {
      case Some(s) =>
        s match {
          case Mate => if(game.situation.winner.get.white) Int.MaxValue else Int.MinValue
          case VariantEnd => if(game.situation.winner.get.white) Int.MaxValue else Int.MinValue
          case Stalemate => 0
          case Draw => 0
        }
      case None => heuristic(game)
    }
  }

  //In centipawns
  private def heuristic(game: Game): Int = {
    game.situation.board.materialImbalance * 100
  }
}
