package edu.brown.cs.chessgame

import java.util.Arrays

import chess.format.Forsyth
import chess.{Game, Hash, PositionHash, variant}

object GameHash {

  def hashGame(game: Game): PosHash ={
    PosHash(Hash(game.situation))
  }

  def hashFen(fen: String): Option[PosHash] ={
    val sit = Forsyth <<@(variant.Standard, fen)
    sit match {
      case Some(s) => Some(PosHash(Hash(s)))
      case None => None
    }
  }
}

case class PosHash(ph: PositionHash){
  override def hashCode(): Int ={
    Arrays.hashCode(ph)
  }

  override def equals(that: Any): Boolean ={
    that match {
      case p: PosHash => Arrays.equals(p.ph, ph)
      case _ => false
    }
  }
}