---
layout: docs
title: Hacking
---

# Hacking 

Aka the `DEVELOPERS` readme, if you want to get your hands dirty with the code, this is the section for you.

1. See the [SBT Plugins](hacking/sbt_plugins.html) for an overview of the SBT plugins we use
1. See the [release](hacking/release.html) instructions on how to release

## Quick Start (in an IDE)

You can then start up the app from the `Main` class. 

To override the sensor file location (for the case when you're testing without sensors), use `-Dsensor.location=src/test/resources/examples`.

Check the web page with [http://localhost:11900](http://localhost:11900).

If you want to add some test data, you can run the `Example` app (say, from within IntelliJ).


## Build the Web App

The front-end is hosted in another project, [https://github.com/tobyweston/temperature-machine-ui](https://github.com/tobyweston/temperature-machine-ui). Development goes on over there but when it's ready to bring into the temperature-machine, run the following script.

    build-webapp.sh

It will download the latest version of the web-app, build it and overwrite the content of `src/main/resources`.

The `resources` folder is statically served by `Server.scala`.


## Resource Monitoring via JMX

You can check the resources used on the machines using JMX tools like `jconsole` or `jvisualvm`.

The JMX port used on startup is dynamic to avoid port clashes (see [#66](https://github.com/tobyweston/temperature-machine/issues/66)). This means to connect `jvisualvm` or similar, you have to grep the logs to find the assigned port. For example, if the log file shows the following;

```bash
2019-08-29 20:52:09:719+0000 [sun.management.jmxremote] CONFIG JMX Connector ready at: service:jmx:rmi:///jndi/rmi://study:34327/jmxrmi 
```

...connect `jvisualvm` to `study.local:34327`.

Note that if you startup the application via the `start.sh` (deprecated method), it will assign a specific port `1616` which you use instead. This is only required if you are stopping and starting the application manually and not using `systemctl`.



## Scalaz vs. Cats Notes

We started with Scalaz but the latest version of Http4s prefers Cats. The break apart of Scalaz to FS2 etc all seems a bit complicated, so I've captured some notes below.

* [scalaz-stream](https://github.com/scalaz/scalaz-stream) (`scalaz.concurrent.Task`) has become [FS2](https://github.com/functional-streams-for-scala/fs2)
* [cats-effects](https://github.com/typelevel/cats-effect) has some standard `IO` type classes for use with FS2. 
* Until we migrate away from Scalaz (if ever), use the [shims](https://github.com/djspiewak/shims) for interop. Maybe. Sounds like there'll be problems.
* `Task.delay(x)` roughly translates to `IO(x)` (where `x` shouldn't need to be evaluated / should be "pure")