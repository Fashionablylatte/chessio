package engine

import java.util

import edu.brown.cs.chessgame.GameHash
import edu.brown.cs.chessgame.GameHash.PosHash
import org.junit.Test

class HashSuite {
  @Test def hashTest: Unit ={
    val startHash = PosHash(GameHash.hashFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1").get)
    println(startHash.hashCode())
    val scotchHash = PosHash(GameHash.hashFen("r1bqk1nr/pppp1ppp/2n5/2b5/3NP3/8/PPP2PPP/RNBQKB1R w KQkq - 1 5").get)
    println(scotchHash.hashCode())
    val scotchHash2 = PosHash(GameHash.hashFen("r1bqk1nr/pppp1ppp/2N5/2b5/4P3/8/PPP2PPP/RNBQKB1R b KQkq - 0 5").get)
    println(scotchHash2.hashCode())
    val shcopy = PosHash(GameHash.hashFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1").get)
    println(shcopy.hashCode())
    assert(startHash.hashCode() == shcopy.hashCode())
    assert(scotchHash.hashCode() != scotchHash2.hashCode())
    assert(startHash.hashCode() != scotchHash.hashCode())
    assert(shcopy.hashCode() != scotchHash2.hashCode())
  }
}
