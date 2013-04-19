Balancer manager updater
========================

This little Java Swing program lets you control several `apache_mod_balancer` instances with one click - enabling and disabling nodes in your cluster very easily.
The load balancer has a horrible web interface, and handling it manually is both annoying, slow and error-prone.

It's still in its infancy, and not very well tested.

Requirements
------------

* JRE1.6 or later
* Maven

Building
--------

After cloning the repository you may build the target jar.
Build like this:

`mvn clean package`

You should get lots of files in a folder called `target`.

Running
-------

To run, do this:

`java -jar target/load-balancer-updater-0.1-jar-with-dependencies.jar`











