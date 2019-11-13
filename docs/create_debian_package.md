
# Releasing

Releases are done as binary via debian packages.

1. `sbt-native-packager` creates the debian package (`sbt clean debian:packageBin`)
1. `release_debian_package.sh` will call the above and then publish to my debian repository, dubbed [robotooling](http://robotooling.com/maven/bad/robot/temperature-machine/debian/)
1. User's manually add the repository to their `/etc/apt/sources.list` then update and install via `apt-get`



# Developer Notes

* Providing application lifecyle management.
    * The Java Server Application Archetype provides configurations for common systeloaders like SystemV, Upstart or SystemD. However create a custome solution, which includes stop scripts, PID management, etc. are not part of native packager.
* Providing deployment configurations
    * Native packager produces artefacts with the packageBin task. What you do with these is part of another step in your process.


## Packaging

### Java

Running `sbt universal:packageBin` would copy anything in `src\universal` into the generated zip file but without adding the Java format plugin, no class files would be included (so the zip would basically be empty).

Include `enablePlugins(JavaApplicationPlugin)` and it will copy in all the JARs (including the application). It also creates default startup scripts which, for us, are no good. They're close; they create an executable (and batch file) for each `App` and a general one to take care of the classpath etc.

So you'll get something like the following.

    total 80
    drwxr-xr-x  9 toby  staff    288  5 Apr 11:15 ./
    drwxr-xr-x  4 toby  staff    128  5 Apr 11:15 ../
    -rwxr-xr-x  1 toby  staff   1388  5 Apr 11:15 client*
    -rw-r--r--  1 toby  staff    110  5 Apr 11:15 client.bat
    -rwxr-xr-x  1 toby  staff   1388  5 Apr 11:15 server*
    -rw-r--r--  1 toby  staff    110  5 Apr 11:15 server.bat
    -rwxr-xr-x  1 toby  staff  11484  5 Apr 11:15 temperature-machine*
    -rw-r--r--  1 toby  staff   7530  5 Apr 11:15 temperature-machine.bat
    
Where `client` for example, ends up running the following.

    # execute the main start script
    $SCRIPTPATH/temperature-machine -main bad.robot.temperature.client.Client "$@"

...and `temperature-machine` handles classpath and Java setup etc.

Including 'enablePlugins(JavaServerAppPackaging)' adds some additional stuff.

### Debian

I think the control file will be defaulted if you don't override it (override by adding into `src\debian\DEBIAN`).

Package with `sbt debian:packageBin`.

When the `.deb` package is built, the same scripts are created in `temperature-machine_2.1-SNAPSHOT_all.deb/data/usr/bin`.

The "log" folder seems to default to `/var/lig/temperature-machine` although I don't know what will go in there.

Including 'systemd' will create a default service wrapper under `temperature-machine_2.1_all.deb/data/lib/systemd/system` looking something like this.

    [Unit]
    Description=temperature-machine
    Requires=network.target
    
    [Service]
    Type=simple
    WorkingDirectory=/usr/share/temperature-machine
    EnvironmentFile=/etc/default/temperature-machine
    ExecStart=/usr/share/temperature-machine/bin/temperature-machine
    ExecReload=/bin/kill -HUP $MAINPID
    Restart=always
    RestartSec=60
    SuccessExitStatus=
    User=temperature-machine
    ExecStartPre=/bin/mkdir -p /run/temperature-machine
    ExecStartPre=/bin/chown temperature-machine:temperature-machine /run/temperature-machine
    ExecStartPre=/bin/chmod 755 /run/temperature-machine
    PermissionsStartOnly=true
    LimitNOFILE=1024
    
    [Install]
    WantedBy=multi-user.target


See [Debian Binary Package Building How-To](http://tldp.org/HOWTO/html_single/Debian-Binary-Package-Building-HOWTO/#AEN60) for the full package contents.


### Creating Debian Package/InRelease/Release

Creating an appropriate set of files for a debian repository is a bit involved. As a minimum, we need to create the following:

* Packages / Packages.gz
* Release / InRelease

`dpkg-scanpackages -m` handles the `Packages` file but not the `Release` (see [SO](https://unix.stackexchange.com/questions/403485/how-to-generate-the-release-file-on-a-local-package-repository)).

`aptly` may be an alternative (as `apt-ftparchive` isn't cross-platform).


## Generating `man` pages

Using http://github.com/rtomayko/ronn, having installed via `gem install ronn`.

    $ cd src/linux/
    $ ronn --roff temperature-machine*.md

It turns markdown into roff. Woof!

Need to figure out how to remove the `.md` files from the package though as these get copied over and result in the following warning.

    man: warning: /usr/share/man/man1/temperature-machine.1.md: ignoring bogus filename

     
