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

1. Prepare a draft release using [Github releases](https://github.com/tobyweston/temperature-machine/releases)
1. As the UI project is copied into the backend project as JavaScript assets (using `build-webapp.sh`), the UI project is not tagged or released via Github
1. When ready, run `release-debian-package.sh` to tag and publish the tagged version to the debian repository
1. Update the release in the [Github releases](https://github.com/tobyweston/temperature-machine/releases) page to include the tag and publish

## Versioning

The software is released using a major/minor number scheme with a Git SHA postfix. For example:

    $ temperature-machine -- -v
    temperature-machine 2.1 (56add8)
    
SHA updates are generally used to get small increments out without the overhead of a "proper" release.


# Releasing

## Tag and Release

To do release:
 
1. draft a new release via the Github [release page](https://github.com/tobyweston/temperature-machine/releases)
1. Tag the release consistently with the `version` field in `build.sbt`
1. Create and deploy the Debian package (see below). This will also tag git
1. Update the Github [release page](https://github.com/tobyweston/temperature-machine/releases) with the tag

Post release:

1. Update the `version` field in `build.sbt` (don't bother with a `SNAPSHOT` postfix, we use the SHA to discriminate)

NB. We could look into automating this with the `sbt-release` plugin at some point. This would usually prepare a JAR, tag and copy the JAR to a local maven repo. We're not interested in publishing the JAR via a Maven repository, instead we want to create a Debian package and publish that (so could delegate to our bash script from within a custom release "step").


## Debian Package

To create the Debian package and push it to the [Robotooling](http://robotooling.com/debian) repository, run the following:

    ./release_debian_package.sh
    


