package engine

import chess.{Game, Situation}
import chess.format.Forsyth
import chess.variant.Standard
import edu.brown.cs.engine.MiniMax

import org.scalatest.Assertions
import org.junit.Test


class MinimaxSuite extends Assertions {

  def isWithin(x: Double, y: Double, precision: Double) = {
    (x - y).abs < precision
  }

  @Test def startingPositionEvaluationTest(): Unit ={
    val sit: Option[Situation] = Forsyth <<@(Standard, "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    val init: Game = new Game(sit.get)
    assert(isWithin(MiniMax.minimax(0, init, MiniMax.MAXIMIZE, 2), 0.0, 0.8))
    //as the heuristics get updated, this will probably stray higher than 0.0, so we use isWithin.
  }

  @Test def mateOnePositionEvaluationTest(): Unit ={
    val sit: Option[Situation] = Forsyth <<@(Standard, "7k/5Q2/5K2/8/8/8/8/8 w - - 0 1")
    val mate: Game = new Game(sit.get)
    assert(MiniMax.minimax(0, mate, MiniMax.MAXIMIZE, 2) == Double.PositiveInfinity)
  }

  @Test def mateTwoPositionEvaluationTest(): Unit ={
    val sit: Option[Situation] = Forsyth <<@(Standard, "7k/4Q3/4K3/8/8/8/8/8 w - - 0 1")
    val mate: Game = new Game(sit.get)
    assert(MiniMax.minimax(0, mate, MiniMax.MAXIMIZE, 2) >= 9.0)
    //as the heuristics get updated, this will probably stray higher than 9.0, so we use isWithin.
    assert(MiniMax.minimax(0, mate, MiniMax.MAXIMIZE, 4) == Double.PositiveInfinity)
    //when depth increases to 4, the minimax can now detect checkmate.
  }

  @Test def captureMaterialPositionEvaluationTest(): Unit ={
    val sit: Option[Situation] = Forsyth <<@(Standard, "3k2r1/8/8/8/8/8/R2K4/8 w - - 0 1")
    val win: Game = new Game(sit.get)
    assert(MiniMax.minimax(0, win, MiniMax.MAXIMIZE, 4) >= 5.0)
    //as the heuristics get updated, this will probably stray higher than 9.0, so we use >=.

    val rev: Option[Situation] = Forsyth <<@(Standard, "3k2r1/8/8/8/8/8/R2K4/8 b - - 0 1")
    val loss: Game = new Game(rev.get)
    assert(MiniMax.minimax(0, loss, MiniMax.MINIMIZE, 4) <= -5.0)
    //as the heuristics get updated, this will probably stray higher than 9.0, so we use >=.
  }
}
