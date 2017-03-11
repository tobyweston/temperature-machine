# Developer's README

## Build the Web App

Run the following script.

    build-webapp.sh

It will download the latest version of the web-app, build it and overwrite the content of `src/main/resources`.

The `resources` folder is statically served from `Server.scala`.


## Build Deployment JAR

Run the following to produce the `temperature-machine-2.0.jar` file.

    sbt assembly


## Plugins

### sbt-scoverage

Run the following to get an idea of test coverage.

    sbt clean coverage test coverageReport

Check out the generated report under `target/scala-2.12/scoverage-report`. Remember good coverage doesn't always mean good tests!


### sbt-assembly

See [Build Deployment JAR](#build-deployment-jar) section above.