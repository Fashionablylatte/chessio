package edu.brown.cs.io.endpoints

import java.util.concurrent.atomic.AtomicBoolean

import edu.brown.cs.io.logging.ChessLogger
import edu.brown.cs.io.uci.EngineCommands
import scalaj.http._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * An endpoint with methods for accessing Lichess.
 * @param token - the user access token permission for the bot. Needs to have bot and board permissions at minimum.
 * @param botId - the id of the bot. Note that this is all lowercase.
 * @param server - specifies if the bot is operating on the .dev or .org instance of Lichess.
 */
class LichessEndpoint(token: String, botId: String, server: String) extends ModelTranslations {

  private val configs = scala.xml.XML.loadFile("config/config.xml")
  private val DEFAULT_READ_TIMEOUT = try {
    (configs \ "read-timeout").map(ms => ms.text)(0).toInt
  } catch {
    case iobe: IndexOutOfBoundsException =>
      ChessLogger.warn("No value found, defaulting to 1800000ms read timeout.")
      1800000
    case nfe: NumberFormatException =>
      ChessLogger.warn("Unparseable value, defaulting to 1800000ms read timeout.")
      1800000
  }
  private val DEFAULT_CONNECT_TIMEOUT = try {
    (configs \ "conn-timeout").map(ms => ms.text)(0).toInt
  } catch {
    case iobe: IndexOutOfBoundsException =>
      ChessLogger.warn("No value found, defaulting to 30000ms connect timeout.")
      30000
    case nfe: NumberFormatException =>
      ChessLogger.warn("Unparseable value, defaulting to 30000ms connect timeout.")
      30000
  }

  private val variants: ArrayBuffer[String] = ArrayBuffer[String]()
  try {
    for{
      variant <- (configs \\ "variant").map(v => v.text.toLowerCase)
    } yield {
      variants.append(variant)
    }
  } catch {
    case _: Exception =>
      ChessLogger.warn("Error getting permissible variants, defaulting to Standard.")
      variants.append("standard")
  }

  private val untimed = try {
    (configs \\ "untimed").map(ut => ut.text)(0).toBoolean
  } catch {
    case iobe: IndexOutOfBoundsException =>
      ChessLogger.warn("No value found, defaulting to untimed games blocked.")
      false
    case nfe: IllegalArgumentException =>
      ChessLogger.warn("Unparseable value, defaulting to untimed games blocked.")
      false
  }

  private val timed = try {
    (configs \\ "timed").map(ut => ut.text)(0).toBoolean
  } catch {
    case iobe: IndexOutOfBoundsException =>
      ChessLogger.warn("No value found, defaulting to timed games allowed.")
      true
    case nfe: IllegalArgumentException =>
      ChessLogger.warn("Unparseable value, defaulting to timed games allowed.")
      true
  }

  private val timeMinimum = try {
    (configs \\ "min").map(min => min.text)(0).toInt * 60
  } catch {
    case iobe: IndexOutOfBoundsException =>
      ChessLogger.warn("No value found, defaulting to 30s minimum timecontrol.")
      30
    case nfe: NumberFormatException =>
      ChessLogger.warn("Unparseable value, defaulting to 30s minimum timecontrol.")
      30
  }

  private val timeMaximum = try {
    (configs \\ "max").map(max => max.text)(0).toInt * 60
  } catch {
    case iobe: IndexOutOfBoundsException =>
      ChessLogger.warn("No value found, defaulting to 18000mins maximum timecontrol.")
      18000
    case nfe: NumberFormatException =>
      ChessLogger.warn("Unparseable value, defaulting to 18000mins maximum timecontrol.")
      18000
  }

  private val incMinimum = try {
    (configs \\ "inc-min").map(incMin => incMin.text)(0).toInt
  } catch {
    case iobe: IndexOutOfBoundsException =>
      ChessLogger.warn("No value found, defaulting to 0s minimum increment.")
      0
    case nfe: NumberFormatException =>
      ChessLogger.warn("Unparseable value, defaulting to 0s minimum increment.")
      0
  }

  private val incMaximum = try {
    (configs \\ "inc-max").map(incMax => incMax.text)(0).toInt
  } catch {
    case iobe: IndexOutOfBoundsException =>
      ChessLogger.warn("No value found, defaulting to 120s minimum increment.")
      120
    case nfe: NumberFormatException =>
      ChessLogger.warn("Unparseable value, defaulting to 120s minimum increment.")
      120
  }

  private val rated = try {
    (configs \ "rated").map(rt => rt.text)(0).toBoolean
  } catch {
    case iobe: IndexOutOfBoundsException =>
      ChessLogger.warn("No value found, defaulting to rated games allowed.")
      true
    case nfe: IllegalArgumentException =>
      ChessLogger.warn("Unparseable value, defaulting to rated games allowed.")
      true
  }

