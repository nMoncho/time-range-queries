CREATE KEYSPACE IF NOT exists time_queries
  WITH replication = {
    'class' : 'SimpleStrategy',
    'replication_factor' : 1
  };

USE time_queries;

CREATE TABLE readings(
  day            DATE, -- We replace 'hour' with 'day' again
  bucket         INT,
  ts             TIMESTAMP,
  device_id      UUID,
  temperature    DOUBLE,
  pressure       DOUBLE,
  PRIMARY KEY ((day, bucket), ts, device_id)
) WITH CLUSTERING ORDER BY (ts ASC, device_id ASC);
