package edu.brown.cs.io.endpoints

import org.json4s.{DefaultFormats, _}
import org.json4s.native.JsonMethods._

/**
 * This class encapsulates the models for Lichess Event and Game stream objects and the methods for translating them
 * from strings to json objects.
 */
class ModelTranslations { //TODO add validation to translations? //TODO add options to basically everything
  implicit val formats = DefaultFormats

  //Event stream models
  /**
   * Translates an InboundEvent from Lichess.
   * @param json - the json string representation of an event.
   * @return an InboundEvent object.
   */
  def getInboundEvent(json: String): InboundEvent = {
    parse(json).extract[InboundEvent]
  }
  case class InboundEvent(`type`: Option[String], challenge: Option[Challenge], game: Option[GameEvent])

  /**
   * Translates a Challenge from Lichess.
   * @param json - the json string representation of a challenge.
   * @return a Challenge object.
   */
  def getChallenge(json: String): Challenge = {
    parse(json).extract[Challenge]
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

  /**
   * Translates an EventPlayer from Lichess.
   * @param json - the json string representation of a player.
   * @return an EventPlayer object.
   */
  def getEventPlayer(json: String): EventPlayer = {
    parse(json).extract[EventPlayer]
  }
  case class EventPlayer(id: Option[String],
                         name: Option[String],
                         title: Option[String],
                         rating: Option[Int],
                         provisional: Option[Boolean],
                         online: Option[Boolean],
                         lag: Option[Int])

  /**
   * Translates an EventVariant from Lichess.
   * @param json - the json string representation of a variant.
   * @return an EventVariant object.
   */
  def getEventVariant(json: String): EventVariant = {
    parse(json).extract[EventVariant]
  }
  case class EventVariant(key: String, name: String, short: String)

  /**
   * Translates an EventTimeControl from Lichess.
   * @param json - the json string representation of the time control.
   * @return an EventTimeControl object.
   */
  def getEventTimeControl(json: String): EventTimeControl = {
    parse(json).extract[EventTimeControl]
  }
  case class EventTimeControl(`type`: String, limit: Int, increment: Int, show: String)

  /**
   * Translates a Perf from Lichess.
   * @param json - the json string representation of the Perf.
   * @return a Perf object.
   */
  def getPerf(json: String): Perf = {
    parse(json).extract[Perf]
  }
  case class Perf(icon: String, name: String)

  /**
   * Translates a GameEvent from Lichess.
   * @param json - the json string representation of the game event.
   * @return a GameEvent object.
   */
  def getGameEvent(json: String): GameEvent = {
    parse(json).extract[GameEvent]
  }
  case class GameEvent(id: String)


  //Game stream models
  /**
   * Translates a GameFull from Lichess.
   * @param json - the json string representation of the full game.
   * @return a GameFull object.
   */
  def getGameFull(json: String): GameFull = {
    parse(json).extract[GameFull]
  }
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

  /**
   * Translates the GameClock from Lichess.
   * @param json - the json string representation of the game clock.
   * @return a GameClock object.
   */
  def getGameClock(json: String): GameClock = {
    parse(json).extract[GameClock]
  }
  case class GameClock(initial: Int, increment: Int)

  /**
   * Translates the GamePerf from Lichess.
   * @param json - the json string representation of the game Perf.
   * @return
   */
  def getGamePerf(json: String): GamePerf = {
    parse(json).extract[GamePerf]
  }
  case class GamePerf(name: String)

  /**
   * Translates the GameEventState from Lichess.
   * @param json - the json string representation of a game state.
   * @return a GameEventState object.
   */
  def getGameEventState(json: String): GameEventState = {
    parse(json).extract[GameEventState]
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

  /**
   * Translates the ChatLine from Lichess.
   * @param json - the json string representation of a chat line.
   * @return a ChatLine object.
   */
  def getChatLine(json: String): ChatLine = {
    parse(json).extract[ChatLine]
  }
  case class ChatLine(`type`: String, username: String, text: String, room: String)
}
