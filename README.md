# HTTP Log Monitor

## Installation Instructions
### For Development
For development, I've used the following system/environment/dev tools, 
but any alternatives or equivalents could work equally well. 

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
