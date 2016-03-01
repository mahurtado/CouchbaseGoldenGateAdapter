# Couchbase GoldenGate Adapter

## Goal

Oracle GoldenGate is an Oracle product used for data syncronization between different databases. It uses log-based 
change data capture (CDC) to detect changes in a source database and propagate changes to a target database. 

CouchbaseGoldenGateAdapter is an example adapter using the [GoldenGate Java Adapter](https://docs.oracle.com/goldengate/gg121211/gg-adapter/GADAD/intro_java.htm) framework to support 
data synchronization propagated from Oracle to Couchbase. In this way, any write transaction on Oracle database (insert, update or delete) gets propagated to Couhbase database.

![](https://github.com/mahurtado/CouchbaseGoldenGateAdapter/blob/master/cb_ggadapter.jpg)

## How it works

1. Make an initial load from Oracle to Couchbase
2. Install GoldenGate in source and target machines
3. Configure Oracle source database
4. Install and configure CouchbaseGoldenGateAdapter
5. Start the GoldenGate processes

You are done. From this point any change in Oracle gets propagated to Couchbase.

## Build and Deployment

The source code is provided for building. 
There is also an already built version published in the [release](https://github.com/mahurtado/CouchbaseGoldenGateAdapter/releases) oh this repository.
The adapter is in fact just a single jar library deployed as a GoldenGate Java Adapter.

**NOTE**: Oracle Database and Oracle Golden Gate are licensed products. Oracle binaries are not distributed with this tool.

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

![](https://github.com/mahurtado/oracle2couchbase/blob/master/oracle2couchbase_1.jpg)

## Installation & Configuration

Prerequisites:

1. Oracle Database installed on oracleHost
2. Couchbase deployed on couchbaseHost (among other servers)
 
Installation steps:

1. Install GoldenGate on oracleHost
2. Install GoldenGate Adapters on couchbaseHost
3. Configure source Oracle Database 
4. Configure GoldenGate on source machine
5. Configure GoldenGate on target machine
6. Configure Couchbase bucket

The sequence to operate is as follows:

### Install GoldenGate on oracleHost

Use Oracle documentation available [here](https://docs.oracle.com/goldengate/1212/gg-winux/GIORA/install.htm#GIORA162)

### Install GoldenGate Adapters on couchbaseHost

Use Oracle documentation available [here](http://docs.oracle.com/goldengate/gg121211/gg-adapter/index.html)

### Configure source Oracle Database 

Execute with sysdba privileges:

```
ALTER DATABASE ADD SUPPLEMENTAL LOG DATA;  
ALTER DATABASE FORCE LOGGING;  
ALTER SYSTEM SWITCH LOGFILE;  

CREATE TABLESPACE GGATE
  LOGGING
  DATAFILE '/u01/app/oracle/oradata/XE/ggate01.dbf' <<<< PUT_YOUR_OWN_PATH
  SIZE 32m
  AUTOEXTEND ON
  NEXT 32m MAXSIZE 2048m
  EXTENT MANAGEMENT LOCAL;  

CREATE USER GGATE_ADMIN identified by GGATE_ADMIN  
DEFAULT TABLESPACE ggate  
TEMPORARY TABLESPACE temp  
QUOTA UNLIMITED ON GGATE;  

GRANT CREATE SESSION, ALTER SESSION to GGATE_ADMIN;  
GRANT ALTER SYSTEM TO GGATE_ADMIN;  
GRANT CONNECT, RESOURCE to GGATE_ADMIN;  
GRANT SELECT ANY DICTIONARY to GGATE_ADMIN;  
GRANT FLASHBACK ANY TABLE to GGATE_ADMIN;  
GRANT SELECT ON DBA_CLUSTERS TO GGATE_ADMIN;  
GRANT EXECUTE ON DBMS_FLASHBACK TO GGATE_ADMIN;  
GRANT SELECT ANY TRANSACTION To GGATE_ADMIN;  
GRANT SELECT ON SYS.V_$DATABASE TO GGATE_ADMIN;  
GRANT FLASHBACK ANY TABLE TO GGATE_ADMIN;  
GRANT ALTER ANY TABLE TO GGATE_ADMIN; 

EXEC DBMS_GOLDENGATE_AUTH.GRANT_ADMIN_PRIVILEGE('GGATE_ADMIN'); 
EXEC DBMS_STREAMS_AUTH.GRANT_ADMIN_PRIVILEGE('GGATE_ADMIN');

```

### Configure GoldenGate on source machine (oracleHost)

**NOTE**: This example configuration will use the schema HR, provided as example with Oracle databases. Paths provided should be chaged to reflect your installation.

GoldenGate install path on source machine (oracleHost):
/u01/app/oracle/goldengate

Use GoldenGate ggsci tool:

#### Manager configuration 

```
ggsci
> EDIT PARAMS MGR
```

Enter the following. Save changes:

```
PORT 7901
USERID GGATE_ADMIN, PASSWORD GGATE_ADMIN
PURGEOLDEXTRACTS /u01/app/oracle/goldengate/dirdat/*, USECHECKPOINTS
```

#### Capture extract configuration

```
ggsci
> EDIT PARAMS ecb
```

Enter the following. Save changes:

```
EXTRACT ecb
USERID GGATE_ADMIN, PASSWORD GGATE_ADMIN
EXTTRAIL /u01/app/oracle/goldengate/dirdat/et, FORMAT RELEASE 11.2
NOCOMPRESSUPDATES
TABLE HR.*;
```

#### Pump extract configuration

```
ggsci
> EDIT PARAMS pcb
```

Enter the following. Save changes:

```
EXTRACT pcb
RMTHOST oracle2couchbase, MGRPORT 7801
RMTTRAIL /u01/app/oracle/ggadapter/dirdat/rt, FORMAT RELEASE 11.2
PASSTHRU
TABLE HR.*;
```

From ggsci command line:

```
> ADD EXTRACT ecb, TRANLOG, BEGIN NOW
> ADD EXTTRAIL /u01/app/oracle/goldengate/dirdat/et, EXTRACT ecb
> ADD EXTRACT pcb, EXTTRAILSOURCE /u01/app/oracle/goldengate/dirdat/et
> ADD RMTTRAIL /u01/app/oracle/ggadapter/dirdat/rt, EXTRACT pcb
```

#### Create table definition file

In order to make the propper mapping between Oracle and Couchbase (JSON) data types, we will use a table definition from GoldenGate, and will use that information from the Couchbase Adapter. For this example will use the schema hr.

Create a file like this:

```
/u01/app/oracle/goldengate$ vi hrdefgen.prm
```

Edit with this content to get definition for the "hr" schema:

```
DEFSFILE ./dirdef/hr.def PURGE FORMAT RELEASE 11.2
USERID GGATE_ADMIN, PASSWORD GGATE_ADMIN
TABLE hr.*; 
```

Now execute GoldenGate's defgen utility:

```
/u01/app/oracle/goldengate$ ./defgen paramfile ./hrdefgen.prm
```

Now we have created the file /u01/app/oracle/goldengate/dirdef/hr.def. 
We will use this file in the next step.

### Configure GoldenGate on target machine (couchbaseHost)

**Note**: GoldenGate Adapter path on target machine (couchbaseHost) used in this example:
/u01/app/oracle/ggadapter

#### Copy schema definition file

Copy file from source machine (oracleHost):
```
/u01/app/oracle/goldengate/dirdef/hr.def
```
To target machine (couchbaseHost):
```
/u01/app/oracle/ggadapter/dirdef/hr.def
```

#### Install & configure CouchbaseGoldenGateAdapter on couchbaseHost

Copy adapter jar file couchbaseGGhandler.jar (download from [release](https://github.com/mahurtado/CouchbaseGoldenGateAdapter/releases) or build from scratch) to this path:
```
/u01/app/oracle/ggadapter/dirprm
```

The CouchbaseGoldenGateAdapter uses the properties file:
```
/u01/app/oracle/ggadapter/dirprm/tcbase.properties 
```

Sample content:

```
gg.handlerlist=couchbase
gg.handler.couchbase.type=com.goldengate.couchbase.CouchbaseHandler
gg.handler.couchbase.bucketName=HR
gg.handler.couchbase.clusterAddress=oracle2couchbase.com
gg.handler.couchbase.tableDefinitionFileName=/u01/app/oracle/ggadapter/dirdef/hr.def

goldengate.userexit.nochkpt=true  
goldengate.userexit.writers=javawriter
 
javawriter.bootoptions=-Xms64m -Xmx512m -Djava.class.path=/u01/app/oracle/ggadapter/dirprm:/u01/app/oracl
e/ggadapter/dirprm/couchbaseGGhandler.jar:/u01/app/oracle/ggadapter/dirprm/couchbase-core-io-1.2.2.jar:/u
01/app/oracle/ggadapter/dirprm/couchbase-java-client-2.2.2.jar:/u01/app/oracle/ggadapter/dirprm/rxjava-1.
0.15.jar:/u01/app/oracle/ggadapter/ggjava/ggjava.jar
```

Use your own values for properties:

* gg.handler.couchbase.bucketName
* gg.handler.couchbase.clusterAddress
* gg.handler.couchbase.tableDefinitionFileName

#### GoldenGate Adapter replicat configuration 

Use GoldenGate ggsci tool:

```
ggsci
> PURGEOLDEXTRACTS /u01/app/oracle/ggadapter/dirdat/*, usecheckpoints, minkeepdays 3
> add extract tcbase, exttrailsource /u01/app/oracle/ggadapter/dirdat/rt
> edit params tcbase
```

Enter the following. Save changes:

```
EXTRACT tcbase
SETENV ( GGS_USEREXIT_CONF     = "dirprm/tcbase.properties" )
SETENV ( GGS_JAVAUSEREXIT_CONF = "dirprm/tcbase.properties" )
SOURCEDEFS dirdef/hr.def
CUserExit ./libggjava_ue.so CUSEREXIT PassThru IncludeUpdateBefores
GETUPDATEBEFORES
TABLE HR.*;
```

### Configure Couchbase bucket

For the purpose of this exercise, you must create a bucket to hold the data. In this example the bucket is called HR.
Documentation on how to create a Couchbase bucket [here](http://developer.couchbase.com/documentation/server/4.1/clustersetup/create-bucket.html)

## Initial Data Loading

Before we start to update changes in real time, data must be present in advance. In this way we will support "update" and "delete" operations.

In order to make an initial load from Oracle, you must follow the conventions exposed in the "Data mapping" section.

This work can be done with the tool [oracle2couchbase](https://github.com/mahurtado/oracle2couchbase). This tool load data with the same data assumptions exposed in this document.

## Operation

Assuming all the installation steps and initial data loading are done now we will start GoldenGate:

### Start GoldenGate Adapter con target machine (couchbaseHost)

First we will start GoldenGate on target machine:

```
ggsci 
> start mgr
> start tcbase
```

### Start GoldenGate Adapter con source machine (oracleHost)

Second, we will start GoldenGate on source machine:

```
ggsci 
> start mgr
> start ecb
> start pcb
```

From this point you can synchronize data (update/delete existing records and adding new records).

