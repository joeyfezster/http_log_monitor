# HTTP Log Monitor

## Installation Instructions


## Running Instructions

## Assumptions
1. The system is to report metrics for every 10 second batch (event time)
rather than report every x seconds for the aggregation of the past 10. 
In other words, reports occur for each mutually exclusive 10 second batch of events. 
1. Timestamps are in seconds, not milliseconds

## System Design



## Next Steps

### Paralelism

### Windowing vs Time Alignment
This system design is inspired by a watermarked window paradigm, 
but it is not such a one ([example](https://gist.github.com/adamw/3803e2361daae5bdc0ba097a60f2d554)).
It does, however, use the watermarking approach to align events that 
are ingested out-of-order (event time perspective) to chronological order.
This stream is then used to compute aggregations with a one second granularity, 
which becomes the system's minimal report granularity. These are then
further aggregated to the desired 10 second and 2 minute windows.

### Schema Evolution
The schema here is not provided, but rather a line of "headers". The assumption is made that the pattern shown in the 
sample file is continued for all inputs. While basic checks for numeric strings are made where relevant, this system
does not enforce other parts of the schema. For example - legal HTTP methods or protocols.

Adding new types of log lines would require three changes:
1. Adding the new headders to the app.conf file under `schema.legal-headers`
1. Adding new `LogEvents` extending the `LogLine` sealed trait (must be in the same file)
1. Adding new parsing logic

This is clearly cumbersome and undesireable. 
todo: describe how this can be improved

### System Output

### File eviction

### Using Out-of-the-Box metrics library
The dropwizard metrics suit (such as [Meter](https://github.com/erikvanoosten/metrics-scala/blob/master/docs/Manual.md#meters)) would have
been perfect for the job, had the requirement been for processing 
time, rather than event time.  
As the de-facto scala standard ([Metrics-Scala](https://index.scala-lang.org/erikvanoosten/metrics-scala/metrics-scala/4.0.0?target=_2.12)), and well known JVM metrics
library, it would have made the job of integrating the output metrics
with other systems an out-of-the-box functionality. 
 
However, since a meter is essentially timed internally [see here](https://github.com/dropwizard/metrics/blob/0313a104bf785e87d7d14a18a82026225304c402/metrics-core/src/main/java/com/codahale/metrics/Meter.java#L68), the full metrics suite is 
not suitable for this task. 