#!/usr/bin/env bash

# assumes you have Ansible setup and a 'ansible/inventory' file setup with machines grouped under 'temperatures'. See http://temperature-machine.com/docs/ansible.html

sbt assembly && ansible -i ansible/inventory temperatures -m copy -a "src=target/scala-2.12/temperature-machine-2.1.jar dest=/home/pi/code/temperature-machine/target/scala-2.12" -u pi

ansible -i ansible/inventory temperatures -a reboot --become -u pi
