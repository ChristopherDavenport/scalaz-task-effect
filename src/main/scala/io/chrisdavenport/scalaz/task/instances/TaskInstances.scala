package io.chrisdavenport.scalaz.task.instances

import cats.effect.{Effect, IO}
import scalaz.\/
import scalaz.concurrent.Task
import java.util.concurrent.atomic.AtomicBoolean

object TaskInstances extends TaskInstances

trait TaskInstances {
  implicit val taskEffect : Effect[Task] = new Effect[Task]{

    // Members declared in cats.Applicative
    def pure[A](x: A): Task[A] = Task.now(x)
  
    // Members declared in cats.ApplicativeError
    def handleErrorWith[A](fa: Task[A])(f: Throwable => Task[A]): Task[A] = fa.handleWith(functionToPartial(f))
    def raiseError[A](e: Throwable): Task[A] = Task.fail(e)
  
    // Members declared in cats.effect.Async
    def async[A](k: (Either[Throwable,A] => Unit) => Unit): Task[A] = 
      Task.async{ registered =>
        val a = new AtomicBoolean(true)
        k(e => if (a.get) { a.set(false); registered(\/.fromEither(e))} else ())
      }
  
    // Members declared in cats.effect.Effect
    def runAsync[A](fa: Task[A])(cb: Either[Throwable,A] => IO[Unit]): IO[Unit] = 
      IO(fa.unsafePerformAsync{disjunction =>  cb(disjunction.toEither).unsafeRunSync})
  
    // Members declared in cats.FlatMap
    def flatMap[A, B](fa: Task[A])(f: A => Task[B]): Task[B] = fa.flatMap(f)
    // Simplest Implementation I could think of
    def tailRecM[A, B](a: A)(f: A => Task[Either[A,B]]): Task[B] = f(a).flatMap{
      case Left(a) => tailRecM(a)(f)
      case Right(b) => Task.now(b)
    }
  
    // Members declared in cats.effect.Sync
    def suspend[A](thunk: => Task[A]): Task[A] = Task.suspend(thunk)
  }

  private def functionToPartial[A, B](f: Function1[A, B]): PartialFunction[A, B] = _ match {
    case a => f(a)
  }
}