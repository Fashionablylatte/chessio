package edu.brown.cs.uci

import edu.brown.cs.io.ChessLogger
import scala.sys.process._

object EngineCommands {

  private var currentProcess: Option[Process] = None
  def startEngine(args: Vector[String]): Unit ={
    if(currentProcess.nonEmpty) {
      currentProcess.get.destroy()
      currentProcess = None
    }
    try {
      val runtime = Runtime.getRuntime()
      val engine = "engines/stockfish_20011801_x64.exe"

      ChessLogger.info("Starting engine...")

      val process = Process("./engines/stockfish_20011801_x64.exe").#<(System.in).run()
      currentProcess = Some(process)
    } catch {
      case e: Exception => ChessLogger.error(s"Error starting engine: \n ${e.printStackTrace()}")
        currentProcess = None
    }
  }

  def stopEngine(args: Vector[String]): Unit ={
    if(currentProcess.nonEmpty){
      currentProcess.get.destroy()
      ChessLogger.info("Stopping engine...")
      currentProcess = None
    } else {
      ChessLogger.warn("No engine running currently")
    }
  }
}
