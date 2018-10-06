package io.chrisdavenport.scalaz.task

import cats.effect.laws.discipline.arbitrary._
import cats.effect.laws.discipline.{EffectTests, Parameters}
import cats.effect.laws.util.{TestContext, TestInstances}
import cats.implicits._
import io.chrisdavenport.scalaz.task.TaskArbitrary._
import io.chrisdavenport.scalaz.task.TaskScalaCheckInstances._
import org.scalatest.prop.Checkers
import org.scalatest.{FunSuite, Matchers}
import org.typelevel.discipline.Laws
import org.typelevel.discipline.scalatest.Discipline
import scalaz.concurrent.Task

import java.io.{ByteArrayOutputStream, PrintStream}

import scala.util.control.NonFatal

class TaskLaws extends FunSuite with Matchers with Checkers with Discipline with TestInstances {

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

  implicit val params: Parameters = Parameters.default.copy(allowNonTerminationLaws = false)
  checkAllAsync("Effect[Task]", implicit e => EffectTests[Task].effect[Int, Int, Int])

}
