# Platform libraries

TBD

### Building

By default, building platform libraries is performed by the bootstrap compiler.

When changes in cinterop are required to build fresh platform libraries, set `kotlin.native.platformLibs.bootstrap=false`
Gradle property. This will force building the libraries with the snapshot compiler (i.e. from the fresh master).

Note: building caches for the libraries is always performed by the snapshot compiler.