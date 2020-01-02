# Time range queries with Cassandra and Akka Streams

This repository acts as companion code for the [blog post](https://new-grumpy-mentat.blogspot.com/2020/01/time-range-queries-with-cassandra-and.html),
it provides a simple implementation of the patterns discussed on it.

## Running code
Ideally this repository would provide a REST API where to see data being read and written, for now this is given in the
form of tests:

```
sbt test
```

Will trigger the `CassandraRepositorySpec` test.