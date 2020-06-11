# HTTP Log Monitor

## Installation Instructions
This artifact is packaged as a jar, and therefore it is required to have java installed on your environment.
For details on installing java see [here](https://java.com/en/download/help/download_options.xml).
## Running Instructions
To run this program you'll need:
1. The artifact provided named `http-log-monitor-assembly-1.0.jar`
1. Some sample data akin to `sample_csv.txt`
1. From a command terminal run `java -jar <path-to-jar> <path-to-logfile>` or `java -jar <path-to-jar> -h` for usage

For custom configurations you can add them as follows:
1. Windows: `java "-D<config1.path>=<value1>" "-D<config2.path>=<value2>" -jar <path-to-jar> <path-to-logfile>`
1. Unix: `java -D<config1.path>=<value1> -D<config2.path>=<value2> -jar <path-to-jar> <path-to-logfile>`  

The list of configurations is visible in `application.conf`. A few notable mentions (with their default values) are:
```
monitor.show-debug-stats=true                 \\Show debug statistics when printing metrics to the console
monitor.top-sections-show-count=3             \\Show at most this many sections in the "top sections" per metric window
metrics.report-every.seconds=10               \\Configure the metrics reporting window size
alerts.raise-recover.avg.seconds=120          \\Alerting raise/recovery time
alerts.requests.per-second.threshold=10       \\Total traffic alerting threshold   
windowing.late-data.delay-allowed.seconds=5   \\Late data allowance, or how long does the system wait for late data after updating to a new event time
 ```

## Assumptions
1. Reporting of metrics is done after each consecutive and mutually exclusive time window (tumbling window)
1. Timestamps are in seconds, not milliseconds
1. Taking input from stdin is assumed to mean by shell piping, not interactive mode, i.e. `> echo "filepath" | myProgram`
 

## System Design
![System Design Sketch](src/main/resources/Data%20Flow.png)  

This Akka graph, expects as input a single file.
  - Stage 1: Parse file and transform log lines to our data case class - `LogEvent`.
  - Stage 2: Align events by time, chronologically, by their event time or "wall clock" value.
  - Stage 3: Aggregate all events with same timestamp to an `AggregatedMetrics` case class
  - Broadcast the stream to separate alert and metrics processing
  - Stage M1: Aggregate seconds to multi-second windows
  - Stage M2: Run aggregated metrics thorough an observed collector
  - Stage A1: Transform `AggregatedMetrics` to simpler `EventsWindow`
  - Stage A2: Run `EventWindow`s through a stateful sliding average computation  
  
  Details:
- Stage 1 
    - Looks for the predefined headers reports when it matches with them.
    - Sends a `SentinelEOFEvent` when the End of File ir reached
- Stage 2
    - There's a late data allowance setting: `windowing.late-data.delay-allowed.seconds`
- Stage 3
    - Timestamps are assumed to be in seconds, so `LogEvents` happening in the same second are aggregated to a single `AggregatedMetrics`
- Stage M1
    - Tumbling window size setting: `metrics.report-every.seconds`
- Stage M2
    - This stage requires an `ObservedMetricsCollector`
- Stage A2
    - For each time unit, an average of the previous `alerts.raise-recover.avg.seconds` is computed
    - The underlying `AlertQueue` implements all the alerting functionality required, including accounting for cases when there a long break of log events
    - This stage requires an `ObservedAlertQueue`
     
    


## Next Steps
This solution scales vertically with ease, however there are ways to improve its extendability, stability, 
testability and interoperability:
- Extendability
    - `AlertQueue` consumes `EventWindow` for a hardcoded computation of average events per second.
    This can be expanded to other alerting cases (avg of other values, stdev,...) by defining an `Alertable` trait with
    defined value extracting and computing functions.
    - Implement other `Reporters`, for example, a `networkReporter` or a `fileReporter`, s.t. the output of
    processing multiple files in parallel will be meaningful.
    - Switching the current `AggregatedMetrics` into a trait that is implemented by the existing. 
    This would allow for other metrics to be added without extending the existing class
    - Schema evolution is a problem here, and to solve it we would need an extarnal single source of truth
    for the input schemas, and have code-generated data objects derived by the schemas in the external source.
- Stability
    - `AlertQueue` and `MetricsCollector` are not thread safe, which is fine with this implementation, since each
    File in a multi-file run would instantiate their own (because graph stages are defined with `def` and not `val` and are therefore instantiated when called),
     therefore not bothering each other. Further, the Observer pattern is such that they do not (should not) modify state of the 
     `Subject`, so only one logical execution unit changes their state.
    - The above can be mitigated by implementing custom `GraphStage`s in place of `statefulMapConcat`.
- Testability
    - The unit tests in this project are currently cross dependent, mocks and stubs should be added to change this
- Interoperability
    - Using Out-of-the-Box metrics library: The dropwizard metrics suit (for example [Meter](https://github.com/erikvanoosten/metrics-scala/blob/master/docs/Manual.md#meters))
     would have been perfect for the job, had the requirement been for processing time, rather than event time. 
     As the de-facto scala standard ([Metrics-Scala](https://index.scala-lang.org/erikvanoosten/metrics-scala/metrics-scala/4.0.0?target=_2.12)), 
     and well known JVM metrics library, it would have made the job of integrating the output metrics with other systems an out-of-the-box functionality. 
     However, since a meter is essentially timed internally [see here](https://github.com/dropwizard/metrics/blob/0313a104bf785e87d7d14a18a82026225304c402/metrics-core/src/main/java/com/codahale/metrics/Meter.java#L68), 
     the full metrics suite is not suitable for this task. 