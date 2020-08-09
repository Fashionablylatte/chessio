package edu.brown.cs.io.lichess

import java.util.concurrent.atomic.AtomicBoolean

import scala.io.Source
import scalaj.http._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.DefaultFormats

import scala.concurrent.{ExecutionContext, Future}
import java.util.concurrent.{BlockingQueue, Executors, LinkedBlockingQueue}

import edu.brown.cs.chessgame.{GameCommands, GameState}

import scala.util.{Failure, Success, Try}
import chess.Move
import edu.brown.cs.engine.{AlphaBeta, AlphaBetaEngine}

class LichessEndpoint {
  val oauth = "config/token.conf"
  val bufferedSource = Source.fromFile(oauth)
  val token = bufferedSource.getLines.nextOption().getOrElse("")
  bufferedSource.close
  val botId = "fltestaccountbot"
  val server = "https://lichess.dev"

  val DEFAULT_READ_TIMEOUT = 1800000
  val DEFAULT_CONNECT_TIMEOUT = 30000

  val activeGame : AtomicBoolean = new AtomicBoolean(false)
  val joinedGame : AtomicBoolean = new AtomicBoolean(false)
  val gameStateExists : AtomicBoolean = new AtomicBoolean(false)
  private var opponentColor = "white"
  val eventQueue = new LinkedBlockingQueue[String]()
  var gameState : GameState = null
  var gameId : String = ""
  var engine : AlphaBetaEngine = null;

  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(8))
  implicit val formats = DefaultFormats

  case class InboundEvent(`type`: Option[String], challenge: Option[Challenge], game: Option[GameEvent])
  case class Challenge(id: String,
                       url: Option[String],
                       status: String,
                       challenger: EventPlayer,
                       destUser: EventPlayer,
                       variant: EventVariant,
                       rated: Boolean,
                       speed: String,
                       timeControl: EventTimeControl,
                       color: String,
                       perf: Perf){
    override def toString = s"Challenge from $challenger for ${variant.name}"
  }
  case class EventPlayer(id: String, name: String, title: Option[String], rating: Int, provisional: Boolean, online: Option[Boolean], lag: Option[Int])
  case class EventVariant(key: String, name: String, short: String)
  case class EventTimeControl(`type`: String, limit: Int, increment: Int, show: String)
  case class Perf(icon: String, name: String)
  case class GameEvent(id: String)

  def upgradeToBot(args: Vector[String]): Unit ={
    val response: HttpResponse[String] = Http(s"${server}/api/bot/account/upgrade").header("Authorization", s"Bearer $token").postForm.asString
    response.code match {
      case 200 => println(s"Success: ${response.body}")
      case 429 => println("Rate limit"); Thread.sleep(60000)
      case _ => println(s"Error: ${response.body}")
    }
  }

  def streamEvents(args: Vector[String]): Unit ={
    Future {
      while(!joinedGame.get){
        try{
          Http(s"${server}/api/stream/event").timeout(DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT).header("Authorization", s"Bearer $token").execute(
            strm => {
              scala.io.Source.fromInputStream(strm).getLines().foreach(processEvent)
            }).code match {
            case 200 => println("event received OK")
            case 429 => println("rate limit req"); Thread.sleep(60000)
            case _ => println("error - other status code")
          }
        } catch {
          case ex: Exception => println("Event Processing Error: " + ex.getMessage); ex.printStackTrace()
        }
      }
      "Event stream closed normally"
    } onComplete {
      case Success(r) => println(r)
      case Failure(ex) => println("Event Future error: "  + ex.getMessage)
    }
  }

  private def processEvent(str: String): Unit ={
    if(!str.isBlank) {
      println(str)
      val event = parse(str).extract[InboundEvent]
      event.`type` match {
        case Some("challenge") => println(s"received challenge ${event.challenge.getOrElse("ex get chal")}"); processChallenge(event.challenge.get)
        case Some("gameStart") => println("game started"); processGame(event.game.get.id)
        case _ => println("event not recognized")
      }
    }
  }

  private def processChallenge(chal: Challenge): Unit ={ //TODO handle multiple challenges
    println("processing challenge")
    println(chal.challenger)
    if(chal.variant.key.equals("standard") && chal.timeControl.`type`.equals("clock") && chal.timeControl.limit >= 900){
      if(!activeGame.getAndSet(true)){
        Http(s"${server}/api/challenge/${chal.id}/accept").header("Authorization", s"Bearer $token").postForm.asString.code match {
          case 200 => println("accepted challenge")
          case 404 => println("challenge not found"); activeGame.set(false)
          case 429 => println("Rate limit"); Thread.sleep(60000)
          case _ => println("error accepting challenge"); activeGame.set(false)
        }
      }
    } else {
      Http(s"${server}/api/challenge/${chal.id}/decline").header("Authorization", s"Bearer $token").postForm.asString.code match {
        case 200 => println("declined challenge")
        case 404 => println("no challenge to decline")
        case 429 => println("Rate limit"); Thread.sleep(60000)
        case _ => println("error declining challenge")
      }
    }
  }

  private def processGame(gameId: String): Unit ={
    println(s"joining game $gameId")
    activeGame.set(true)
    joinedGame.set(true)
    this.gameId = gameId
    while(activeGame.get()){
      try{
        Http(s"${server}/api/bot/game/stream/${gameId}").timeout(DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT).header("Authorization", s"Bearer $token").execute(
          strm => {
            scala.io.Source.fromInputStream(strm).getLines().foreach(processGameState)
          }).code match {
          case 200 => println("event received OK")
          case 429 => println("rate limit req"); Thread.sleep(60000)
          case _ => println("error - other status code")
        }
      } catch {
        case ex: Exception => println("Game Processing Error: " + ex.getMessage)
      }
    }
    println("Game stream closed normally")
  }

  case class GameFull(id: String, variant: EventVariant, clock: GameClock,
                      speed: String, perf: GamePerf, rated: Boolean, createdAt: BigInt,
                      white: EventPlayer, black: EventPlayer, initialFen: String,
                      `type`: String, state: GameEventState)
  case class GameClock(initial: Int, increment: Int)
  case class GamePerf(name: String)
  case class GameEventState(`type`: String, moves: String, wtime: Option[Int], btime: Option[Int],
                            winc: Option[Int], binc: Option[Int], wdraw: Boolean, bdraw: Boolean, status: String)
  case class ChatLine(`type`: String, username: String, text: String, room: String)

  private def processGameState(state: String): Unit ={
    if(!state.isBlank) {
      try {
        val st: Either[GameFull, GameEventState] = try {
          Left(parse(state).extract[GameFull])
        } catch {
          case _ => Right(parse(state).extract[GameEventState])
        }

        st match {
          case Left(gf) => println(gf); processFullGame(gf)
          case Right(ges) => println(ges); processGameEventState(ges)
        }
      } catch {
        case _ => println("chat or unknown game state")
      }
    }
  }

  private def processFullGame(gf: GameFull): Unit ={
    if(!gameStateExists.get){
      println(gf.white.id + " opp")
      opponentColor = if(gf.white.id.equals(botId)) "black" else "white"
      gf.initialFen match {
        case "startpos" =>
          gameState = GameCommands.startGame(Vector(opponentColor), Some(this)).get
          engine = new AlphaBetaEngine(gameState, !opponentColor.equals("white"))
          gameStateExists.set(true)
        case s : String =>
          gameState = GameCommands.startGame(Vector(opponentColor, s), Some(this)).get
          engine = new AlphaBetaEngine(gameState, !opponentColor.equals("white"))
          gameStateExists.set(true)
        case _ => println("invalid fen received")
      }
    }
    if(!gf.state.status.equals("started")){
      println(s"Game ${gf.state.status}")
      joinedGame.set(false)
      activeGame.set(false)
    } else {
      processGameEventState(gf.state)
    }
  }

  private def processGameEventState(ges: GameEventState): Unit ={
    val moves = ges.moves.split(" ")
    val whoseTurn = moves.length % 2
    lazy val move = moves.last
    lazy val from = move.take(2)
    lazy val to = move.drop(2).take(2)
    lazy val prom = move.drop(4).take(1)
    println(s"turn $whoseTurn, opp is $opponentColor")
    if(whoseTurn == 1){
      println("turn of 1")
      if(opponentColor.equals("white")){
        println(s"adding opp (W) move $from $to $prom")
        if(gameState.makeMove(from, to, prom).nonEmpty){
          engine.makeMove()
        }
      }
    } else {
      println("turn of 0")
      if(opponentColor.equals("black")){
        println(s"adding opp (B) move $from $to $prom")
        if(gameState.makeMove(from, to, prom).nonEmpty){
          engine.makeMove()
        }
      }
    }
  }

  def sendMove(move: Move){
    lazy val uci = move.toUci.uci
    if(!move.situationBefore.color.name.equals(opponentColor)){
      val response: HttpResponse[String] = Http(s"${server}/api/bot/game/${gameId}/move/${uci}").header("Authorization", s"Bearer $token").postForm.asString
      response.code match {
        case 200 => println(s"Move Success: ${response.body}")
        case 429 => println("Rate limit"); Thread.sleep(60000); sendMove(move)
        case _ => println(s"Move Error: ${response.body}")
      }
    }
  }
}
