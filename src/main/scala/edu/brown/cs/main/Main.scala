package edu.brown.cs.main

import edu.brown.cs.chessgame.{GameCommands, GameState}
import edu.brown.cs.io.REPL

import scala.collection.mutable

/**
 * @author ${user.name}
 */
object Main {
  def main(args: Array[String]): Unit = {
    val commandMap: mutable.HashMap[String, Vector[String] => Any] = mutable.HashMap(
      "greet" -> GeneralCommands.hello,
      "startgame" -> GameCommands.startGame,
      "move" -> GameCommands.makeMove,
      "evaluate" -> GameCommands.evaluate,
      "depth" -> GameCommands.depth
    )

    val repl = new REPL(commandMap)
    repl.run()
    new GameState()

  }

}
