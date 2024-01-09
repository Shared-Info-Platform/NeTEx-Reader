# NeTEx-Reader

```
Usage: <main class> [-hV] [-c=<STRING>] [-d=<DIRECTORY>] [-f=<FILE>]
                    [-n=<NAME>] [-t=<DIRECTORY>] [-u=<URL>] [-z=<FILE>]
Imports journeys from a set of NeTEx files. Exactly one input must be defined:
file, directory, zip-file, or url.
  -f, --file=<FILE>       An *.xml file that contains netex data
  -d, --directory=<DIRECTORY>
                          A directory that contains *.xml files with netex data
  -z, --zip-file=<FILE>   A *.zip file that contains *.xml files with netex data
  -u, --url=<URL>         URL to a *.zip file that contains *.xml files with
                            netex data
  -t, --temporary-directory=<DIRECTORY>
                          Optional directory where temporary files can be
                            stored (downloaded or unpacked zip files). If not
                            defined, the system default is used.
  -c, --mongo-connection-string=<STRING>
                          Connection string for MongoDB. Default: mongodb:
                            //localhost:27017/
  -n, --mongo-database-name=<NAME>
                          Name of the database in MongoDB. Default: netex
  -h, --help              Show this help message and exit.
  -V, --version           Print version information and exit.
```
