package edu.brown.cs.main

object GeneralCommands {
  def hello(arg: Vector[String]): Unit ={
    println(s"Hello, ${arg(0)}!")
  }
}
