---
layout: docs
title: Useful Ad-hoc Commands
---

# Useful Ad-hoc Commands
      
### Copy Binary JAR to all machines

Build the JAR on your development machine (the control node where Ansible is installed):

    $ sbt clean assembly
    
Then copy the artifact into each machines `target` folder (and avoid having to compile on each Pi). In this example `temperatures` is the group of machines in my inventory file and you'd run it form the source folder of `temperature-machine`.

    $ ansible -i ansible/inventory temperatures -m copy -a "src=target/scala-2.12/temperature-machine-2.1.jar dest=/home/pi/code/temperature-machine/target/scala-2.12" -u pi
    
    
### Bounce all Machines

    $ ansible -i ansible/inventory temperatures -a reboot --become -u pi


### Update Sources

    $ ansible -i ansible/inventory temperatures -m git -a "repo=https://github.com/tobyweston/temperature-machine.git dest=home/pi/code update=yes" -u pi
