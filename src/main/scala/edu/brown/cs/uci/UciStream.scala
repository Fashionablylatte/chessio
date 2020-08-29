package edu.brown.cs.uci

import java.io.InputStream
import java.util.Objects

import edu.brown.cs.io.ChessLogger

import scala.collection.mutable

class UciStream extends InputStream {
  val contents: mutable.Queue[Byte] = mutable.Queue[Byte]()
  var terminate = false

  override def read(): Int = {
    while(contents.isEmpty) {
      Thread.sleep(1000)
    }
    ChessLogger.trace("found contents")
    val ret = contents.dequeue().toInt
    if (ret != 0) {
      ChessLogger.trace(ret.toChar.toString())
      ChessLogger.trace(ret.toString)
    }
    ret
  }

  override def read(b: Array[Byte], off: Int, len: Int): Int = { //TODO worry about unexpected behavior here?
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

  def append(str: String): Unit ={
    synchronized {
      val bytes = (str + "\n").getBytes("UTF-8")
      bytes.foreach(b => contents.enqueue(b))
      ChessLogger.trace(s"element '$str' appended to uci stream")
    }
  }

  def endStream(): Unit = {
    terminate = true
  }
}
