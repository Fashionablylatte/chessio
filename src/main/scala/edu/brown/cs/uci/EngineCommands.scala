package edu.brown.cs.uci

import java.io.{BufferedInputStream, BufferedReader, IOException, InputStreamReader, PipedInputStream, PipedOutputStream}
import java.nio.charset.{Charset, StandardCharsets}

import edu.brown.cs.io.ChessLogger

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process._

object EngineCommands {

  private val uciStream = new UciStream
  private val outStream = new PipedInputStream
  private val pipe = new PipedOutputStream(outStream)
  private var currentProcess: Option[Process] = None
  implicit val ec = ExecutionContext.global

  def startEngine(args: Vector[String]): Unit ={
    if(currentProcess.nonEmpty) {
      currentProcess.get.destroy()
      currentProcess = None
    }
    try {
      val runtime = Runtime.getRuntime()
      val engine = "engines/stockfish_20011801_x64.exe"

      ChessLogger.info("Starting engine...")

      val process = Process("./engines/stockfish_20011801_x64.exe").#<(uciStream).#>(pipe).run()
      currentProcess = Some(process)
      Future{
        val reader = new BufferedReader(new InputStreamReader(outStream))
        while(currentProcess.nonEmpty){
          ChessLogger.info(reader.readLine())
        } //TODO close all pipes as necessary. TODO link output to Lc endpoint
      }
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

  def updateUciStream(args: Vector[String]): Unit ={
    val str = if(args.length > 0) args(0) else ""
    uciStream.append(str)
  }
}
