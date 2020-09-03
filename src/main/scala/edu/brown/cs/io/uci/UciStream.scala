package edu.brown.cs.io.uci

import java.io.InputStream
import java.util.Objects

import edu.brown.cs.io.logging.ChessLogger

import scala.collection.mutable

/**
 * An InputStream for handling feeding UCI requests to an external engine. Allows for a
 * user REPL to occupy System.in without impacting the engine.
 */
class UciStream extends InputStream {
  val contents: mutable.Queue[Byte] = mutable.Queue[Byte]()
  var terminate = false

  override def read(): Int = {
    while(contents.isEmpty) {
      Thread.sleep(1000)
    }
    contents.dequeue().toInt
  }

  override def read(b: Array[Byte], off: Int, len: Int): Int = {
    Objects.checkFromIndexSize(off, len, b.length)
    if (len == 0){
      0
    } else {
      var c = read()
      if (c == -1) {
        -1
      } else {
        b(off) = c.toByte
        var i = 1
        try {
          while (i < len && c != -1 && c != 10) {
            c = read()
            if (c != -1) {
              b(off + i) = c.toByte
              i += 1
            }
          }
        } catch {
          case _ =>
        }
        i
      }
    }
  }

  /**
   * Adds a String command to the UCI stream for the engine to process.
   * @param str - a UCI command in String form.
   */
  def append(str: String): Unit ={
    synchronized {
      val bytes = (str + "\n").getBytes("UTF-8")
      bytes.foreach(b => contents.enqueue(b))
      ChessLogger.trace(s"element '$str' appended to uci stream")
    }
  }
}