  private val casual = try {
    (configs \ "casual").map(rt => rt.text)(0).toBoolean
  } catch {
    case iobe: IndexOutOfBoundsException =>
      ChessLogger.warn("No value found, defaulting to unrated games allowed.")
      true
    case nfe: IllegalArgumentException =>
      ChessLogger.warn("Unparseable value, defaulting to unrated games allowed.")
      true
  }

  private val activeGame : AtomicBoolean = new AtomicBoolean(false)
  private val joinedGame : AtomicBoolean = new AtomicBoolean(false)
  private val connectionOpen : AtomicBoolean = new AtomicBoolean(false)
  private var opponentColor = "white"
  private var initialFen = "startpos"
  private var gameId : String = ""

  implicit val ec = ExecutionContext.global

  /**
   * Upgrades the account to bot status. THIS IS NOT REVERSIBLE! Note that only brand-new accounts with no game history
   * can be converted this way.
   * @param args - a String Vector of arguments - this method does not use arguments, but requires an (empty) args Vector to
   *             comply with the REPL method signature.
   */
  def upgradeToBot(args: Vector[String]): Unit ={
    val response: HttpResponse[String] = Http(s"${server}/api/bot/account/upgrade").header("Authorization", s"Bearer $token").postForm.asString
    response.code match {
      case 200 => ChessLogger.info(s"Success: ${response.body}")
      case 429 => ChessLogger.warn("Rate limit"); Thread.sleep(60000)
      case 401 => ChessLogger.fatal("unauthorized - check id and token!"); System.exit(1)
      case _ => ChessLogger.error(s"Error: ${response.body}")
    }
  }

  /**
   * Streams the events the bot account receives.
   * @param args - a String Vector of arguments - this method does not use arguments, but requires an (empty) args Vector to
   *             comply with the REPL method signature.
   */
  def streamEvents(args: Vector[String]): Unit ={
    Future {
      connectionOpen.set(true)
      while(!joinedGame.get){
        try{
          Http(s"${server}/api/stream/event").timeout(DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT).header("Authorization", s"Bearer $token").execute(
            strm => {
              scala.io.Source.fromInputStream(strm).getLines().foreach(processEvent)
            }).code match {
            case 200 => ChessLogger.debug("event received OK")
            case 429 => ChessLogger.warn("rate limit req"); Thread.sleep(60000)
            case 401 => ChessLogger.fatal("unauthorized - check id and token!"); System.exit(1)
            case _ => ChessLogger.error("error - other status code"); Thread.sleep(60000)
          }
        } catch {
          case ex: Exception => ChessLogger.error("Event Processing Error: " + ex.getMessage); Thread.sleep(60000)
        }
      }
    } onComplete {
      case Success(r) =>
        ChessLogger.info("Event stream shut down normally.")
        connectionOpen.set(false)
      case Failure(ex) =>
        ChessLogger.error("Stream closed due to error: "  + ex.getMessage)
        connectionOpen.set(false)
    }
  }

  /**
   * Checks if there is an open connection to Lichess.
   * @return a Boolean indicating if there is an open connection.
   */
  def isConnectionOpen(): Boolean ={
    connectionOpen.get()
  }

  //Processes an event string from the event stream, converting it to a json object to be further processed later.
  private def processEvent(str: String): Unit ={
    if(!str.isBlank) {
      ChessLogger.debug(str)
      val event = getInboundEvent(str)
      event.`type` match {
        case Some("challenge") => ChessLogger.debug(s"received challenge ${event.challenge.getOrElse("ex get chal")}"); processChallenge(event.challenge.get)
        case Some("gameStart") => ChessLogger.debug("game started"); processGame(event.game.get.id)
        case Some("gameFinish") => ChessLogger.debug("game finished"); resetEndpoint();
        case Some("gameAborted") => ChessLogger.debug("game aborted"); resetEndpoint();
        case _ => ChessLogger.warn("event not recognized")
      }
    }
  }

  //resets the game status. Used after a game ends.
  private def resetEndpoint(): Unit ={
    activeGame.set(false)
    joinedGame.set(false)
  }

  //checks if a challenge is acceptable. Accepting unlimited time games is not advisable, but hey it's your bot.
  private def screenChallenge(chal: Challenge): Boolean ={
    variants.contains(chal.variant.key.toLowerCase()) &&
      (((chal.timeControl.`type`.equals("clock") && timed) &&
      (chal.timeControl.limit.get <= timeMaximum && chal.timeControl.limit.get >= timeMinimum) &&
      (chal.timeControl.increment.get <= incMaximum && chal.timeControl.increment.get >= incMinimum)) ||
        (chal.timeControl.`type`.equals("unlimited") && untimed)) &&
      ((chal.rated && rated) || ((!chal.rated) && casual))
  }

