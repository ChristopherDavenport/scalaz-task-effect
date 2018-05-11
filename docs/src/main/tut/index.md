---
layout: home

---
# scalaz-task-effect [![Build Status](https://travis-ci.com/ChristopherDavenport/scalaz-task-effect.svg?branch=master)](https://travis-ci.com/ChristopherDavenport/scalaz-task-effect) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/scalaz-task-effect_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/scalaz-task-effect_2.12)

Cats Effect Instances for Scalaz Concurrent Task. Trying to allow everything
to play nicely together with minimal hardships.

## Quick Start

To use scalaz-task-effect in an existing SBT project with Scala 2.11 or a later version, add the following dependency to your
`build.sbt`:

```scala
libraryDependencies += "io.chrisdavenport" %% "scalaz-task-effect" % "<version>"
```

## Getting Started

```tut
import cats.implicits._
import cats.effect._
import io.chrisdavenport.scalaz.task._

import scalaz.concurrent.Task

val usingCatsSyntax = Task.delay(println("Hello There!")) *> Task.delay(println("Very Convincing"))
usingCatsSyntax.unsafePerformSync

val summonImplicits = Sync[Task].delay(println("I came out to play!"))
summonImplicits.unsafePerformSync

val lifted = LiftIO[Task].liftIO(IO(println("I could have done arbitrary IO")))
lifted.unsafePerformSync
```

## Instances

### Task

- Effect