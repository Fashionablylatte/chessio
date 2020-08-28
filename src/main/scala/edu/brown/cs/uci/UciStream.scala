package edu.brown.cs.uci

import java.io.InputStream
import java.util.concurrent.atomic.AtomicReferenceArray

class UciStream extends InputStream {
  val contents: AtomicReferenceArray[Byte] = new AtomicReferenceArray[Byte](1024)
  var pointer = 0

  override def read(): Int = {
    if(pointer >= contents.length){
      -1
    } else {
      pointer += 1
      contents.get(pointer)
    }
  }

  def append(str: String): Unit ={
    val bytes = str.getBytes("UTF-8")
    bytes.foreach(b => contents)
  }
}
