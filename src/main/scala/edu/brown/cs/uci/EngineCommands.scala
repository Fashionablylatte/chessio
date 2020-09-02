package edu.brown.cs.uci

import java.io.{BufferedInputStream, BufferedReader, IOException, InputStreamReader, PipedInputStream, PipedOutputStream}
import java.nio.charset.{Charset, StandardCharsets}

import edu.brown.cs.chessgame.GameCommands
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
      val runtime = Runtime.getRuntime() //TODO still need this?
      val configs = scala.xml.XML.loadFile("config/config.xml") //TODO make a separate config for the engine?
      val engine = (configs \ "engine").map(token => token.text)(0)

      ChessLogger.info("Starting engine...")

      val process = Process(engine).#<(uciStream).#>(pipe).run()
      currentProcess = Some(process)
      Future{
        val reader = new BufferedReader(new InputStreamReader(outStream)) //TODO debug this pipe
        while(currentProcess.nonEmpty){
          val result = reader.readLine()
          ChessLogger.debug(result)
          processResult(result)
        } //TODO close all pipes as necessary. TODO link output to Lc endpoint
      }
    } catch {
      case e: Exception => ChessLogger.error(s"Error starting engine: \n ${e.printStackTrace()}")
        currentProcess = None
    }
  }

  private def processResult(str: String): Unit ={
    if(str.nonEmpty) {
      val arr = str.split(" ")
      arr(0) match {
        case "bestmove" =>
          val move = arr(1)
          val orig = arr.take(2).mkString("")
          val dest = arr.drop(2).take(2).mkString("")
          val prom = if (move.length == 5) move.drop(4).take(1).mkString("") else ""
          GameCommands.makeMove(Vector[String](orig, dest, prom)) //TODO bind incoming moves as well
        case "uciok" =>
        case "readyok" =>
        case "info" =>
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
}
