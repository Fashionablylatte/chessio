package edu.brown.cs.io.endpoints

import chess.format.Uci
import org.json4s.native.JsonMethods.parse
import org.json4s.{DefaultFormats, JValue}
import scalaj.http.Http

object OpeningEndpoint {
  implicit val formats = DefaultFormats

  case class Response(white: Int, draws: Int, black: Int, averageRating: Int, moves: List[RMove], topGames: List[TGame])
  case class RMove(uci: String, san: String, white: Int, draws: Int, black: Int, averageRating: Int)
  case class TGame(id: String, winner: String, white: TWhite, black: TBlack, year: Int, speed: String)
  case class TWhite(name: String, rating: Int)
  case class TBlack(name: String, rating: Int)

  private def sendQuery(fen: String, attempt: Int): Option[JValue] = { //TODO make the retry attempts in config.xml
    if (attempt > 2) {
      None
    } else {
      val response = Http("https://explorer.lichess.ovh/master").params(("fen", fen), ("moves", "1"), ("topGames", "0")).asString
      response.code match {
        case 429 => Console.println("429 - sleeping for 1 minute."); Thread.sleep(60000); sendQuery(fen, attempt + 1)
        case 200 => Some(parse(response.body))
        case _ => Console.err.println("Failed opening query"); None
      }
    }
  }

  def openingQuery(fen: String): Option[String] = {
    sendQuery(fen, 0) match {
      case None => None
      case Some(jval) =>
        val resp = jval.extract[Response]
        if(resp.moves.isEmpty) None else Some(resp.moves(0).uci) //TODO make this selection configurable
    }
  }
}
