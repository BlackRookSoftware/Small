# Black Rook Small

Copyright (c) 2020-2022 Black Rook Software.  
[https://github.com/BlackRookSoftware/Small](https://github.com/BlackRookSoftware/Small)

[Latest Release](https://github.com/BlackRookSoftware/Small/releases/latest)    
[Online Javadoc](https://blackrooksoftware.github.io/Small/javadoc/Core/)


### Required Libraries

Any servlet 3.X+ container implementation (javax.servlet-api.jar).  
Any Websocket container implementation (javax.websocket-api.jar).  

Most servlet container servers will provide implementations of these.


### Required Java Modules

[java.xml](https://docs.oracle.com/en/java/javase/11/docs/api/java.xml/module-summary.html)  
* [java.base](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/module-summary.html)  


### Where to Get

* [Maven Central](https://central.sonatype.com/artifact/com.blackrooksoftware/small)  
* [GitHub Releases](https://github.com/BlackRookSoftware/Small/releases/latest)


### Introduction

This library contains classes for Java web application creation. For use with servlet containers.


### Why?

The world needs a singleton management system for removing the tedium of writing servlet
applications, and something less hefty than Spring that also doesn't need the Apache Commons bloat.
Lots of flexible stubs, but enforces rigidity where it counts.


### Library

Contained in this release is a series of classes that route HTTP requests and fulfill them.


### Compiling with Maven

To install/compile this library and make all artifacts with Apache Maven, type:

	mvn install

To compile this library, type:

	mvn compile

To make Maven-compatible JARs of this library, type:

	mvn jar:jar

To make Javadocs:

	mvn javadoc:javadoc

To run tests, type:

	mvn test

To generate a coverage report, type:

	mvn test jacoco:report

To clean up everything:

	mvn clean
	

### Javadocs

Online Javadocs can be found at: [https://blackrooksoftware.github.io/Small/javadoc/Core/](https://blackrooksoftware.github.io/Small/javadoc/Core/)


### Other

This program and the accompanying materials are made available under the 
terms of the LGPL v2.1 License which accompanies this distribution.

A copy of the LGPL v2.1 License should have been included in this release (LICENSE.txt).
If it was not, please contact us for a copy, or to notify us of a distribution
that has not included it. 

This contains code copied from Black Rook Base, under the terms of the MIT License (docs/LICENSE-BlackRookBase.txt).
