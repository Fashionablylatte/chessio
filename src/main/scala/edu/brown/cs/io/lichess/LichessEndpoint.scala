package edu.brown.cs.io.lichess

import java.util.concurrent.atomic.AtomicBoolean

import scalaj.http._

import scala.concurrent.{ExecutionContext, Future}
import edu.brown.cs.chessgame.{GameCommands, GameState}

import scala.util.{Failure, Success}
import chess.Move
import edu.brown.cs.engine.{AlphaBetaEngine, ExternalEngine}
import edu.brown.cs.io.{ChessLogger, ModelTranslations}

/**
 * An endpoint with methods for accessing Lichess.
 * @param token - the user access token permission for the bot. Needs to have bot and board permissions at minimum.
 * @param botId - the id of the bot. Note that this is all lowercase.
 * @param server - specifies if the bot is operating on the .dev or .org instance of Lichess.
 */
class LichessEndpoint(token: String, botId: String, server: String) extends ModelTranslations {

  //TODO might set these in config.xml instead?
  val DEFAULT_READ_TIMEOUT = 1800000
  val DEFAULT_CONNECT_TIMEOUT = 30000

  private val activeGame : AtomicBoolean = new AtomicBoolean(false)
  private val joinedGame : AtomicBoolean = new AtomicBoolean(false)
  private val gameStateExists : AtomicBoolean = new AtomicBoolean(false) //TODO refactor to be stateless and UCI compliant
  private val connectionOpen : AtomicBoolean = new AtomicBoolean(true)
  private var opponentColor = "white"
  private var gameState : GameState = null
  private var gameId : String = ""
  private var engine : ExternalEngine = null;

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
      while(!joinedGame.get){
        try{
          Http(s"${server}/api/stream/event").timeout(DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT).header("Authorization", s"Bearer $token").execute(
            strm => {
              scala.io.Source.fromInputStream(strm).getLines().foreach(processEvent)
            }).code match {
            case 200 => ChessLogger.debug("event received OK")
            case 429 => ChessLogger.warn("rate limit req"); Thread.sleep(60000)
            case _ => ChessLogger.error("error - other status code")
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
  //TODO fix starting as white engine
  private def processEvent(str: String): Unit ={
    if(!str.isBlank) {
      ChessLogger.debug(str)
      val event = getInboundEvent(str)
      event.`type` match { //TODO catch exceptions - mobile breaks this for some reason
        case Some("challenge") => ChessLogger.debug(s"received challenge ${event.challenge.getOrElse("ex get chal")}"); processChallenge(event.challenge.get)
        case Some("gameStart") => ChessLogger.debug("game started"); processGame(event.game.get.id)
        case Some("gameFinish") => ChessLogger.debug("game finished"); resetEndpoint();
        case Some("gameAborted") => ChessLogger.debug("game aborted"); resetEndpoint(); //TODO
        case _ => ChessLogger.warn("event not recognized")
      }
    }
  }

  private def resetEndpoint(): Unit ={
    activeGame.set(false)
    joinedGame.set(false)
    gameStateExists.set(false)
  }

  private def processChallenge(chal: Challenge): Unit ={ //TODO handle multiple challenges
    ChessLogger.trace("processing challenge")
    ChessLogger.trace(chal.challenger.toString())
    if(chal.variant.key.equals("standard") && chal.timeControl.`type`.equals("clock") && chal.timeControl.limit >= 900){
      if(!activeGame.getAndSet(true)){
        Http(s"${server}/api/challenge/${chal.id}/accept").header("Authorization", s"Bearer $token").postForm.asString.code match {
          case 200 => ChessLogger.debug("accepted challenge")
          case 404 => ChessLogger.warn("challenge not found"); activeGame.set(false)
          case 429 => ChessLogger.warn("Rate limit"); Thread.sleep(60000)
          case _ => ChessLogger.error("error accepting challenge"); activeGame.set(false)
        }
      }
    } else {
      Http(s"${server}/api/challenge/${chal.id}/decline").header("Authorization", s"Bearer $token").postForm.asString.code match {
        case 200 => ChessLogger.debug("declined challenge")
        case 404 => ChessLogger.warn("no challenge to decline")
        case 429 => ChessLogger.warn("Rate limit"); Thread.sleep(60000)
        case _ => ChessLogger.error("error declining challenge")
      }
    }
  }

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
          case _ => ChessLogger.error("error - other status code")
        }
      } catch {
        case ex: Exception => ChessLogger.error("Game Processing Error: " + ex.getMessage)
      }
    }
    ChessLogger.debug("Game stream closed normally")
  }

  private def processGameState(state: String): Unit ={
    if(!state.isBlank) {
      println(state)
      try {
        val st: Either[GameFull, GameEventState] = try {
          Left(getGameFull(state))
        } catch {
          case _: Throwable => Right(getGameEventState(state))
        }

        st match {
          case Left(gf) => println(gf); processFullGame(gf)
          case Right(ges) => println(ges); processGameEventState(ges)
        }
      } catch {
        case _: Throwable => ChessLogger.debug("chat or unknown game state")
      }
    }
  }

  private def processFullGame(gf: GameFull): Unit ={
    if(!gameStateExists.get){
      ChessLogger.debug(if(gf.white.nonEmpty) gf.white.get.id.getOrElse("") else "anon" + " opp")
      opponentColor = if((if(gf.white.nonEmpty) gf.white.get.id else "anon").equals(botId)) "black" else "white"
      gf.initialFen match {
        case "startpos" =>
          gameState = GameCommands.startGame(Vector(opponentColor), Some(this)).get
          engine = new ExternalEngine(gameState, !opponentColor.equals("white"))
          gameStateExists.set(true)
        case s : String =>
          gameState = GameCommands.startGame(Vector(opponentColor, s), Some(this)).get
          engine = new ExternalEngine(gameState, !opponentColor.equals("white"))
          gameStateExists.set(true)
        case _ => ChessLogger.error("invalid fen received")
      }
    }
  }

  private def processGameEventState(ges: GameEventState): Unit ={
    if(ges.status.equals("started")) {
      val moves = ges.moves.split(" ")
      val whoseTurn = moves.length % 2
      lazy val move = moves.last
      lazy val from = move.take(2)
      lazy val to = move.drop(2).take(2)
      lazy val prom = move.drop(4).take(1)
      ChessLogger.trace(s"turn $whoseTurn, opp is $opponentColor")
      if (whoseTurn == 1) {
        ChessLogger.trace("turn of 1")
        if (opponentColor.equals("white")) {
          ChessLogger.trace(s"adding opp (W) move $from $to $prom")
          if (gameState.makeMove(from, to, prom).nonEmpty) {
            engine.makeMove()
          }
        }
      } else {
        ChessLogger.trace("turn of 0")
        if (opponentColor.equals("black")) {
          ChessLogger.trace(s"adding opp (B) move $from $to $prom")
          if (gameState.makeMove(from, to, prom).nonEmpty) {
            engine.makeMove()
          }
        }
      }
    } else {
      ChessLogger.debug(s"Game ${ges.status}")
      resetEndpoint()
    }
  }

  def sendMove(move: Move){
    lazy val uci = move.toUci.uci
    if(!move.situationBefore.color.name.equals(opponentColor)){
      val response: HttpResponse[String] = Http(s"${server}/api/bot/game/${gameId}/move/${uci}").header("Authorization", s"Bearer $token").postForm.asString
      response.code match {
        case 200 => ChessLogger.debug(s"Move Success: ${response.body}")
        case 429 => ChessLogger.warn("Rate limit"); Thread.sleep(60000); sendMove(move)
        case _ => ChessLogger.error(s"Move Error: ${response.body}")
      }
    }
  }
}
