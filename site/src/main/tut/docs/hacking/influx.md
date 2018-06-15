---
layout: docs
title: InfluxDB
---

# InfuxDB Notes

Standard APT repositories have an older version of influx available (1.0.2) so we can depend on it in our control file. It doesn't come with `influx` the CLI.

## Setup

### The Structure of the Database

| Name        | Description                                                                                    |
|-------------|------------------------------------------------------------------------------------------------|
| time series | series of temperatures over time                                                               |
| point       | a single measurement (sample) in the time series  `point = (time, measurement, field*, tags*)` |
| measurement | `temperature`, akin to a SQL table                                                             |
| field       | The value, `sensor-1=21.2`, like a column in a table                                        |
| tag         | Meta-data about the value, `host=bedroom` (like a column and tags are indexed so make sense using `WHERE` clauses)                 |

The "line protocol" influxDB is as follows.

```
<measurement>[,<tag-key>=<tag-value>...] <field-key>=<field-value>[,<field2-key>=<field2-value>...] [unix-nano-timestamp]
```

So, for us, our protocol would look like this:

```
temperature,host=bedroom,timezone=UTC sensor-1=23.1,sensor-2=23.4,sensor-3=21.3 1434067467100293230 
```

| Name        | Description                                                                                    |
|-------------|------------------------------------------------------------------------------------------------|
| measurement  | `temperature` |
| fields       | `sensor-1`, `sensor-2`, `sensor-3` |
| tags         | `host`, `timezone` |

### Create a Database

```bash
curl -i -XPOST http://telephone.local:8086/query --data-urlencode "q=CREATE DATABASE temperatures"
```

### Write Temperatures

```bash
curl -i -XPOST 'http://telephone.local:8086/write?db=temperatures' --data-binary 'temperature,host=bedroom,timezone=UTC sensor-1=23.1,sensor-2=23.4,sensor-3=21.3'
```

### Getting Data

```bash
curl -GET 'http://telephone.local:8086/query?pretty=true' --data-urlencode "db=temperatures" --data-urlencode "q=SELECT \"host\", \"timezone\", \"sensor-1\", \"sensor-2\" FROM \"temperature\" WHERE \"host\"='bedroom'"
```

Which results in the following.

```json
{
  "results": [
    {
      "series": [
        {
          "name": "temperature",
          "columns": [
            "time",
            "host",
            "sensor-1",
            "sensor-2",
            "sensor-3",
            "timezone"
          ],
          "values": [
            [
              "2018-06-16T10:40:43.643958357Z",
              "bedroom",
              23.1,
              23.4,
              21.3,
              "UTC"
            ],
            [
              "2018-06-16T10:40:45.105449267Z",
              "bedroom",
              23.1,
              23.4,
              21.3,
              "UTC"
            ],
            [
              "2018-06-16T10:40:48.76742141Z",
              "bedroom",
              23.1,
              23.4,
              21.3,
              "UTC"
            ],
            [
              "2018-06-16T10:40:55.431934275Z",
              "bedroom",
              23.1,
              23.4,
              21.4,
              "UTC"
            ]
          ]
        }
      ]
    }
  ]
}

```