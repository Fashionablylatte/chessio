package edu.brown.cs.uci

import java.io.{BufferedInputStream, BufferedReader, IOException, InputStreamReader, PipedInputStream, PipedOutputStream}

import edu.brown.cs.io.ChessLogger
import edu.brown.cs.io.endpoints.LichessEndpoint

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process._

object EngineCommands {

  private val uciStream = new UciStream
  private val outStream = new PipedInputStream
  private val pipe = new PipedOutputStream(outStream)
  private var currentProcess: Option[Process] = None
//  implicit val ec = ExecutionContext.global
  import scala.concurrent.ExecutionContext
  import java.util.concurrent.Executors
  implicit val ec = ExecutionContext.fromExecutorService(Executors.newWorkStealingPool(8))
  private var endpoint: LichessEndpoint = null

  def startEngine(args: Vector[String]): Unit ={
    if(currentProcess.nonEmpty) {
      currentProcess.get.destroy()
      currentProcess = None
    }
    try {
      val runtime = Runtime.getRuntime() //TODO still need this?
      val configs = scala.xml.XML.loadFile("config/config.xml") //TODO make a separate config for the engine?
      val engine = (configs \ "engine").map(token => token.text)(0)

      ChessLogger.info("Starting engine...")

      val process = Process(engine).#<(uciStream).#>(pipe).run()
      currentProcess = Some(process)
      Future{
        ChessLogger.debug("Future split off")
        val reader = new BufferedReader(new InputStreamReader(outStream)) //TODO debug this pipe
        while(currentProcess.nonEmpty){
          processResult(reader.readLine())
        } //TODO close all pipes as necessary. TODO link output to Lc endpoint
        ChessLogger.info("Terminating engine.")
      }
    } catch {
      case e: Exception => ChessLogger.error(s"Error starting engine: \n ${e.printStackTrace()}")
        currentProcess = None
    }
  }

  private def processResult(str: String): Unit ={
    ChessLogger.debug(str)
    if(str.nonEmpty) {
      val arr = str.split(" ")
      arr(0) match {
        case "bestmove" =>
          val move = arr(1)
          ChessLogger.debug(move)
          endpoint.sendMove(move)
        case _ =>
//        case "uciok" => ChessLogger.debug("case 2")
//        case "readyok" => ChessLogger.debug("case 3")
//        case "info" => ChessLogger.debug("case 4")
//        case _ => ChessLogger.debug("case 5") //TODO see if I want to handle this
      }
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

  def updateUciStream(args: Vector[String]): Unit ={
    val str = if(args.length > 0) args(0) else ""
    uciStream.append(str)
  }

  def setEndpoint(ep: LichessEndpoint): Unit ={
    this.endpoint = ep
  }

  def makeMove(fen: String, moves: String): Unit = { //TODO configure options for blocking table lookup
    if(currentProcess.isEmpty){
      startEngine(Vector[String]())
      updateUciStream(Vector("uci"))
    }
    if(fen.equals("startpos")){
      updateUciStream(Vector(s"position startpos moves $moves"))
    } else {
      updateUciStream(Vector(s"position fen $fen moves $moves"))
    }
    updateUciStream(Vector("go"))
    ChessLogger.trace("Sent request to external engine.") //TODO reconfigure
  }
}
