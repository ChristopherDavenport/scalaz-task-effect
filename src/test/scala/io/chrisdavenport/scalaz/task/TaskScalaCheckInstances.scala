package io.chrisdavenport.scalaz.task

import org.scalacheck._
import scalaz.{\/-, -\/, \/}
import scalaz.concurrent.Task
import cats._
import cats.implicits._

object TaskScalaCheckInstances extends TaskScalaCheckInstances

trait TaskScalaCheckInstances{

  implicit def cogenTask[A]: Cogen[Task[A]] =
      Cogen[Unit].contramap(_ => ())

  implicit def taskEq[A: Eq](implicit E: Eq[Throwable]): Eq[Task[A]] = 
    new Eq[Task[A]]{
      def eqv(x: Task[A], y: Task[A]): Boolean = {
        val xatt: Throwable \/ A = x.attempt.unsafePerformSync
        val yatt: Throwable \/ A = y.attempt.unsafePerformSync

        (xatt, yatt) match {
          case (\/-(x), \/-(y)) => x === y
          case (-\/(xf), -\/(yf)) => xf === yf
          case _ => false
        } 
      }
    }

}