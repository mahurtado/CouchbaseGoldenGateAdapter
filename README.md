# Couchbase GoldenGate Adapter

## Goal

Oracle GoldenGate is an Oracle product used for data syncronization between different databases. It uses log-based 
change data capture (CDC) to detect changes in a source database and propagate changes to a target database. 

CouchbaseGoldenGateAdapter is an example adapter using the GoldenGate Java Adapter framework to support 
data synchronization propagated from Oracle to Couchbase.

## Build and Deployment

The source code is provided for building. 
There is also a build version published in the [release](https://github.com/mahurtado/CouchbaseGoldenGateAdapter/releases) oh this repository.
The adapter is in fact just a single library deployed as a GoldenGate Java Adapter.

## Architecture

Let us consider a deployment on two machines, and define which components run on each machine:

* oracleHost: 
  * Oracle Database
  * Oracle GoldenGate

* couchbaseHost:
  * Couchbase database (typically Couchbase runs in a cluster deployed on several machines. 
  The Adapter will run in only one machine of the cluster, and we will refer to it as couchbaseHost)
  * Oracle GoldenGate Adapters

## Data mapping

Oracle database is a relational database. It store data in tables. Each table has a fixed set of columns, 
and uses primary key to identify a row in a table.

On the other hand, Couchbase uses JSON for storing data (a single JSON value is called a Document), 
and a single String as key for Documents. There are no fixed formats for each JSON Document.

In order to map data between both we will use the following conventions

### Conventions

* Each JSON Document will include a root-element equal to the name of the table
* All the attribute names are lower-case
* Couchbase is a key-value document database. The format of the **key** will be derived from the relational primary key as follows:

`[table name in lower case]::[value of field1 of the PK]::[value of field 2 of the PK]:: ...`

* Numeric, boolean and text data types will be preserved in the transformation
* Oracle Date and Timestamps types will be stored in Couchbase as milliseconds since January 1, 1970, 00:00:00 GMT 

### Transformation Example

Oracle row in a table:

location_id | street_address | postal_code | city	| state_province | country_id
----------- | -------------- | ----------- | ----	| -------------- | ----------
1200 | 2017 Shinjuku-ku | 1689 | Tokyo | Tokyo Prefecture | JP
**primary key** |

JSON Document to be stored in Couchbase:

**Key**
```
locations::1200
```
**Value** 
```
  "locations": {
    "street_address": "2017 Shinjuku-ku",
    "state_province": "Tokyo Prefecture",
    "postal_code": "1689",
    "city": "Tokyo",
    "location_id": 1200,
    "country_id": "JP"
  }
```

## Initial Data Loading

In order to make an initial load from Oracle, you must follow the conventions exposed in the previous section.
This work can be done with the tool [oracle2couchbase](https://github.com/mahurtado/oracle2couchbase).

From this point you can synchronize data (update/delete existing records and adding new records).

## Install & Operate

Prerequisites:

1. Oracle Database installed on oracleHost
2. Couchbase deployed on couchbaseHost (among other servers)
3. Declare a Couchbase bucket to hold the data
4. Make a initial load ([oracle2couchbase](https://github.com/mahurtado/oracle2couchbase))

The sequence to operate is as follows:

### Install GoldenGate on oracleHost

Use Oracle documentation available [here](https://docs.oracle.com/goldengate/1212/gg-winux/GIORA/install.htm#GIORA162)


### Install GoldenGate Adapters on couchbaseHost

Use Oracle documentation available [here](http://docs.oracle.com/goldengate/gg121211/gg-adapter/index.html)

### Configure source Oracle Database 



## References




