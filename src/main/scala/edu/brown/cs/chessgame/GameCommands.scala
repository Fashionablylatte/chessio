package edu.brown.cs.chessgame

import edu.brown.cs.engine.{AlphaBetaEngine, Evaluation}

object GameCommands {
  private var game : Option[GameState] = None
  private var side : Int = 0
  private var eng : Option[AlphaBetaEngine] = None
  private val ENG_AS_WHITE = true
  private val ENG_AS_BLACK = false

  def startGame(args: Vector[String]): Unit ={ //TODO help instructions
    if(args.length == 2){
      val color = args(1).split(" ")(1)
      if(args(0).equals("black")){
        Console.println(s"Starting from fen ${args(1)} as black.")
        gameInit(1, ENG_AS_WHITE, args(1))
        if(color.equals("w")) eng.get.makeMove()
      } else {
        Console.println(s"Starting from fen ${args(1)} as white.")
        gameInit(0, ENG_AS_BLACK, args(1))
        if(color.equals("b")) eng.get.makeMove()
      }
    } else if(args.length != 1 || args(0).equals("white")){
      Console.println("Starting Standard game as white.")
      gameInit(0, ENG_AS_BLACK)
    } else {
      Console.println("Starting Standard game as black.")
      gameInit(1, ENG_AS_WHITE)
      eng.get.makeMove()
    }
  }

  private def gameInit(playerSide: Int, engSide: Boolean, fen: String = "None"): Unit ={
    game = Some(new GameState(fen))
    eng = Some(new AlphaBetaEngine(game.get, engSide))
    side = playerSide
  }

  def makeMove(args: Vector[String]): Unit ={
    game match {
      case None => Console.println("No existing game. Type 'startgame' to start one.")
      case Some(g) =>
        if(g.getTurn() != side){
          Console.println(s"It is not your turn! ${g.getTurn()}")
        } else if(args.size != 2){
          Console.println("Please format your move as START_SQUARE END_SQUARE, e.g. e2 e4")
        } else {
          g.makeMove(args(0), args(1))
          eng.get.makeMove()
        }
    }
  }

  def evaluate(args: Vector[String]): Unit ={
    game match {
      case None => Console.println("No existing game. Type 'startgame' to start one.")
      case Some(g) =>
        Console.println(s"Current eval: ${Evaluation.evalPos(g.getGame())}")
    }
  }

  def depth(args: Vector[String]): Unit ={
    game match {
      case None => Console.println("No existing game. Type 'startgame' to start one.")
      case Some(g) =>
        try{
          eng.get.setDepth(args(0).toInt)
        } catch {
          case e => println("Failed to set depth")
        }
    }
  }
}
