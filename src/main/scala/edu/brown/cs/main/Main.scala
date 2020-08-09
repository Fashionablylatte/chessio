package edu.brown.cs.main

import edu.brown.cs.chessgame.{GameCommands, GameState}
import edu.brown.cs.io.REPL
import edu.brown.cs.io.lichess.LichessEndpoint

import scala.collection.mutable

/**
 * @author ${user.name}
 */
object Main {
  def main(args: Array[String]): Unit = {
    val ep = new LichessEndpoint
    val commandMap: mutable.HashMap[String, Vector[String] => Any] = mutable.HashMap(
      "greet" -> GeneralCommands.hello,
      "startgame" -> GameCommands.startGame,
      "move" -> GameCommands.makeMove,
      "evaluate" -> GameCommands.evaluate,
      "depth" -> GameCommands.depth,
      "upgrade" -> ep.upgradeToBot,
      "connect" -> ep.streamEvents
    )

    val repl = new REPL(commandMap)
    repl.run()
  }
}
