package edu.brown.cs.main

import edu.brown.cs.io.endpoints.LichessEndpoint
import edu.brown.cs.io.{ChessLogger, REPL}
import edu.brown.cs.uci.EngineCommands

import scala.collection.mutable


/**
 * @author ${user.name}
 */
object Main {
  val configs = scala.xml.XML.loadFile("config/config.xml") //TODO enforce schema and catch no file ex
  val botId = (configs \ "bot" \ "id").map(token => token.text)(0).toLowerCase()
  val server = (configs \ "environment").map(token => token.text)(0)
  val token = try{ //TODO RESET ALL TOKENS BEFORE PUBLISHING
    (configs \ "bot" \ "token").map(token => token.text)(0)
  } catch {
    case e: IndexOutOfBoundsException =>
      Console.println("No token found in configs, attempting to retrieve from env")
      System.getenv("TOKEN")
  }

  def main(args: Array[String]): Unit = {
    val ep = new LichessEndpoint(token, botId, server)
    EngineCommands.setEndpoint(ep)
    if(args.size == 0){ //default - run with Lichess bot only
      ChessLogger.info("Initializing in bot-only mode.")
      ep.streamEvents(Vector[String]())
      while(ep.isConnectionOpen()){

      }
    } else { // run with a REPL in terminal
      if(args(0).toLowerCase().equals("repl")){ //TODO slim these down
        val commandMap: mutable.HashMap[String, Vector[String] => Any] = mutable.HashMap(
          "help" -> GeneralCommands.hello,
          "upgrade" -> ep.upgradeToBot,
          "connect" -> ep.streamEvents,
        )
        val repl = new REPL(commandMap)
        repl.run()
      } else {
        ChessLogger.error("Unrecognized argument.")
      }
    }
  }
}
