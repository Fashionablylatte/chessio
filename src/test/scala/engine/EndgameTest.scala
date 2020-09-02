package engine

import chess.{Pos, Queen}
import chess.format.Uci
import edu.brown.cs.io.endpoints.{OpeningEndpoint, TablebaseEndpoint}
import org.scalatest.Assertions._
import org.junit.Test

class EndgameTest {
  val mov = TablebaseEndpoint.mainlineQuery("4k3/6KP/8/8/8/8/7p/8 w - - 0 1")
  val non = TablebaseEndpoint.mainlineQuery("6k1/5pb1/6p1/1p1B4/1p6/1P5P/5PP1/6K1 w - - 0 1")

  @Test def endTest(): Unit ={
    assert(mov.equals(Some(Uci.Move(Pos.H7, Pos.H8, Some(Queen)))))
  }

  @Test def noneTest(): Unit ={
    assert(non.equals(None))
  }
}
