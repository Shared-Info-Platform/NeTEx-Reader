# NeTEx-Reader

## General

This application can be run as a background service that exposes a REST-API and periodically imports new NeTEx
data. Additionally, the application can also be used as a command line tool for manual imports. Both options are
documented in the following sections.

The process needs around 1 GB of RAM. With default settings, the Java process will use 25% of the pod's memory
as heap space. To change this, use the environment variable JAVA_TOOL_OPTIONS with the following values:

	JAVA_TOOL_OPTIONS="-Xmx1024M -Xms1024M"

The pod will need slightly more RAM, for example 1.5 GB. Different values for java heap size and the pod's memory
may work as well, depending on the input data and maybe also other factors.

The process spawns several threads to read and store the data in parallel. Therefore the pod can benefit of multiple
cores.

## Run as background process

### Import

When the application is run as a background process, it runs import and cleanup tasks according to a cron expression
and provides a REST-API. If only the REST-API is required, the cron job can be disabled.

If the cron job is executed, it performs the following steps:

 1. The configured URLs are checked for new NeTEx data releases. If a new release is found, the data is downloaded and
   extracted and a new entry in the admin database is created (no import yet).
 2. NeTEx data is imported for all existing versions (if necessary) and for new versions (if there are any). The import
   starts with the oldest version and ends with the newest. Data is only imported for a limited number of calendar days
   around the current day. The cron job automatically imports new data for existing data releases whenever this time
   window shifts (i.e. after a new calendar day started).
    * If the downloaded and extracted data is not available anymore locally when more data has to be imported at a
      later time, it is automatically re-downloaded from its original URL.
    * If data was imported for a new release, it is validated by comparing it with the imported data of the previous
      release.
 3. The history database is amended if necessary. This means that the data for the current calendar day of the currently
   active NeTEx data release is copied into a separate database. This happens once per day when the cron job detects that
   the history database doesn't already contain an entry for the current day (i.e. it happens after the start of a new calendar
   day). Note that for this to work, the application must be running daily, otherwise the history database will have gaps.
 4. A cleanup is performed that contains the following steps:
     a) First all imported data that is not in the time window anymore is deleted (i.e. data of older calendar days).
     b) If there are more versions per timetable than required, the oldest versions are deleted (unless they have the flags
        `forced` or `keep` set).
     c) If there are versions of old timetables that are not part of the properties anymore, they are deleted when all their
       data was deleted (in the first cleanup-step).
     d) Orphaned files (downloaded/extracted data releases) are deleted if `deleteUnknownResources` is `true`.

The number of NeTEx versions per timetable that exist in the MongoDB cluster in parallel is limited. The cron job keeps the
most active per timetable (the exact number is defined by `maxVersionsToKeep`). If there are versions that have the
`keep` flag set or a version that has the `forced` flag set (both flags are set via API calls) that are not part of the most
active versions, they are kept as well. Incomplete versions (if the initial import hasn't finished yet after downloading a
new version) are also kept for a while and also versions that didn't validate after the initial import. The latter is done
because the validation can be overruled manually and the version could become active, so it should not be deleted immediately.
Incomplete or invalid versions are still deleted after a while, when they are older than the oldest version that is
among the most recent `maxVersionsToKeep`.

Note that versions marked with the `keep` flag are not "archived" in the sense that they are not modified. Versions marked
with the `keep` flag are still updated, i.e. new data is imported and old data is removed if the time window shifts.

There is one version per timetable that is considered "active". For the NeTEx-reader itself this is only important for
filling the history database and for the routes API. Additionally it is also important for some external services (e.g. the
one creating departures for the departures-service). There is no explicit flag to denote the active version. The active version
is the one that has the `forced` flag set (if there is any) or otherwise the most recent version that is complete and valid
(complete means that the initial import was completed and valid means that the validation of the initial import was successful).

### API

The application has two APIs:

 * `/admin/v1`: An API to manage the automatically imported NeTEx versions
 * `/routes/v1`: An API to find routes (lists of stop places) for specific line and operator codes, based on NeTEx data

The application also contains a [GUI](http://localhost:8080/swagger-ui/index.html#/) to use the REST APIs (hostname and port
depend on configuration).


### Properties

If the application is run as a background service, the configuration can be defined in a *.properties file.
Additionally, as usual in spring-boot-appliactions, properties can also be defined as command line arguments
or as environment variables.

