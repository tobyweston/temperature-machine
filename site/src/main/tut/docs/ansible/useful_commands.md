---
layout: docs
title: Useful Ad-hoc Commands
---

# Useful Ad-hoc Commands

Note:

| Option | Description  |
|---|---|
| `-m` | module |
| `-a` | command |
| `-u` | user |
      
### Copy Binary JAR to all machines

Build the JAR on your development machine (the control node where Ansible is installed):

    $ sbt clean assembly
    
Then copy the artifact into each machines `target` folder (and avoid having to compile on each Pi). In this example `temperatures` is the group of machines in my inventory file and you'd run it form the source folder of `temperature-machine`.

    $ ansible -i ansible/inventory temperatures -m copy -a "src=target/scala-2.12/temperature-machine-2.1.jar dest=/home/pi/code/temperature-machine/target/scala-2.12" -u pi
    
    
### Bounce all Machines

    $ ansible -i ansible/inventory temperatures -a reboot --become -u pi


### Update Sources

Well, this doesn't work but it'd be nice if it did!

    $ ansible -i ansible/inventory temperatures -m git -a "repo=https://github.com/tobyweston/temperature-machine.git dest=/home/pi/code update=yes clone=no" -u pi


### Config Git 

I have a standard set of configs I like to apply on all boxes (notice the single quotes around the `value` arguments):

    $ ansible -i ansible/inventory temperatures -m git_config -a "name=alias.st scope=global value='status -sb'" -u pi
    $ ansible -i ansible/inventory temperatures -m git_config -a "name=alias.last scope=global value='log -1 HEAD'" -u pi
