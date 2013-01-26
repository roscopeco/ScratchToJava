Scratch2Java (Convert Scratch project files to Java)
====================================================

This project aims to convert project files from [MIT's Scratch](http://scratch.mit.edu/) (v1.4) programming environment into Java code, with the ultimate aim of compiling these converted projects for the Android environment.

This software is Copyright &copy; Ross Bamford (& contributors) and is Open Source software licensed under the Apache License 2.0. See the included LICENSE file for more details.

Usage
-----

This project requires the [ScratchFileReader](https://github.com/roscopeco/ScratchFileReader) project. The recommended way to build is with Eclipse, having both projects checked out into your workspace.

If you don't use Eclipse, then you can build using Ant. The ant build expects that you have ScratchFileReader checked out in ../ScratchFileReader, and that you have already build that project's Jar with `ant jar`.
