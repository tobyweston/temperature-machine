---
layout: docs
title: Hacking
---

# Hacking 

Aka the `DEVELOPERS` readme, if you want to get your hands dirty with the code, this is the section for you.


## Quick Start (in an IDE)

If you want to play with the source, you can build some test data by running the `Example` app from within IntelliJ.

You can then start up the app from the `Main` class. If you want to override the sensor file location (for the case when you're testing without sensors), use `-Dsensor.location=src/test/resources/examples`.

Check the web page with [http://localhost:11900](http://localhost:11900).


## Build the Web App

The front-end is hosting in another project, [https://github.com/tobyweston/temperature-machine-ui](https://github.com/tobyweston/temperature-machine-ui). Development goes on over there but when it's ready to bring into the temperature-machine, run the following script.

    build-webapp.sh

It will download the latest version of the web-app, build it and overwrite the content of `src/main/resources`.

The `resources` folder is statically served by `Server.scala`.


## Build Deployment JAR

Run the following to produce the `temperature-machine-2.1.jar` file.

    sbt assembly


## Plugins

### sbt-scoverage

Run the following to get an idea of test coverage.

    sbt clean coverage test coverageReport

Check out the generated report under `target/scala-2.12/scoverage-report`. Remember good coverage doesn't always mean good tests!


### sbt-assembly

See [Build Deployment JAR](#build-deployment-jar) section above.


### sbt-microsite

Run `sbt site/makeMicrosite` to create the site you're reading now. Find it in `site/target/jekyll` once built.

You should be able to preview the site using Jekyll to serve it.

    $ cd site/target/jekyll
    $ jekyll serve

If you're going to publish it (`sbt site/publishMicrosite`), make sure you `site/makeMicrosite` first (otherwise you'll see a commit on the [`gh-pages`](https://github.com/tobyweston/temperature-machine/commit/7aa09f7612f3ff38d86de9ab6ae2fe6ba03223c0) branch with `0 commits`)