package io.chrisdavenport.scalaz.task

import org.scalacheck._
import scalaz.\/
import scalaz.concurrent.Task

object TaskArbitrary {
  implicit def catsEffectLawsArbitraryForTask[A: Arbitrary: Cogen]: Arbitrary[Task[A]] =
    Arbitrary(Gen.delay(genTask[A]))
    
  def genTask[A: Arbitrary: Cogen]: Gen[Task[A]] = {
    Gen.frequency(
      5 -> genPure[A],
      5 -> genApply[A],
      1 -> genFail[A],
      5 -> genAsync[A],
      5 -> genNestedAsync[A],
      5 -> getMapOne[A],
      5 -> getMapTwo[A],
      10 -> genFlatMap[A]
    )
  }

  def genPure[A: Arbitrary]: Gen[Task[A]] =
    Arbitrary.arbitrary[A].map(Task.now(_))

  def genApply[A: Arbitrary]: Gen[Task[A]] =
    Arbitrary.arbitrary[A].map(Task.apply(_))
  
  def genFail[A]: Gen[Task[A]] =
    Arbitrary.arbitrary[Throwable].map(Task.fail)

  def genAsync[A: Arbitrary]: Gen[Task[A]] = 
    Arbitrary.arbitrary[(Either[Throwable, A] => Unit) => Unit].map(f => 
      Task.async{ registered => f(e => registered(\/.fromEither(e)))}
    )

  def genNestedAsync[A: Arbitrary: Cogen]: Gen[Task[A]] =
    Arbitrary.arbitrary[(Either[Throwable, Task[A]] => Unit) => Unit]
      .map(f => 
        Task.async{ registered:((Throwable \/ Task[A]) => Unit) => 
          f(e => registered(\/.fromEither(e)))
        }
        .flatMap(x => x)
      )

  def genBindSuspend[A: Arbitrary: Cogen]: Gen[Task[A]] =
    Arbitrary.arbitrary[A].map(Task.apply(_).flatMap(Task.now))

  def genFlatMap[A: Arbitrary: Cogen]: Gen[Task[A]] =
    for {
      ioa <- Arbitrary.arbitrary[Task[A]]
      f <- Arbitrary.arbitrary[A => Task[A]]
    } yield ioa.flatMap(f)

  def getMapOne[A: Arbitrary: Cogen]: Gen[Task[A]] =
    for {
      ioa <- Arbitrary.arbitrary[Task[A]]
      f <- Arbitrary.arbitrary[A => A]
    } yield ioa.map(f)

  def getMapTwo[A: Arbitrary: Cogen]: Gen[Task[A]] =
    for {
      ioa <- Arbitrary.arbitrary[Task[A]]
      f1 <- Arbitrary.arbitrary[A => A]
      f2 <- Arbitrary.arbitrary[A => A]
    } yield ioa.map(f1).map(f2)

}