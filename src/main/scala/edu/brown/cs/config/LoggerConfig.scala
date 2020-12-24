package edu.brown.cs.config

/**
 * Utility for loading in configuration settings for the Logger utility.
 */
object LoggerConfig {

  /**
   * A method that gets the logging level from the configuration files.
   * @param filePath - a String representing the path to the XML configuration file.
   * @return an Int representing the configuration level.
   */
  def getLoggingLevel(filePath: String): Int = {
    val conf = scala.xml.XML.loadFile(filePath)
    val levelName = try {
      (conf \ "logging").map(ln => ln.text)(0)
    } catch {
      case e: IndexOutOfBoundsException =>
        Console.err.println("No logger setting found! Defaulting to 'info' (moderately verbose).")
        "info"
    }
    levelName.toUpperCase() match { // TODO replace with actual enumerations
      case "NONE" => 0
      case "FATAL" => 1
      case "ERROR" => 2
      case "WARN" => 3
      case "INFO" => 4
      case "DEBUG" => 5
      case "TRACE" => 6
    }
  }
}
