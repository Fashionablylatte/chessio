package engine

import chess.{Game, Situation}
import chess.format.Forsyth
import chess.variant.Standard
import edu.brown.cs.engine.Evaluation

import org.scalatest.Assertions
import org.junit.Test


class EvaluationSuite extends Assertions {

  def isWithin(x: Double, y: Double, precision: Double) = {
    (x - y).abs < precision
  }

  @Test def startingPositionEvaluationTest(): Unit ={
    val sit: Option[Situation] = Forsyth <<@(Standard, "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    val init: Game = new Game(sit.get)
    assert(isWithin(Evaluation.evalPos(init), 0.0, 0.8))
    //as the heuristics get updated, this will probably stray higher than 0.0, so we use isWithin.
  }

  @Test def checkmatePositionEvaluationTest(): Unit ={
    val sit: Option[Situation] = Forsyth <<@(Standard, "r1bqkbnr/ppp2Qpp/2np4/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 1")
    val mate: Game = new Game(sit.get)
    assert(Evaluation.evalPos(mate) == Double.PositiveInfinity)
  }

  @Test def stalematePositionEvaluationTest(): Unit ={
    val sit: Option[Situation] = Forsyth <<@(Standard, "8/8/8/8/8/4k3/5q2/7K w - - 0 1")
    val stale: Game = new Game(sit.get)
    assert(Evaluation.evalPos(stale) == 0.0)
  }

  @Test def extraBlackQueenPositionEvaluationTest(): Unit ={
    val sit: Option[Situation] = Forsyth <<@(Standard, "8/8/8/8/8/4kq2/8/7K w - - 0 1")
    val win: Game = new Game(sit.get)
    assert(Evaluation.evalPos(win) <= -9.0)
    //as the heuristics get updated, this will probably stray higher than 9.0, so we use <=
  }
}
