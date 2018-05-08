---
layout: docs
title: Hacking
---

# Hacking 

Aka the `DEVELOPERS` readme, if you want to get your hands dirty with the code, this is the section for you.

1. See the [SBT Plugins](hacking/sbt_plugins.html) for an overview of the SBT plugins we use
1. See the [release](hacking/release.html) instructions on how to release

## Quick Start (in an IDE)

If you want to play with the source, you can build some test data by running the `Example` app from within IntelliJ.

You can then start up the app from the `Main` class. If you want to override the sensor file location (for the case when you're testing without sensors), use `-Dsensor.location=src/test/resources/examples`.

Check the web page with [http://localhost:11900](http://localhost:11900).


## Build the Web App

The front-end is hosted in another project, [https://github.com/tobyweston/temperature-machine-ui](https://github.com/tobyweston/temperature-machine-ui). Development goes on over there but when it's ready to bring into the temperature-machine, run the following script.

    build-webapp.sh

It will download the latest version of the web-app, build it and overwrite the content of `src/main/resources`.

The `resources` folder is statically served by `Server.scala`.



## Scalaz vs. Cats Notes

We started with Scalaz but the latest version of Http4s prefers Cats. The break apart of Scalaz to FS2 etc all seems a bit complicated, so I've captured some notes below.

* [scalaz-stream](https://github.com/scalaz/scalaz-stream) (`scalaz.concurrent.Task`) has become [FS2](https://github.com/functional-streams-for-scala/fs2)
* [cats-effects](https://github.com/typelevel/cats-effect) has some standard `IO` type classes for use with FS2. 
* Until we migrate away from Scalaz (if ever), use the [shims](https://github.com/djspiewak/shims) for interop. Maybe. Sounds like there'll be problems.
* `Task.delay(x)` roughly translates to `IO(x)` (where `x` shouldn't need to be evaluated / should be "pure")