package io.chrisdavenport.scalaz.task

import cats._
import cats.effect.implicits._
import cats.effect.laws.util.{TestContext, TestInstances}
import cats.implicits._
import scalaz.Tag
import scalaz.concurrent.Task
import scalaz.concurrent.Task.ParallelTask

object TaskTestInstances extends TaskTestInstances

trait TaskTestInstances extends TestInstances {

  implicit def taskEq[A: Eq](implicit ec: TestContext): Eq[Task[A]] =
    Eq.instance((x, y) => x.toIO eqv y.toIO)

  implicit def parallelTaskEq[A: Eq](implicit ec: TestContext): Eq[ParallelTask[A]] =
    Tag.subst(taskEq[A])
}