  //process a challenge.
  private def processChallenge(chal: Challenge): Unit ={
    ChessLogger.trace("processing challenge")
    ChessLogger.trace(chal.challenger.toString())
    if(screenChallenge(chal)){
      if(!activeGame.getAndSet(true)){
        Http(s"${server}/api/challenge/${chal.id}/accept").header("Authorization", s"Bearer $token").postForm.asString.code match {
          case 200 => ChessLogger.debug("accepted challenge")
          case 404 => ChessLogger.warn("challenge not found"); activeGame.set(false)
          case 429 => ChessLogger.warn("Rate limit"); Thread.sleep(60000)
          case 401 => ChessLogger.fatal("unauthorized - check id and token!"); System.exit(1)
          case _ => ChessLogger.error("error accepting challenge"); activeGame.set(false)
        }
      }
    } else {
      Http(s"${server}/api/challenge/${chal.id}/decline").header("Authorization", s"Bearer $token").postForm.asString.code match {
        case 200 => ChessLogger.debug("declined challenge")
        case 404 => ChessLogger.warn("no challenge to decline")
        case 429 => ChessLogger.warn("Rate limit"); Thread.sleep(60000)
        case 401 => ChessLogger.fatal("unauthorized - check id and token!"); System.exit(1)
        case _ => ChessLogger.error("error declining challenge")
      }
    }
  }

  //processes a game.
  private def processGame(gameId: String): Unit ={
    ChessLogger.info(s"joining game $gameId")
    activeGame.set(true)
    joinedGame.set(true)
    this.gameId = gameId
    while(activeGame.get()){
      try{
        Http(s"${server}/api/bot/game/stream/${gameId}").timeout(DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT).header("Authorization", s"Bearer $token").execute(
          strm => {
            scala.io.Source.fromInputStream(strm).getLines().foreach(processGameState)
          }).code match {
          case 200 => ChessLogger.debug("event received OK")
          case 429 => ChessLogger.warn("rate limit req"); Thread.sleep(60000)
          case 401 => ChessLogger.fatal("unauthorized - check id and token!"); System.exit(1)
          case _ => ChessLogger.error("error - other status code"); Thread.sleep(60000)
        }
      } catch {
        case ex: Exception => ChessLogger.error("Game Processing Error: " + ex.getMessage)
      }
    }
    ChessLogger.debug("Game stream closed normally")
  }

  //processes
  private def processGameState(state: String): Unit ={
    if(!state.isBlank) {
      ChessLogger.debug(state)
      try {
        val st: Either[GameFull, GameEventState] = try {
          Left(getGameFull(state))
        } catch {
          case _: Throwable => Right(getGameEventState(state))
        }

        st match {
          case Left(gf) => ChessLogger.debug(gf.toString); processFullGame(gf)
          case Right(ges) => ChessLogger.debug(ges.toString); processGameEventState(ges)
        }
      } catch {
        case _: Throwable => ChessLogger.debug("chat or unknown game state")
      }
    }
  }

  //processes a full game, setting the sides as appropriate.
  private def processFullGame(gf: GameFull): Unit ={
    ChessLogger.debug(if(gf.white.nonEmpty) gf.white.get.id.getOrElse("") else "anon" + " opp")
    opponentColor = if((if(gf.white.nonEmpty) gf.white.get.id.getOrElse("") else "anon").equals(botId)) "black" else "white"
    gf.initialFen match {
      case s : String =>
        initialFen = s
        processGameEventState(gf.state)
      case _ => ChessLogger.error("invalid fen received")
    }
  }

  //processes a game state.
  private def processGameEventState(ges: GameEventState): Unit ={
    if(ges.status.equals("started")) {
      val moveCount = if(ges.moves.isEmpty) 0 else ges.moves.count(p => p == 32) + 1
      ChessLogger.trace(s"Number of moves: $moveCount")
      val whoseTurn = moveCount % 2
      ChessLogger.trace(s"turn $whoseTurn, opp is $opponentColor")
      if (whoseTurn == 1) {
        ChessLogger.trace("turn of 1")
        if (opponentColor.equals("white")) {
          EngineCommands.makeMove(initialFen, ges.moves)
        }
      } else {
        ChessLogger.trace("turn of 0")
        if (opponentColor.equals("black")) {
          EngineCommands.makeMove(initialFen, ges.moves)
        }
      }
    } else {
      ChessLogger.debug(s"Game ${ges.status}")
      resetEndpoint()
    }
  }

  /**
   * Sends a move in UCI format to the Lichess server.
   * @param uci - a chess move in UCI format.
   */
  def sendMove(uci: String){
    val response: HttpResponse[String] = Http(s"${server}/api/bot/game/${gameId}/move/${uci}").header("Authorization", s"Bearer $token").postForm.asString
    response.code match {
      case 200 => ChessLogger.debug(s"Move Success: ${response.body}")
      case 429 => ChessLogger.warn("Rate limit"); Thread.sleep(60000); sendMove(uci)
      case 401 => ChessLogger.fatal("unauthorized - check id and token!"); System.exit(1)
      case _ => ChessLogger.error(s"Move Error: ${response.body}")
    }
  }
}
