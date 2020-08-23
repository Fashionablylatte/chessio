package edu.brown.cs.io

import chess.{Game, Pos}

object ChessLogger extends Logger {
  def prettypos(game: Game): Unit ={
    if(level >= TRACE) {
      val pieces = game.situation.board.pieces
      val sb = new StringBuilder
      for(y <- 8 to 1 by -1){
        sb.append(y)
        sb.append(" ")
        for(x <- 1 to 8){
          val piece = pieces.get(Pos.posAt(x, y).get)
          val highlight = if((x + y) % 2 == 1) s"${Console.WHITE_B}" else ""
          val pieceStr =
            if(piece.nonEmpty) {
              if(piece.get.color.white) {
                s"${Console.YELLOW}${Console.BOLD}${piece.get.role.forsythUpper}"
              } else {
                s"${Console.BLUE}${Console.BOLD}${piece.get.role.forsyth}"
              }
            } else {
              s" "
            }
          sb ++= s"${highlight} ${pieceStr} ${Console.RESET}"
        }
        sb ++= "\n"
      }
      sb ++= "   a  b  c  d  e  f  g  h"
      println(sb.toString())
    }
  }

  def pos(info: String, game: Game): Unit ={
    if(level >= TRACE) {
      val pieces = game.situation.board.pieces
      val sb = new StringBuilder
      sb.append(s"LOGGER [${Console.BLUE}TRACE${Console.RESET}]: $info \n")
      for(y <- 8 to 1 by -1){
        sb.append(y)
        sb.append(" ")
        for(x <- 1 to 8){
          val piece = pieces.get(Pos.posAt(x, y).get)
          val pieceStr =
            if(piece.nonEmpty) {
              if(piece.get.color.white) {
                piece.get.role.forsythUpper
              } else {
                piece.get.role.forsyth
              }
            } else {
              " "
            }
          sb ++= s"[$pieceStr]"
        }
        sb ++= "\n"
      }
      sb ++= "   a  b  c  d  e  f  g  h\n "
      println(sb.toString())
    }
  }
}
