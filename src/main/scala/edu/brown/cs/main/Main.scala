package edu.brown.cs.main

import edu.brown.cs.chessgame.{GameCommands, GameState}
import edu.brown.cs.io.{ChessLogger, REPL}
import edu.brown.cs.io.lichess.LichessEndpoint
import edu.brown.cs.uci.EngineCommands

import scala.collection.mutable
import scala.io.Source
import scala.io.BufferedSource

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
    if(args.size == 0){ //default - run with Lichess bot only
      ChessLogger.info("Initializing in bot-only mode.")
      ep.streamEvents(Vector[String]())
      while(ep.isConnectionOpen()){

      }
    } else { // run with a REPL in terminal
      if(args(0).toLowerCase().equals("repl")){
        val commandMap: mutable.HashMap[String, Vector[String] => Any] = mutable.HashMap(
          "greet" -> GeneralCommands.hello,
          "startgame" -> GameCommands.startGame,
          "move" -> GameCommands.makeMove,
          "bestmove" -> GameCommands.makeMove,
          "evaluate" -> GameCommands.evaluate,
          "depth" -> GameCommands.depth,
          "upgrade" -> ep.upgradeToBot,
          "connect" -> ep.streamEvents,
          "engine" -> EngineCommands.startEngine,
          "stopengine" -> EngineCommands.stopEngine,
          "uci" -> EngineCommands.updateUciStream
        )
        val repl = new REPL(commandMap)
        repl.run()
      } else {
        ChessLogger.error("Unrecognized argument.")
      }
    }
  }
}
