# Black Rook Small

Copyright (c) 2020 Black Rook Software.  
[https://github.com/BlackRookSoftware/Small](https://github.com/BlackRookSoftware/Small)

[Latest Release](https://github.com/BlackRookSoftware/Small/releases/latest)    
[Online Javadoc](https://blackrooksoftware.github.io/Small/javadoc/)


### NOTICE

This library is currently in **EXPERIMENTAL** status. This library's API
may change many times in different ways over the course of its development!


### Required Libraries

Any servlet 2.X+ container implementation (javax.servlet-api.jar).  
Any Websocket container implementation (javax.websocket-api.jar).  

Most servlet container servers will provide implementations of these.

### Required Java Modules

[java.xml](https://docs.oracle.com/en/java/javase/11/docs/api/java.xml/module-summary.html)  
* [java.base](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/module-summary.html)  


### Introduction

This library contains classes for Java web application creation. For use with servlet containers.


### Why?

The world needs a singleton management system for removing the tedium of writing servlet
applications, and something less hefty than Spring that also doesn't need the Apache Commons bloat.
Lots of flexible stubs, but enforces rigidity where it counts.


### Library

Contained in this release is a series of classes that route HTTP requests and fulfill them.


### Compiling with Ant

To download dependencies for this project, type (`build.properties` will also be altered/created):

	ant dependencies

To compile this library with Apache Ant, type:

	ant compile

To make Maven-compatible JARs of this library (placed in the *build/jar* directory), type:

	ant jar

To make Javadocs (placed in the *build/docs* directory):

	ant javadoc

To compile main and test code and run tests (if any):

	ant test

To make Zip archives of everything (main src/resources, bin, javadocs, placed in the *build/zip* directory):

	ant zip

To compile, JAR, test, and Zip up everything:

	ant release

To clean up everything:

	ant clean
	

### Javadocs

Online Javadocs can be found at: [https://blackrooksoftware.github.io/Small/javadoc/](https://blackrooksoftware.github.io/Small/javadoc/)


### Other

This program and the accompanying materials are made available under the 
terms of the LGPL v2.1 License which accompanies this distribution.

A copy of the LGPL v2.1 License should have been included in this release (LICENSE.txt).
If it was not, please contact us for a copy, or to notify us of a distribution
that has not included it. 

This contains code copied from Black Rook Base, under the terms of the MIT License (docs/LICENSE-BlackRookBase.txt).
