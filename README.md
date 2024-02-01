# NeTEx-Reader

## Run

With docker:

	docker run -it harbor.bernmobil.ch/sip/netex-reader:latest <arguments>

The supported arguments are explained in the next section. Instead of providing command line arguments, it's also
possible to use environment variables (also explained in the next section).

The process needs around 1 GB of RAM. With default settings, the Java process will only use 25% of the pod's memory
as heap space. To increase this, use the environment variable JAVA_TOOL_OPTIONS with the following values:

	JAVA_TOOL_OPTIONS="-Xmx1024M -Xms1024M"

The pod will need slightly more RAM, for example 1.5 GB. Different values for java heap size and the pod's memory
may work as well, depending on the input data and maybe also other factors.

The process spawns several threads to read and store the data in parallel. Therefore the pod can benefit of multiple
cores.

## Help
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

