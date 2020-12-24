package edu.brown.cs.io.uci

import java.io.{BufferedReader, InputStreamReader, PipedInputStream, PipedOutputStream}

import edu.brown.cs.io.endpoints.LichessEndpoint
import edu.brown.cs.io.logging.ChessLogger

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process._
import java.util.concurrent.Executors

import edu.brown.cs.config.UCIEngineConfig

import scala.collection.mutable.ArrayBuffer

/**
 * Holds all the commands for running an engine instance.
 */
object EngineCommands {

  private val uciStream = new UciStream
  private var currentProcess: Option[Process] = None

  implicit val ec = ExecutionContext.fromExecutorService(Executors.newWorkStealingPool(8))
  private var endpoint: LichessEndpoint = null
  private var engineConf = ""

  /**
   * Starts the chess engine specified in the config file.
   * @param args - a Vector of String arguments. Unused for this method, only exists for REPL compliance.
   */
  def startEngine(args: Vector[String]): Unit ={
    val outStream = new PipedInputStream
    val pipe = new PipedOutputStream(outStream)
    if(currentProcess.nonEmpty) {
      currentProcess.get.destroy()
      currentProcess = None
    }
    try {
      val (engine, options) = UCIEngineConfig.getEngineSettings(engineConf)

      options.foreach(p =>
        updateUciStream(Vector(s"setoption name ${p._1} value ${p._2}"))
      )

      ChessLogger.info("Starting engine...")

      val process = Process(engine).#<(uciStream).#>(pipe).run()
      currentProcess = Some(process)
      Future{
        ChessLogger.debug("Future split off")
        val reader = new BufferedReader(new InputStreamReader(outStream))
        while(currentProcess.nonEmpty){
          processResult(reader.readLine())
        }
        ChessLogger.info("Terminating engine.")
        reader.close()
      }
    } catch {
      case e: Exception => ChessLogger.error(s"Error starting engine: \n ${e.printStackTrace()}")
        currentProcess = None
    }
  }

  //processes a response from the engine instance.
  private def processResult(str: String): Unit ={
    if(str.nonEmpty) {
      val arr = str.split(" ")
      arr(0) match {
        case "bestmove" =>
          val move = arr(1)
          ChessLogger.info(str)
          if(endpoint != null && endpoint.isConnectionOpen()){
            endpoint.sendMove(move)
          }
        case _ =>
          ChessLogger.info(str)
      }
    }
  }

  /**
   * Stops the engine instance that is currently running.
   * @param args - a Vector of String arguments. Unused for this method, only exists for REPL compliance.
   */
  def stopEngine(args: Vector[String]): Unit ={
    if(currentProcess.nonEmpty){
      currentProcess.get.destroy()
      ChessLogger.info("Stopping engine...")
      currentProcess = None
    } else {
      ChessLogger.warn("No engine running currently")
    }
  }

  /**
   * Sends a command to the UCI stream that an engine instance listens to.
   * @param args - a Vector of String arguments. Unused for this method, only exists for REPL compliance.
   */
  def updateUciStream(args: Vector[String]): Unit ={
    val str = if(args.length > 0) args(0) else ""
    uciStream.append(str)
  }

  /**
   * Sets the endpoint that this engine instance communicates with.
   * @param ep - a LichessEndpoint.
   */
  def setEndpoint(ep: LichessEndpoint): Unit ={
    this.endpoint = ep
  }

  /**
   * Tells the engine to select a move.
   * @param fen - the starting position of the game.
   * @param moves - the moves that should be made to get to the current position.
   */
  def makeMove(fen: String, moves: String): Unit = {
    if(currentProcess.isEmpty){
      startEngine(Vector[String]())
    }
    if(fen.equals("startpos")){
      updateUciStream(Vector(s"position startpos moves $moves"))
    } else {
      updateUciStream(Vector(s"position fen $fen moves $moves"))
    }
    updateUciStream(Vector("go"))
    ChessLogger.trace("Sent request to engine.")
  }

  def setEngineConf(path: String): Unit ={
    this.engineConf = path
  }
}
