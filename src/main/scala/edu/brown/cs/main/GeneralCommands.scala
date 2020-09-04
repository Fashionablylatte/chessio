package edu.brown.cs.main

object GeneralCommands {
  def hello(arg: Vector[String]): Unit ={
    if(arg.isEmpty){
      println("Hello there!")
    } else {
      println(s"Hello, ${arg(0)}!")
    }
  }
}
