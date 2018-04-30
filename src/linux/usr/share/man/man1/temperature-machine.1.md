temperature-machine(1) -- The homebrew data logger
==================================================

## SYNOPSIS

`temperature-machine` `-i`|`--init`

## SPECIAL OPTIONS

These options must include a double-dash (`--`) prefix to escape the parent scripts options. For example, omitting `--`
for the version option will display the parent script version and not the temperature-machine's version.
 
`temperature-machine` `-- -v`|`-- --version`
`temperature-machine` `-- -h`|`-- --help`


## DESCRIPTION

Homebrew temperature data logger based on the DS18B20 sensor. Logs temperature data from one or 
more temperature probes and displays some pretty graphs via the web server.

Requires a configuration file to exist before running. Create one with the `--init` option. 

Run just a **single** server node but as many **clients** as you like.


## FIND OUT MORE

See the `temperature-machine` website at [temperature-machine.com](http://temperature-machine.com).

## OPTIONS

These options control whether the temperature-machine runes in client or server mode.

  * NO OPTIONS:
    Run the **temperature-machine** as either a **server** or **client** as set in the configuration file. 
    
    When run as a **server**, the temperature-machine will be accessible at http://localhost:11900. One or 
    more DS18B20 probes must be connected and data from these will be logged.
    
    When run as a **client** (and a server is run on another machine) the temperature-machine will auto-discover 
    the server's location and start sending temperature data to it (again, one or more DS18B20 probes must be 
    connected). 

  * `-i`, `--init`:
    Create the `temperature-machine.cfg` file under `~/.temperature`. This is required to run the program.
  
Miscellaneous options:

  * `-- -v`, `-- --version`:
    Display the current version. This option must be prefixed with the special option `--` to stop parsing 
    built-in commands from the parent script (sbt-native-packager).

  * `-- -h`, `-- --help`:
    Display some help. This option must be prefixed with the special option `--` to stop parsing 
    built-in commands from the parent script (sbt-native-packager).

## EXAMPLES

    $ temperature-machine --init
    $ temperature-machine --v 
    $ temperature-machine -- --help 
    

## SOURCE

**temperature-machine** is open source and developed in the open at http://github.com/tobyweston/temperature-machine. 

## BUGS 

Bugs are tracked on the Github site http://github.com/tobyweston/temperature-machine/issues.

## GETTING HELP

Read the documentation at [http://temperature-machine.com/docs](http://temperature-machine.com/docs) and ask questions
on Gitter ([https://gitter.im/temperature-machine/Lobby](https://gitter.im/temperature-machine/Lobby)).

## COPYRIGHT

temperature-machine is Copyright (C) 2016-2018 Toby Weston