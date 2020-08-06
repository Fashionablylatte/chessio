package edu.brown.cs.io.endgames

import chess.format.Uci
import scalaj.http._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.DefaultFormats

object TablebaseEndpoint {
  implicit val formats = DefaultFormats

  case class MainlineResponse(dtz: Int, mainline: List[MainlineMove], winner: String)
  case class MainlineMove(uci: String, dtz: Int)

  private def sendQuery(fen: String, attempt: Int): Option[JValue] = {
    if (attempt > 2) {
      None
    } else {
      val response = Http("http://tablebase.lichess.ovh/standard/mainline").param("fen", fen).asString
      response.code match {
        case 429 => Console.println("429 - sleeping for 1 minute."); Thread.sleep(60000); sendQuery(fen, attempt + 1)
        case 200 => Some(parse(response.body))
        case _ => Console.err.println("Failed tablebase query"); None
      }
    }
  }

  def mainlineQuery(fen: String): Option[Uci.Move] = {
    sendQuery(fen, 0) match {
      case None => None
      case Some(jval) =>
        val resp = jval.extract[MainlineResponse]
        if(resp.mainline.isEmpty) None else Uci.Move(resp.mainline(0).uci)
    }
  }
}