| Property | Type | Default | Description |
| -------- | ---- | ------- | ----------- |
| `importCronExpression` | `String` | `0 0 * * * *` | A cron expression that triggers the periodic download & import job |
| `runImportAtStartup` | `Boolean` | `false` | If true then the import job is run at startup, independently of the cron expression. Attention: This blocks the startup until the job is done, therefore the API of the application is not accessible until the import job is finished. |
| `uriPerTimetable` | `Map` (`String` -> `String`) |  | Contains an URI per timetable. Timetable is an arbitrary string, e.g. 2026, 2027, etc. Normally this map will only include an entry for the current timetable, but when timetables change, there can be two for a while. |
| `importDaysInPast` | `Integer` | `3` | How many calendar days before the current day should be imported. Should be more than the equivalent setting in the service that reads netex data for the departures service. |
| `importDaysInFuture` | `Integer` | `10` | How many calendar days after the current day should be imported. Should be more than the equivalent setting in the service that reads netex data for the departures service. |
| `writeCallsCollection` | `Boolean` | `false` | The departures service only needs data from the Journeys collection, so with this property the export of data into the Calls collection can be suppressed. |
| `importDatabasePrefix` | `String` | `netex-autoimport` | The prefix that will be used for databases that contain netex data. It should be a distinct prefix that is not used by any other database in the system because the prefix will also be used to find and delete unreferenced leftover databases. |
| `temporaryFilesDirectory` | `String` | `tmp` | The directory where zip files with netex data will be downloaded and extracted to. It should be a directory that contains no other files or directories because files/directories that are not referenced by any known import version will be deleted periodically. |
| `maxVersionsToKeep` | `Integer` | `3` | If there are more import versions for one timetable than the number specified in this property then they are deleted (except when they are forcibly active or marked to be kept). |
| `maxRelativeDifference` | `Float` | `0.1` | When a new version is imported then it is validated by comparing it to the previous version. For each imported day the number of journeys in the database is evaluated, both for the new and the previous version. Then the ratio between the difference and the average of these two numbers is calculated. The new version is valid if the ratio is smaller than the limit defined by this property. Example: Previous version 100'000 journeys, new version 80'000 journeys => ratio = 20'000 / 90'000 = 0.2222... (i.e. roughly 22%), so with a maxRelativeDifference of 0.25 this would still be valid but with a maxRelativeDifference of 0.20 it would be invalid. |
| `deleteUnknownResources` | `Boolean` | `true` | Whether databases and files/directories that are not referenced by any known import version should be deleted (see comments to `importDatabasePrefix` and `temporaryFilesDirectory`). |
| `historyNumberOfDays` | `Integer` | `30` | Defines the number of days that should be stored in the history database. |
| `mongoConnectionString` | `String` | `mongodb://localhost:27017/` | Connection string for the MongoDB cluster where imported netex data is stored. |
| `adminDatabaseName` | `String` | `netex-admin` | The name of the database where metadata for the automated import is stored. |
| `historyDatabaseName` | `String` | `netex-history` | The name of the database where a history of netex data is stored. |
| `historyExportTimeOfDay` | `Local Time` | `12:00` | Defines the time of day when the history is exported. It affects which version is exported as the "active" version for a day, depending on whether a new netex version was imported before or after this time. |
| `apiDatabaseName` | `String` |  | The netex database that should be used to serve the routes API. If not defined, the application uses the active databases of the automated import, but this property can be used if another database should be used. |

### Monitoring

There are two endpoints for monitoring:

 * Health (`/actuator/health/`): Used for readiness and liveness
 * Info (`/actuator/info`): Contains information about the Git-commit, the build, active version(s) and the last import(s).
   Active versions and last imports are a list (or map) because there can be multiple timetables.

## Run as command line tool

The application can also be used as a command line tool to import NeTEx data. Make sure that manual imports
don't interfere with automatic imports, i.e. use a different database name.

To run the appliaction with docker:

	docker run -it <image-name> <arguments>

The supported arguments are explained in the next section. Instead of providing command line arguments, it's also
possible to use environment variables (also explained in the next section).

### Command line arguments
```
Usage: <main class> [-hV] [-c=<STRING>] [-d=<DIRECTORY>] [-f=<FILE>]
                    [-n=<NAME>] [-t=<DIRECTORY>] [-u=<URL>] [-z=<FILE>]
Imports journeys from a set of NeTEx files. Exactly one input must be defined:
file, directory, zip-file, or url.
  -f, --file=<FILE>       An *.xml file that contains netex data. Can also be
                            defined with environment variable NETEX_FILE.
  -d, --directory=<DIRECTORY>
                          A directory that contains *.xml files with netex
                            data. Can also be defined with environment variable
                            NETEX_DIRECTORY.
  -z, --zip-file=<FILE>   A *.zip file that contains *.xml files with netex
                            data. Can also be defined with environment variable
                            NETEX_ZIP_FILE.
  -u, --url=<URL>         URL to a *.zip file that contains *.xml files with
                            netex data. Can also be defined with environment
                            variable NETEX_URL.
  -t, --temporary-directory=<DIRECTORY>
                          Optional directory where temporary files can be
                            stored (downloaded or unpacked zip files). If not
                            defined, the system default is used. Can also be
                            defined with environment variable
                            NETEX_TEMPORARY_DIRECTORY.
  -c, --mongo-connection-string=<STRING>
                          Connection string for MongoDB. Can also be defined
                            with environment variable
                            NETEX_MONGO_CONNECTION_STRING. Default: mongodb:
                            //localhost:27017/
  -n, --mongo-database-name=<NAME>
                          Name of the database in MongoDB. Can also be defined
                            with environment variable
                            NETEX_MONGO_DATABASE_NAME. Default: netex
  -h, --help              Show this help message and exit.
  -V, --version           Print version information and exit.
```

## Build

This project uses java and maven. 

Build an executable jar file:

    mvn -f netex-importer clean install

Build a docker image:

    mvn -f netex-importer package docker:build
