# spark_struc_streaming_basic_app

## Installation Instructions
### System Installations
For development, I've used the following system/environment/dev tools:

1. [jdk 11](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
1. [sbt](https://www.scala-sbt.org/download.html)
1. [idea community edition](https://www.jetbrains.com/idea/download/#section=windows)

## Running Instructions

## Next Steps

### Configurations
I have implemented an embedded configuration object. 
It would be better to use something like [typesafe config](https://github.com/lightbend/config)
so that a configuration change would not require repackaging of the program.

### Logging
Logging outputs for spark have been silenced. This is obviously not a good
idea for production code - since it would be impossible to address or analyze 
many potential bugs, but I wanted to provide you with a simple output, so that 
the desired result is neatly in the stdout.

There may be error messages due to lack of hadoop-related resources.
- https://stackoverflow.com/questions/35652665/java-io-ioexception-could-not-locate-executable-null-bin-winutils-exe-in-the-ha
