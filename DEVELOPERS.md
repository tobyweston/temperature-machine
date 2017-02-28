# Developer's README

## Build the Web App

Run the following script.

    build-webapp.sh

It will download the latest version of the web-app, build it and overwrite the content of `src/main/resources`.

The `resources` folder is statically served from `Server.scala`.


## Build Deployment JAR

Run the following to produce the `temperature-machine-2.0.jar` file.

    sbt assembly
