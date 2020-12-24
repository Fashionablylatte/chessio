package edu.brown.cs.config

import scala.collection.mutable.ArrayBuffer

object UCIEngineConfig {
  def getEngineSettings(filePath: String): (String, ArrayBuffer[(String, String)]) ={

    val configs = scala.xml.XML.loadFile(filePath)
    val engine = (configs \ "location").map(token => token.text)(0)
    val options: ArrayBuffer[(String, String)] = ArrayBuffer[(String, String)]()
    for {
      option <- configs \\ "option"
      name <- (option \ "name").map(token => token.text)
      value <- (option \ "value").map(token => token.text)
    } yield {
      options.append((name, value))
    }
    (engine, options)
  }
}
