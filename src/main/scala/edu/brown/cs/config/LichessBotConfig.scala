package edu.brown.cs.config

import edu.brown.cs.io.logging.ChessLogger

import scala.collection.mutable.ArrayBuffer

/**
 * Utility for loading the configuration for connecting to the Lichess bot API.
 */
object LichessBotConfig {

  /**
   * A method that loads in the parameters from the XML configuration file.
   * @param filePath a string representing the filepath of the XML configuration file for the Lichess API.
   * @return a Tuple4 of Strings representing the Lichess server, botId, bot token, and engine location.
   */
  def loadGeneralConfig(filePath: String): (String, String, String, String) = {
    val configs = scala.xml.XML.loadFile(filePath)
    val server = (configs \ "environment").map(token => token.text)(0)

    val botId =  (configs \ "bot" \ "id").map(token => token.text)(0).toLowerCase()
    if(botId.isEmpty) throw new InstantiationException("Missing bot ID")

    val token = (configs \ "bot" \ "token").map(token => token.text)(0)
    if(botId.isEmpty) throw new InstantiationException("Missing bot token")

    val engine = (configs \ "engine").map(token => token.text)(0)

    (server, botId, token, engine)
  }

  //TODO this is ugly. Partion into different methods?
  def loadEndpointConfig(filepath: String): (Int, Int, ArrayBuffer[String], Boolean, Boolean, Int, Int, Int, Int,
    Boolean, Boolean) = {
    val configs = scala.xml.XML.loadFile("config/config.xml")
    val DEFAULT_READ_TIMEOUT = try {
      (configs \ "read-timeout").map(ms => ms.text)(0).toInt
    } catch {
      case iobe: IndexOutOfBoundsException =>
        ChessLogger.warn("No value found, defaulting to 1800000ms read timeout.")
        1800000
      case nfe: NumberFormatException =>
        ChessLogger.warn("Unparseable value, defaulting to 1800000ms read timeout.")
        1800000
    }
    val DEFAULT_CONNECT_TIMEOUT = try {
      (configs \ "conn-timeout").map(ms => ms.text)(0).toInt
    } catch {
      case iobe: IndexOutOfBoundsException =>
        ChessLogger.warn("No value found, defaulting to 30000ms connect timeout.")
        30000
      case nfe: NumberFormatException =>
        ChessLogger.warn("Unparseable value, defaulting to 30000ms connect timeout.")
        30000
    }

    val variants: ArrayBuffer[String] = ArrayBuffer[String]()
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

    val untimed = try {
      (configs \\ "untimed").map(ut => ut.text)(0).toBoolean
    } catch {
      case iobe: IndexOutOfBoundsException =>
        ChessLogger.warn("No value found, defaulting to untimed games blocked.")
        false
      case nfe: IllegalArgumentException =>
        ChessLogger.warn("Unparseable value, defaulting to untimed games blocked.")
        false
    }

    val timed = try {
      (configs \\ "timed").map(ut => ut.text)(0).toBoolean
    } catch {
      case iobe: IndexOutOfBoundsException =>
        ChessLogger.warn("No value found, defaulting to timed games allowed.")
        true
      case nfe: IllegalArgumentException =>
        ChessLogger.warn("Unparseable value, defaulting to timed games allowed.")
        true
    }

    val timeMinimum = try {
      (configs \\ "min").map(min => min.text)(0).toInt * 60
    } catch {
      case iobe: IndexOutOfBoundsException =>
        ChessLogger.warn("No value found, defaulting to 30s minimum timecontrol.")
        30
      case nfe: NumberFormatException =>
        ChessLogger.warn("Unparseable value, defaulting to 30s minimum timecontrol.")
        30
    }

    val timeMaximum = try {
      (configs \\ "max").map(max => max.text)(0).toInt * 60
    } catch {
      case iobe: IndexOutOfBoundsException =>
        ChessLogger.warn("No value found, defaulting to 18000mins maximum timecontrol.")
        18000
      case nfe: NumberFormatException =>
        ChessLogger.warn("Unparseable value, defaulting to 18000mins maximum timecontrol.")
        18000
    }

    val incMinimum = try {
      (configs \\ "inc-min").map(incMin => incMin.text)(0).toInt
    } catch {
      case iobe: IndexOutOfBoundsException =>
        ChessLogger.warn("No value found, defaulting to 0s minimum increment.")
        0
      case nfe: NumberFormatException =>
        ChessLogger.warn("Unparseable value, defaulting to 0s minimum increment.")
        0
    }

    val incMaximum = try {
      (configs \\ "inc-max").map(incMax => incMax.text)(0).toInt
    } catch {
      case iobe: IndexOutOfBoundsException =>
        ChessLogger.warn("No value found, defaulting to 120s minimum increment.")
        120
      case nfe: NumberFormatException =>
        ChessLogger.warn("Unparseable value, defaulting to 120s minimum increment.")
        120
    }

    val rated = try {
      (configs \ "rated").map(rt => rt.text)(0).toBoolean
    } catch {
      case iobe: IndexOutOfBoundsException =>
        ChessLogger.warn("No value found, defaulting to rated games allowed.")
        true
      case nfe: IllegalArgumentException =>
        ChessLogger.warn("Unparseable value, defaulting to rated games allowed.")
        true
    }

    val casual = try {
      (configs \ "casual").map(rt => rt.text)(0).toBoolean
    } catch {
      case iobe: IndexOutOfBoundsException =>
        ChessLogger.warn("No value found, defaulting to unrated games allowed.")
        true
      case nfe: IllegalArgumentException =>
        ChessLogger.warn("Unparseable value, defaulting to unrated games allowed.")
        true
    }

    (DEFAULT_READ_TIMEOUT, DEFAULT_CONNECT_TIMEOUT, variants, untimed, timed, timeMinimum,
      timeMaximum, incMinimum, incMaximum, rated, casual)
  }
}
