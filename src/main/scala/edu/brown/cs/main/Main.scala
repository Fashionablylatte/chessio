package edu.brown.cs.main

import edu.brown.cs.chessgame.{GameCommands, GameState}
import edu.brown.cs.io.REPL
import edu.brown.cs.io.lichess.LichessEndpoint

import scala.collection.mutable
import scala.io.Source

/**
 * @author ${user.name}
 */
object Main {
  val oauth = "config/token.conf"
  val bufferedSource = Source.fromFile(oauth)
  val token = bufferedSource.getLines.nextOption().getOrElse("")
  bufferedSource.close
  val botId = "fltestaccountbot"
  val server = "https://lichess.dev"

  def main(args: Array[String]): Unit = {
    val ep = new LichessEndpoint(token, botId, server)
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
