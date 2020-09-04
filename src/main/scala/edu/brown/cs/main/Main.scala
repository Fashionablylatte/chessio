package edu.brown.cs.main

import edu.brown.cs.io.endpoints.LichessEndpoint
import edu.brown.cs.io.REPL
import edu.brown.cs.io.logging.ChessLogger
import edu.brown.cs.io.uci.EngineCommands

import scala.collection.mutable


/**
 * @author ${user.name}
 */
object Main {
  val configs = scala.xml.XML.loadFile("config/config.xml")
  val server = (configs \ "environment").map(token => token.text)(0)

  val botId =  (configs \ "bot" \ "id").map(token => token.text)(0).toLowerCase()
  if(botId.isEmpty) throw new InstantiationException("Missing bot ID")

  val token = (configs \ "bot" \ "token").map(token => token.text)(0)
  if(botId.isEmpty) throw new InstantiationException("Missing bot token")

  val engine = (configs \ "engine").map(token => token.text)(0)

  def main(args: Array[String]): Unit = {
    val ep = new LichessEndpoint(token, botId, server)
    EngineCommands.setEngineConf(engine)
    EngineCommands.setEndpoint(ep)
    if(args.size == 0){ //default - run without REPL.
      ChessLogger.info("Initializing in REPL-free mode.")
      ep.streamEvents(Vector[String]())
      while(ep.isConnectionOpen()){

      }
      ChessLogger.info("Connection closed.")
    } else { // run with a REPL in terminal
      if(args.contains("repl")){
        val commandMap: mutable.HashMap[String, (String, Vector[String] => Any)] = mutable.HashMap(
          "hello" -> ("Send a greeting, e.g. 'hello' or 'hello, Bot'", GeneralCommands.hello),
          "upgrade" -> ("Upgrade account to Bot status. CANNOT BE UNDONE!", ep.upgradeToBot),
          "connect" -> ("Connects the bot to Lichess.", ep.streamEvents),
          "startengine" -> ("Starts the UCI engine locally, for debugging purposes.", EngineCommands.startEngine),
          "stopengine" -> ("Stops the UCI engine instance.", EngineCommands.stopEngine),
          "conf" -> ("Sends a UCI configuration message to the engine, e.g. 'conf \"uci\"' sends the message 'uci' to" +
            "the engine. For multiple params make sure you surround them in double quotes!", EngineCommands.updateUciStream)
        )
        val repl = new REPL(commandMap)
        repl.run()
      } else {
        ChessLogger.error("Unrecognized argument.")
      }
    }
  }
}
