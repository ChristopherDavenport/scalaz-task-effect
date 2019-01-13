package io.chrisdavenport.scalaz.task

import org.scalacheck._
import scalaz.concurrent.Task
import scalaz.concurrent.Task.ParallelTask
import scalaz.{\/, Tag}

object TaskArbitrary {
  implicit def catsEffectLawsArbitraryForTask[A: Arbitrary: Cogen]: Arbitrary[Task[A]] =
    Arbitrary(Gen.delay(genTask[A]))

  implicit def catsEffectLawsArbitraryForParallelTask[
      A: Arbitrary: Cogen
  ]: Arbitrary[ParallelTask[A]] = Tag.subst(catsEffectLawsArbitraryForTask[A])

  def genTask[A: Arbitrary: Cogen]: Gen[Task[A]] = {
    Gen.frequency(
      5 -> genPure[A],
      1 -> genFail[A],
      5 -> genAsync[A],
      5 -> genNestedAsync[A],
      5 -> genSuspend[A],
      5 -> genMapOne[A],
      5 -> genMapTwo[A],
      10 -> genFlatMap[A]
    )
  }

  def genPure[A: Arbitrary]: Gen[Task[A]] =
    Arbitrary.arbitrary[A].map(Task.now)

  def genFail[A]: Gen[Task[A]] =
    Arbitrary.arbitrary[Throwable].map(Task.fail)

  def genAsync[A: Arbitrary]: Gen[Task[A]] =
    Arbitrary
      .arbitrary[(Either[Throwable, A] => Unit) => Unit]
      .map(f =>
        Task.async { registered =>
          f(e => registered(\/.fromEither(e)))
      })

  def genNestedAsync[A: Arbitrary: Cogen]: Gen[Task[A]] =
    Arbitrary
      .arbitrary[(Either[Throwable, Task[A]] => Unit) => Unit]
      .map(f =>
        Task
          .async { registered: ((Throwable \/ Task[A]) => Unit) =>
            f(e => registered(\/.fromEither(e)))
          }
          .flatMap(x => x))

  def genSuspend[A: Arbitrary: Cogen]: Gen[Task[A]] =
    Arbitrary.arbitrary[Task[A]].map(Task.suspend(_))

  def genFlatMap[A: Arbitrary: Cogen]: Gen[Task[A]] =
    for {
      ioa <- Arbitrary.arbitrary[Task[A]]
      f <- Arbitrary.arbitrary[A => Task[A]]
    } yield ioa.flatMap(f)

  def genMapOne[A: Arbitrary: Cogen]: Gen[Task[A]] =
    for {
      ioa <- Arbitrary.arbitrary[Task[A]]
      f <- Arbitrary.arbitrary[A => A]
    } yield ioa.map(f)

  def genMapTwo[A: Arbitrary: Cogen]: Gen[Task[A]] =
    for {
      ioa <- Arbitrary.arbitrary[Task[A]]
      f1 <- Arbitrary.arbitrary[A => A]
      f2 <- Arbitrary.arbitrary[A => A]
    } yield ioa.map(f1).map(f2)

}
