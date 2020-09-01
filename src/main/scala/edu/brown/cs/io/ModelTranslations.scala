package edu.brown.cs.io

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.DefaultFormats

/**
 * This class encapsulates the models for Lichess Event and Game stream objects and the methods for translating them
 * from strings to json objects.
 */
class ModelTranslations { //TODO add validation to translations?
  implicit val formats = DefaultFormats

  //Event stream models
  case class InboundEvent(`type`: Option[String], challenge: Option[Challenge], game: Option[GameEvent])
  def getInboundEvent(json: String): InboundEvent = {
    parse(json).extract[InboundEvent]
  }

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
  def getChallenge(json: String): Challenge = {
    parse(json).extract[Challenge]
  }

  case class EventPlayer(id: String,
                         name: String,
                         title: Option[String],
                         rating: Int,
                         provisional: Boolean,
                         online: Option[Boolean],
                         lag: Option[Int])
  def getEventPlayer(json: String): EventPlayer = {
    parse(json).extract[EventPlayer]
  }

  case class EventVariant(key: String, name: String, short: String)
  def getEventVariant(json: String): EventVariant = {
    parse(json).extract[EventVariant]
  }

  case class EventTimeControl(`type`: String, limit: Int, increment: Int, show: String)
  def getEventTimeControl(json: String): EventTimeControl = {
    parse(json).extract[EventTimeControl]
  }

  case class Perf(icon: String, name: String)
  def getPerf(json: String): Perf = {
    parse(json).extract[Perf]
  }

  case class GameEvent(id: String)
  def getGameEvent(json: String): GameEvent = {
    parse(json).extract[GameEvent]
  }

  //Game stream models
  case class GameFull(id: String,
                      variant: EventVariant,
                      clock: GameClock,
                      speed: String,
                      perf: GamePerf,
                      rated: Boolean,
                      createdAt: BigInt,
                      white: Option[EventPlayer],
                      black: Option[EventPlayer],
                      initialFen: String,
                      `type`: String,
                      state: GameEventState)
  def getGameFull(json: String): GameFull = {
    parse(json).extract[GameFull]
  }

  case class GameClock(initial: Int, increment: Int)
  def getGameClock(json: String): GameClock = {
    parse(json).extract[GameClock]
  }

  case class GamePerf(name: String)
  def getGamePerf(json: String): GamePerf = {
    parse(json).extract[GamePerf]
  }

  case class GameEventState(`type`: String,
                            moves: String,
                            wtime: Option[Int],
                            btime: Option[Int],
                            winc: Option[Int],
                            binc: Option[Int],
                            wdraw: Boolean,
                            bdraw: Boolean,
                            status: String)
  def getGameEventState(json: String): GameEventState = {
    parse(json).extract[GameEventState]
  }

  case class ChatLine(`type`: String, username: String, text: String, room: String)
  def getChatLine(json: String): ChatLine = {
    parse(json).extract[ChatLine]
  }


}
