package edu.brown.cs.io

import java.util.regex.Pattern

import scala.Console.RED
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * A basic REPL for Scala programs.
 * @param functionMap - A mapping of String command names to functions of the signature Vector[String] => Any.
 */
class REPL(functionMap: mutable.HashMap[String, (String, Vector[String] => Any)]) {
  addCommand("help", ("Get a list of commands and what they do.", helpCommand))

  /**
   * Adds a supported command to the REPL.
   * @param commandName - the String name of the command.
   * @param commandFunction
   */
  def addCommand(commandName: String, commandFunction: (String, Vector[String] => Any)): Unit ={
    functionMap += (commandName -> commandFunction)
  }

  /**
   * Retrieves a list of all available commands.
   * @param args - a Vector of String arguments to provide to this command.
   */
  def helpCommand(args: Vector[String]): Unit ={
    Console.println("[REPL]: Available commands:")
    functionMap.foreach(c => Console.println(s"${c._1}: ${c._2._1}"))
  }

  // runs a command.
  private def processCommand(commandName: String, args: Vector[String]): Unit ={
    functionMap.get(commandName) match {
      case Some(func) => func._2(args)
      case None => Console.err.println(s"${RED}Command not found!")
    }
  }

  /**
   * Runs the REPL. This REPL uses io.StdIn, so make sure no other processes also occupy System.in.
   */
  def run(): Unit ={
    var input = io.StdIn.readLine()
    while(input != null){
      if(!input.equals("")){
        val pattern = Pattern.compile("\"(.*?)\"|([^\\s]+)")
        val matcher = pattern.matcher(input)
        val buf: ArrayBuffer[String] = ArrayBuffer()
        while(matcher.find){
          buf.append(matcher.group().replaceAll("\"", ""))
        }
        val command = buf(0)
        val args = buf.takeRight(buf.length - 1).toVector
        processCommand(command, args)
      }
      input = io.StdIn.readLine()
    }
    Console.println("[REPL]: User input Ctrl-D exit")
  }
}

