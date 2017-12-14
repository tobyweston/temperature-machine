---
layout: docs
title: Build From Source
---

# Getting Started

## Installing

On Mac, it's probably easiest to install via Homebrew.

    $ brew install ansible
    
You only need to do this on the **control node**, most likely your laptop.

## Defining your Inventory

Create a file to list all of your Raspberry Pis.

    $ mkdir ansible
    $ touch ansible/inventory
    
Mine looks like this:

    [temperatures]
    study.local
    kitchen.local
    garage.local
    bedroom1.local    

## Setup SSH Keys

On the control node, create a key if you don't already have one.

    $ ssh-keygen -t rsa -C "my key"

Copy the key onto the target inventory. Ensure you include the user before the host name (`pi` in my case). The `ssh-copy-id` command copies your public key to `~/.ssh/authorized_keys` file on the target machine.

    $ ssh-copy-id -i id_rsa.pub pi@study.local
    
    /usr/bin/ssh-copy-id: INFO: Source of key(s) to be installed: "id_rsa.pub"
    /usr/bin/ssh-copy-id: INFO: attempting to log in with the new key(s), to filter out any that are already installed
    /usr/bin/ssh-copy-id: INFO: 1 key(s) remain to be installed -- if you are prompted now it is to install the new keys
    pi@study.local's password: 
    
    Number of key(s) added:        1
    
    Now try logging into the machine, with:   "ssh 'pi@study.local'"
    and check to make sure that only the key(s) you wanted were added.     

As it says, try `ssh`ing into the box. You shouldn't need to type a passphrase. If you do, you [may need to update your config](https://help.github.com/articles/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent/#adding-your-ssh-key-to-the-ssh-agent).


## Test

Try pinging all the machines with the following (`-i` specifies the inventory file):

    $ ansible -i ansible/inventory all -m ping -u pi
    
Don't forget to set the user with `-u pi`.

You'll see something like this.

```
bedroom1.local | SUCCESS => {
    "changed": false, 
    "failed": false, 
    "ping": "pong"
}
kitchen.local | SUCCESS => {
    "changed": false, 
    "failed": false, 
    "ping": "pong"
}
study.local | UNREACHABLE! => {
    "changed": false, 
    "msg": "Failed to connect to the host via ssh: ssh: Could not resolve hostname study.local: nodename nor servname provided, or not known\r\n", 
    "unreachable": true
}
```