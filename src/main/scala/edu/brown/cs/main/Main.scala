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
    EngineCommands.setEndpoint(ep)
    EngineCommands.setEngineConf(engine)
    if(args.size == 0){ //default - run without REPL.
      ChessLogger.info("Initializing in dialogue-free mode.")
      ep.streamEvents(Vector[String]())
      while(ep.isConnectionOpen()){

      }
    } else { // run with a REPL in terminal
      if(args.contains("repl")){
        val commandMap: mutable.HashMap[String, Vector[String] => Any] = mutable.HashMap(
          "help" -> GeneralCommands.hello,
          "upgrade" -> ep.upgradeToBot,
          "connect" -> ep.streamEvents,
          "startengine" -> EngineCommands.startEngine,
          "conf" -> EngineCommands.updateUciStream
        )
        val repl = new REPL(commandMap)
        repl.run()
      } else {
        ChessLogger.error("Unrecognized argument.")
      }
    }
  }
}
