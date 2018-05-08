---
layout: docs
title: Contributing
---

# Release Overview

The general process is perhaps a little unconventional, we have three elements to consider.

1. The [backend](http://github.com/tobyweston/temperature-machine) / server code (Scala)
1. The [front-end](http://github.com/tobyweston/temperature-machine-ui) / UI (JavaScript)
1. The [debian package](http://robotooling.com/debian/) available via `apt-get`

Each is managed separately but should be consistent in terms of the release artifacts.

To do this, we:

1. Tag and release from the backend project using the `sbt-release` plugin and [Github releases](https://github.com/tobyweston/temperature-machine/releases)
1. As the UI project is copied into the backend project as JavaScript assets (using `build-webapp.sh`), the UI project is not tagged or released via Github
1. After tagging, immediately run `release-debian-package.sh` to publish the tagged version to the debian repository

## Versioning

The software is released using a major/minor number scheme with a Git SHA postfix. For example:

    $ temperature-machine -- -v
    temperature-machine 2.1 (56add8)
    
SHA updates are generally used to get small increments out without the overhead of a "proper" release.


# Releasing

## Tag and Release

To do release, run the following

    sbt release

This would usually prepare a JAR, tag and copy the JAR to a local maven repo. We're not interested in publishing the JAR via a Maven repository, instead we want to create a Debian package and publish that.


## Debian Package

