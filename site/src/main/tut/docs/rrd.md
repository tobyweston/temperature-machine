---
layout: docs
title: Round Robin Database (RRD)
---

# Round Robin Database

The software writes temperature data into a Round Robin Database ([RRD](https://en.wikipedia.org/wiki/RRDtool)) stored in `~/temperature.rrd`.

A round robin database is a fixed size database meaning you can leave the data logger on for years and you'll never run out of disk (SD card) space. By knowing what you want to monitor ahead of time (your job to configure) and what time series you want to aggregate over (my job to program), we can initialise the database and its size will remain the same. 


## Cleaning Up

Because we have to define what's in the RRD upfront, you'll have to delete the `~/.temperature/temperature.rdd` if the configuration changes. Starting the server for the first time will initialise the RRD; it will create "archives" for every machine it's expecting to monitor. These are passed in as command line arguments when you start up the server (for example `kitchen`, `garage` and `bedroom`).

    $ java -jar temperature-machine-2.0.jar bad.robot.temperature.server.Server kitchen garage bedroom

> Don't forget to set the hostname of each machine to match. So for the above, the server might be named `kitchen` and a RRD "archive" will be created along with `garage` and `kitchen`. If a client with a hostname of `outside` connects, it won't have an archive to write data to and you won't be able to log it's temperatures.


So, if you need to add additional machines after running the server for the first time, you'll need to do the following.

    `rm ~/.temperature/*`

Starting the server up when the `~/temperature.rrd` already exists **doesn't** delete it.

