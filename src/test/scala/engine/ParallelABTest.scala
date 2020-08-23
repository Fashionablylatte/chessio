package engine

import chess.format.Forsyth
import chess.variant.Standard
import chess.{Game, Situation}
import edu.brown.cs.chessgame.PosHash
import edu.brown.cs.engine.{AlphaBeta, ParallelAB}
import org.junit.Test
import org.scalatest.Assertions

import scala.collection.mutable


class ParallelABSuite extends Assertions {

  val pm = new mutable.HashMap[PosHash, Int](120000, 0.9)
  val ab = new ParallelAB(pm)

  def isWithin(x: Int, y: Int, precision: Int) = {
    (x - y).abs < precision
  }

  @Test def startingPositionEvaluationTest(): Unit ={
    val sit: Option[Situation] = Forsyth <<@(Standard, "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    val init: Game = new Game(sit.get)

    val bestPair = ab.alphabeta(init, true, 2)

    assert(isWithin(bestPair.get._1, 0, 800))
    //as the heuristics get updated, this will probably stray higher than 0.0, so we use isWithin.
  }

  @Test def mateOnePositionEvaluationTest(): Unit ={
    val sit: Option[Situation] = Forsyth <<@(Standard, "7k/5Q2/5K2/8/8/8/8/8 w - - 0 1")
    val mate: Game = new Game(sit.get)

    val bestPair = ab.alphabeta(mate, true, 2)

    assert(bestPair.get._1 == Int.MaxValue)
    assert(bestPair.get._2.orig.toString.equals("f7"))
    assert(bestPair.get._2.dest.toString.equals("g7"))
  }

  @Test def mateTwoPositionEvaluationTest(): Unit ={
    val sit: Option[Situation] = Forsyth <<@(Standard, "7k/4Q3/4K3/8/8/8/8/8 w - - 0 1")
    val mate: Game = new Game(sit.get)

    val bestPair = ab.alphabeta(mate, true, 2)

    assert(bestPair.get._1 >= 900)
    //as the heuristics get updated, this will probably stray higher than 9.0, so we use isWithin.

    val bestPair2 = ab.alphabeta(mate, true, 3)
    assert(bestPair2.get._1 == Int.MaxValue)
    //when depth increases to 4, the alphabeta can now detect checkmate.
  }

  @Test def captureMaterialPositionEvaluationTest(): Unit ={
    val sit: Option[Situation] = Forsyth <<@(Standard, "3k2r1/8/8/8/8/8/R2K4/8 w - - 0 1")
    val win: Game = new Game(sit.get)

    val bestPair = ab.alphabeta(win, true, 3)

    assert(bestPair.get._1 >= 500)
    //as the heuristics get updated, this will probably stray higher than 9.0, so we use >=.

    val rev: Option[Situation] = Forsyth <<@(Standard, "3k2r1/8/8/8/8/8/R2K4/8 b - - 0 1")
    val loss: Game = new Game(rev.get)

    val bestPair2 = ab.alphabeta(loss, false, 3)

    assert(bestPair2.get._1 <= -500)
    //as the heuristics get updated, this will probably stray higher than 9.0, so we use >=.
  }

  @Test def rooksWalkingMate(): Unit ={
    val mate2: Option[Situation] = Forsyth <<@(Standard, "4r2k/2R4p/p4p2/8/3P2P1/5R1P/r7/7K b - - 2 36")
    val win: Game = new Game(mate2.get)

    val bestPair = ab.alphabeta(win, false, 3)

    assert(bestPair.get._1 == Int.MinValue)
    assert(bestPair.get._2.dest.toString.equals("e1"))
  }

  @Test def pawnForkCheck(): Unit ={
    val fork: Option[Situation] = Forsyth <<@(Standard, "2k5/1p6/p1p1ppqb/P2p3p/1P1P2RP/2P1PN1Q/4KP2/8 b - - 0 33")
    val win: Game = new Game(fork.get)

    val bestPair = ab.alphabeta(win, false, 4)

    assert(bestPair.get._1 <= -200)
    assert(bestPair.get._2.orig.toString.equals("h5"))
  }

//  @Test def trapRook(): Unit ={
//    val trap: Option[Situation] = Forsyth <<@(Standard, "r1bq2kr/ppp1b1pp/8/3pP1NQ/4p3/8/PPP2PPP/R1B1K2R b KQ - 2 11")
//    val gain: Game = new Game(trap.get)
//
//    val bestPair = ab.alphabeta(gain, false, 8)
//
//    assert(bestPair.get._1 <= 100)
//    assert(bestPair.get._2.orig.equals("g7"))
//    assert(bestPair.get._2.orig.equals("g6"))
//  }
}
