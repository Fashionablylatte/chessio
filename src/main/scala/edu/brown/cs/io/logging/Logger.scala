package edu.brown.cs.io.logging

import edu.brown.cs.config.LoggerConfig

/**
 * Logging utility with 7 possible levels of information. 0 (None) for no information printouts, and 6 (trace) for
 * extremely verbose printouts.
 */
abstract class Logger { // TODO maybe replace with actual enumerations? would need to create custom implicit ordering
  val NONE = 0
  val FATAL = 1
  val ERROR = 2
  val WARN = 3
  val INFO = 4
  val DEBUG = 5
  val TRACE = 6
  val name = "LOGGER"

  val level = LoggerConfig.getLoggingLevel("config/config.xml")

  /**
   * Logs a message for a fatal event. Intended only for errors that should cause the program to exit.
   * @param msg - a message String about the event.
   */
  def fatal(msg: String): Unit ={
    if(level >= FATAL) {
      Console.println(s"$name [${Console.RED}${Console.YELLOW_B}FATAL${Console.RESET}]: ${msg}")
    }
  }

  /**
   * Logs a message for an error event. Intend for errors that interrupt normal functionality.
   * @param msg - a message String about the event.
   */
  def error(msg: String): Unit ={
    if(level >= ERROR) {
      Console.println(s"$name [${Console.RED}ERROR${Console.RESET}]: ${msg}")
    }
  }

  /**
   * Logs a warning for an anomalous event. Intended for events that may cause unexpected behavior.
   * @param msg - a message String about the event.
   */
  def warn(msg: String): Unit ={
    if(level >= WARN){
      Console.println(s"$name [${Console.YELLOW}WARN${Console.RESET}]: ${msg}")
    }
  }

  /**
   * Logs generic information about the program.
   * @param msg - a message String about the event.
   */
  def info(msg: String): Unit ={
    if(level >= INFO) {
      Console.println(s"$name [${Console.GREEN}INFO${Console.RESET}]: ${msg}")
    }
  }

  /**
   * Logs information useful for general debugging. More verbose than necessary for normal operation.
   * @param msg - a message String about the event.
   */
  def debug(msg: String): Unit ={
    if(level >= DEBUG) {
      Console.println(s"$name [${Console.CYAN}DEBUG${Console.RESET}]: ${msg}")
    }
  }

  /**
   * Logs information for fine-grained debugging. Very verbose, to the point of impacting performance significantly.
   * @param msg - a message String about the event.
   */
  def trace(msg: String): Unit ={
    if(level >= TRACE) {
      Console.println(s"$name [${Console.BLUE}TRACE${Console.RESET}]: ${msg}")
    }
  }
}
