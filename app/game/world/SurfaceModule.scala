package game.world

import game.EventModule
import akka.actor.ActorRef
import game.mobile.MobileModule

/**
 * A surface is an object with length, slope, and position. A surface
 * can be either a Wall or a Floor.
 *
 * A Wall always has a vertical (undefined) slope, while a Surface
 * always has a Defined slope.
 */
trait SurfaceModule {
  this: RoomModule with EventModule with MobileModule ⇒

  case class Landed( ar: ActorRef ) extends Event

  class UndefinedSlopeException extends Exception

  trait Slope {
    def dx: Int
    def dy: Int
  }

  abstract class Defined( val dx: Int, val dy: Int ) extends Slope {
    lazy val m = dy.toDouble / dx.toDouble
  }

  case class Slant( _x: Int, _y: Int ) extends Defined( _x, _y )

  case object Flat extends Defined( 1, 0 )

  case object Undefined extends Slope {
    val dx = 0
    def dy = throw new UndefinedSlopeException
  }

  /**
   * A Surface is essentially just a line with a length, position (x,y), and a slope.
   * Surfaces are owned by Room objects, and can supply Adjusts to modify certain Events.
   */
  trait Surface extends AdjustSupplier {
    val xpos: Int
    val ypos: Int
    val length: Int
    val slope: Slope
  }

  trait Floor extends Surface {
    val slope: Defined
    lazy val b = slope.dy - ( slope.m * slope.dx )

    val stopDown: Adjust = {
      case Moved( ar, p, Falling( yspeed ) ) if landing( p, yspeed ) && inBounds( p ) ⇒
        Moved( ar, p, Falling( 0 ) )
    }

    def landing( p: Position, speed: Int ) = {
      val yintersect = slope.m * p.x + b
      speed < 0 &&
        p.bottom >= yintersect &&
        ( p.bottom + speed ) < yintersect
    }

    def inBounds( p: Position ) = {
      true
    }

    adjusts = adjusts :+ stopDown
  }

  case class Wall( val xpos: Int,
                   val ypos: Int,
                   val length: Int ) extends Surface {
    val slope = Undefined
    val ytop = ypos + ( length / 2 )
    val ybottom = ypos - ( length / 2 )
    def inBounds( p: Position ) =
      ( p.top > ybottom && p.top < ytop ) || ( p.bottom < ytop && p.bottom > ybottom )

    val stopLeft: Adjust = {
      case Moved( ar, p, w: Walking ) if p.left == this.xpos && w.x < 0 && inBounds( p ) ⇒
        Moved( ar, p, Walking( 0, w.y ) )
    }

    val stopRight: Adjust = {
      case Moved( ar, p, w: Walking ) if p.right == this.xpos && w.x > 0 && inBounds( p ) ⇒
        Moved( ar, p, Walking( 0, w.y ) )
    }

    adjusts = adjusts ::: List( stopLeft, stopRight )
  }

  case class SingleSided( val xpos: Int,
                          val ypos: Int,
                          val length: Int,
                          val slope: Defined ) extends Floor

  case class DoubleSided( val xpos: Int,
                          val ypos: Int,
                          val length: Int,
                          val slope: Defined ) extends Floor

}