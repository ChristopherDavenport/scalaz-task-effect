package io.chrisdavenport.scalaz.task

import cats.effect.Async
import cats.effect.laws.discipline.EffectTests
import cats.effect.laws.discipline.arbitrary._
import cats.effect.laws.util.TestContext
import cats.implicits._
import cats.laws.discipline.{ApplicativeTests, ParallelTests}
import cats.{Applicative, Eq}
import io.chrisdavenport.scalaz.task.TaskArbitrary._
import io.chrisdavenport.scalaz.task.instances.TaskInstances
import org.scalatest.prop.Checkers
import org.scalatest.{FunSuite, Matchers}
import org.typelevel.discipline.Laws
import org.typelevel.discipline.scalatest.Discipline
import scalaz.concurrent.Task
import scalaz.concurrent.Task.ParallelTask

import java.io.{ByteArrayOutputStream, PrintStream}
import java.util.concurrent.RejectedExecutionException

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class TaskLaws
    extends FunSuite
    with Matchers
    with Checkers
    with Discipline
    with TaskInstances
    with TaskTestInstances {

  implicit val parallelTaskAp: Applicative[ParallelTask] = parallelTaskApplicative

  /**
   * Silences `System.err`, only printing the output in case exceptions are
   * thrown by the executed `thunk`.
   */
  def silenceSystemErr[A](thunk: => A): A = synchronized {
    // Silencing System.err
    val oldErr = System.err
    val outStream = new ByteArrayOutputStream()
    val fakeErr = new PrintStream(outStream)
    System.setErr(fakeErr)
    try {
      val result = thunk
      System.setErr(oldErr)
      result
    } catch {
      case NonFatal(e) =>
        System.setErr(oldErr)
        // In case of errors, print whatever was caught
        fakeErr.close()
        val out = outStream.toString("utf-8")
        if (out.nonEmpty) oldErr.println(out)
        throw e
    }
  }

  def checkAllAsync(name: String, f: TestContext => Laws#RuleSet): Unit = {
    val context = TestContext()
    val ruleSet = f(context)

    for ((id, prop) ← ruleSet.all.properties)
      test(name + "." + id) {
        silenceSystemErr(check(prop))
      }
  }

  checkAllAsync(
    "Effect[Task]",
    implicit ec => EffectTests[Task].effect[Int, Int, Int]
  )

  checkAllAsync(
    "Applicative[ParallelTask]",
    implicit ec => {
      val tests = ApplicativeTests[ParallelTask]
      tests.applicative[Int, Int, Int]
    }
  )

  checkAllAsync(
    "Parallel[Task, ParallelTask]",
    implicit ec => ParallelTests[Task, ParallelTask].parallel[Int, Int]
  )

  test("Async.shift[Task] on rejecting execution context") {
    implicit val context: TestContext = TestContext()
    val boom = new RejectedExecutionException("Boom")
    val rejectingEc = new ExecutionContext {
      def execute(runnable: Runnable): Unit = throw boom
      def reportFailure(cause: Throwable): Unit = ()
    }

    assert(Eq[Task[Unit]].eqv(Async.shift[Task](rejectingEc), Task.fail(boom)))
  }
}
