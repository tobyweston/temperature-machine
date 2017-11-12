---
layout: docs
title: Build From Source
---

# Build From Source

To get the software on the box, I tend to do the following;

1. Clone the repository on the Pi
2. Run `sbt -J-Xmx512m -J-Xms512m assembly` from a terminal (memory set low for the Pi Zero). This might take around 30m on the Pi Zero.
3. Run `./start.sh &`, `./start-server.sh room1 room2 room3` or `./start-client.sh` from the checked out folder

You can also read my [blog post](http://baddotrobot.com/blog/2016/03/23/homebrew-temperature-logger/) for more detailed instructions (including automatically logging temperatures on reboot). 

Don't forget to `git pull` the latest version every so often.

> Don't forget to enable 1-wire support by adding `dtoverlay=w1-gpio` to `/boot/config.txt`

## Troubleshooting

SBT will often fail when running on the Pi. Generally, I have to run it twice.

Move this out to it's own file...

### Unable to access jarfile

