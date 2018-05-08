---
layout: docs
title: SBT
---

# SBT

An overview of the plugins we use and what they do (they are defined in `plugins.sbt`).

## sbt-buildinfo

<p class="bg-warning">
Configured in <code>version.sbt</code>.
</p>

Auto-generates classes containing the major/minor version number plus the current Git SHA and makes it available in the code.


## sbt-scoverage

Run the following to get an idea of test coverage.

    sbt clean coverage test coverageReport

Check out the generated report under `target/scala-2.12/scoverage-report`. Remember good coverage doesn't always mean good tests!

## sbt-release 

<p class="bg-warning">
Configured in <code>release.sbt</code>.
</p>

Used minimally as a convenience to tag a release.


## sbt-assembly

<p class="bg-warning">
Configured in <code>assembly.sbt</code>.
</p>

The Assembly plugin will generate a "fat" jar. Run:

    sbt assembly

...to create (for example) `target/`.


## sbt-native-packager

<p class="bg-warning">
Configured in <code>package.sbt</code>.
</p>

The Native Packager plugin will take the output of the [Assembly](#sbt-assembly) plugin and package it up as a deployment artifact. In our case, this takes the form of a Debian package (`.deb`) that can be installed via `dpkg` and `apt-get` on the Raspberry Pi.

To prepare a package, run the following:

    sbt debian:packageBin

To perform a traditional "release", see the [Release](release.html) section.


## sbt-proguard

<p class="bg-warning">
Configured in <code>proguard.sbt</code>.
</p>


We use the Proguard plugin purely to reduce the file size of the deployment artifact. It reduces the ~50MB artifact to around 10MB, which is pretty cool.


## sbt-microsite

<p class="bg-warning">
Configured in <code>site.sbt</code>.
</p>

Run `sbt site/makeMicrosite` to create the site you're reading now. Find it in `site/target/jekyll` once built.

You should be able to preview the site using Jekyll to serve it.

    $ cd site/target/jekyll
    $ jekyll serve

If you're going to publish it (`sbt site/publishMicrosite`), make sure you `site/makeMicrosite` first (otherwise you'll see a commit on the [`gh-pages`](https://github.com/tobyweston/temperature-machine/commit/7aa09f7612f3ff38d86de9ab6ae2fe6ba03223c0) branch with `0 commits`)

