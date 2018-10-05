package io.chrisdavenport.scalaz.task.instances

import cats.effect.{Effect, ExitCase, IO, SyncIO}
import scalaz.concurrent.Task
import scalaz.{-\/, \/, \/-}

import java.util.concurrent.atomic.AtomicBoolean

object TaskInstances extends TaskInstances

trait TaskInstances {
  implicit val taskEffect: Effect[Task] = new Effect[Task] {

    // Members declared in cats.Applicative
    def pure[A](x: A): Task[A] = Task.now(x)
    override def map[A, B](fa: Task[A])(f: A => B): Task[B] = fa.map(f)

    // Members declared in cats.FlatMap
    def flatMap[A, B](fa: Task[A])(f: A => Task[B]): Task[B] = fa.flatMap(f)
    def tailRecM[A, B](a: A)(f: A => Task[Either[A, B]]): Task[B] = f(a).flatMap {
      case Left(a) => tailRecM(a)(f)
      case Right(b) => Task.now(b)
    }

    // Members declared in cats.ApplicativeError
    def raiseError[A](e: Throwable): Task[A] = Task.fail(e)
    def handleErrorWith[A](fa: Task[A])(f: Throwable => Task[A]): Task[A] =
      fa.handleWith({ case e => f(e) })

    // Members declared in cats.effect.Sync
    def suspend[A](thunk: => Task[A]): Task[A] = Task.suspend(thunk)

    // Members declared in cats.effect.Async

    // In order to comply with `repeatedCallbackIgnored` law
    // on async, a custom AtomicBoolean is required to ignore
    // second callbacks.
    def async[A](k: (Either[Throwable, A] => Unit) => Unit): Task[A] =
      Task.async(registerCallback(k))

    def asyncF[A](k: (Either[Throwable, A] => Unit) => Task[Unit]): Task[A] =
      Task.async(registerCallback(k)(_).unsafePerformAsync(_ => ()))

    // Members declared in cats.effect.Effect

    /** runAsync takes the final callback to something that
     * summarizes the effects in an IO[Unit] as such this
     * takes the Task and executes the internal IO callback
     * into the task asynchronous execution all delayed
     * within the outer IO, discarding any error that might
     * occur
      **/
    def runAsync[A](fa: Task[A])(cb: Either[Throwable, A] => IO[Unit]): SyncIO[Unit] =
      SyncIO(fa.unsafePerformAsync(d => cb(d.toEither).unsafeRunAsyncAndForget()))

    override def toIO[A](fa: Task[A]): IO[A] =
      IO.async(k => fa.unsafePerformAsync(k.compose(_.toEither)))

    // Members declared in cats.effect.Bracket

    def bracketCase[A, B](acquire: Task[A])(use: A => Task[B])(
        release: (A, ExitCase[Throwable]) => Task[Unit]
    ): Task[B] = acquire.flatMap { a =>
      use(a).attempt.flatMap {
        case -\/(e) => release(a, ExitCase.error(e)).flatMap(_ => Task.fail(e))
        case \/-(b) => release(a, ExitCase.complete).map(_ => b)
      }
    }

    private def registerCallback[A, B](k: (Either[Throwable, A] => Unit) => B)(
        registered: Throwable \/ A => Unit
    ): B = {
      val fence = new AtomicBoolean(true)
      k(e => if (fence.getAndSet(false)) { registered(\/.fromEither(e)) })
    }
  }
}
