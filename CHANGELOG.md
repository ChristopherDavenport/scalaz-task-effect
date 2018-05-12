# changelog

This file summarizes **notable** changes for each release, but does not describe internal changes unless they are particularly exciting.

----

## <a name="0.0.2"></a>New and Noteworthy for Version 0.0.2

Addresses Minor Performance and Race Conditions.

- Improve `runAsync` implementation to execute using `IO.unsafeRunAsync` internally [#10](https://github.com/ChristopherDavenport/scalaz-task-effect/pull/10)
- Address Possible Race Condition in `async` implementation [#9](https://github.com/ChristopherDavenport/scalaz-task-effect/pull/9)

## <a name="0.0.1"></a>New and Noteworthy for Version 0.0.1

Initial Release

- First Implementation of Effect for Task that passes laws.