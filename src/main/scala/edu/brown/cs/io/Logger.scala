package edu.brown.cs.io

/**
 * Logging utility with 7 possible levels of information. 0 (None) for no information printouts, and 6 (trace) for
 * extremely verbose printouts.
 */
abstract class Logger {
  val NONE = 0
  val FATAL = 1
  val ERROR = 2
  val WARN = 3
  val INFO = 4
  val DEBUG = 5
  val TRACE = 6

  val conf = scala.xml.XML.loadFile("config/config.xml")
  val levelName = try {
    (conf \ "logging").map(ln => ln.text)(0)
  } catch {
    case e: IndexOutOfBoundsException =>
      Console.err.println("No logger setting found! Defaulting to 'info' (moderately verbose).")
      "info"
  }
  val level = levelName.toUpperCase() match {
    case "NONE" => 0
    case "FATAL" => 1
    case "ERROR" => 2
    case "WARN" => 3
    case "INFO" => 4
    case "DEBUG" => 5
    case "TRACE" => 6
  }

  def fatal(msg: String): Unit ={
    if(level >= FATAL) {
      Console.println(s"LOGGER [${Console.RED}${Console.YELLOW_B}FATAL${Console.RESET}]: ${msg}")
    }
  }

  def error(msg: String): Unit ={
    if(level >= ERROR) {
      Console.println(s"LOGGER [${Console.RED}ERROR${Console.RESET}]: ${msg}")
    }
  }

  def warn(msg: String): Unit ={
    if(level >= WARN){
      Console.println(s"LOGGER [${Console.YELLOW}WARN${Console.RESET}]: ${msg}")
    }
  }

  def info(msg: String): Unit ={
    if(level >= INFO) {
      Console.println(s"LOGGER [${Console.GREEN}INFO${Console.RESET}]: ${msg}")
    }
  }

  def debug(msg: String): Unit ={
    if(level >= DEBUG) {
      Console.println(s"LOGGER [${Console.CYAN}DEBUG${Console.RESET}]: ${msg}")
    }
  }

  def trace(msg: String): Unit ={
    if(level >= TRACE) {
      Console.println(s"LOGGER [${Console.BLUE}TRACE${Console.RESET}]: ${msg}")
    }
  }
}
