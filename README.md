# HTTP Log Monitor

## Installation Instructions


## Running Instructions


## Next Steps

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

### Logging
