package engine

import chess.Pos
import chess.format.Uci
import edu.brown.cs.io.endpoints.OpeningEndpoint
import org.scalatest.Assertions._
import org.junit.Test

class OpeningSuite {
  val mov = OpeningEndpoint.openingQuery("rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2")
  val non = OpeningEndpoint.openingQuery("6k1/5pb1/6p1/1p1B4/1p6/1P5P/5PP1/6K1 w - - 0 1")

  @Test def scotchTest(): Unit ={
    assert(mov.equals(Some(Uci.Move(Pos.B8, Pos.C6, None))))
  }

  @Test def noneTest(): Unit ={
    assert(non.equals(None))
  }
}
