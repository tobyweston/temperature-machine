temperature-machine(1) -- The homebrew data logger
==================================================

## SYNOPSIS

`temperature-machine` `-s`|`--server` 
`temperature-machine` `-c`|`--client`

## DESCRIPTION

Homebrew temperature data logger based on the DS18B20 sensor. Logs temperature data from one or 
more temperature probes and displays some pretty graphs via the web server.

Run a **single** server node and as many **clients** as you like.

## FIND OUT MORE

See the `temperature-machine` website at [temperature-machine.com](http://temperature-machine.com).

## OPTIONS

These options control whether the temperature-machine runes in client or server mode.

  * `-s`, `--server`:
    Run the **temperature-machine** server, accessible at http://localhost:11900. One or more DS18B20 probes should be 
    connected and data from these will be logged.

    When <module> is a simple word, search for files named <module>`.css` in all
    directories listed in the [`temperature-machine_STYLE`](#ENVIRONMENT) environment variable,
    and then search internal styles.

  * `-c`, `--client`:
    Run as a **client** - when a server is run (on another machine) the client will auto-discover it's location and 
    start sending temperature data to it (assuming one or more DS18B20 probes are connected). 

Miscellaneous options:

  * `-w`, `--warnings`:
    Show troff warnings on standard error when performing roff conversion.
    Warnings are most often the result of a bug in temperature-machine's HTML to roff conversion
    logic.

  * `-W`:
    Disable troff warnings. Warnings are disabled by default. This option can be
    used to revert the effect of a previous `-w` argument.

  * `-v`, `--version`:
    Show temperature-machine version and exit.


## EXAMPLES

Build roff and HTML output files and view the roff manpage using man(1):

    $ temperature-machine some-great-program.1.temperature-machine
    roff: some-great-program.1
    html: some-great-program.1.html
    $ man ./some-great-program.1

Build only the roff manpage for all `.temperature-machine` files in the current directory:

    $ temperature-machine --roff *.temperature-machine
    roff: mv.1
    roff: ls.1
    roff: cd.1
    roff: sh.1


## SOURCE

**temperature-machine** is open source and developed in the open at http://github.com/tobyweston/temperature-machine. 

## BUGS 

Bugs are tracked on the Github site http://github.com/tobyweston/temperature-machine/issues.

## GETTING HELP

Read the documentation at [http://temperature-machine.com/docs](http://temperature-machine.com/docs) and ask questions
on Gitter ([https://gitter.im/temperature-machine/Lobby](https://gitter.im/temperature-machine/Lobby)).

## COPYRIGHT

temperature-machine is Copyright (C) 2016-2018 Toby Weston