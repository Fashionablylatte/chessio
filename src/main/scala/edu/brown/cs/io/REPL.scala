package edu.brown.cs.io

import java.util.regex.Pattern

import scala.Console.RED
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class REPL(functionMap: mutable.HashMap[String, Vector[String] => Any]) {

  def addCommand(commandName: String, commandFunction: Vector[String] => Any): Unit ={
    functionMap += (commandName -> commandFunction)
  }

  addCommand("help", helpCommand)

  private def helpCommand(args: Vector[String]): Unit ={
    Console.println(functionMap.keySet.mkString("These commands are available: ", ", ", ""))
  }

  private def processCommand(commandName: String, args: Vector[String]): Unit ={
    functionMap.get(commandName) match {
      case Some(func) => func(args)
      case None => Console.err.println(s"${RED}Command not found!")
    }
  }

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
    Console.println("User input Ctrl-D exit")
  }
}

