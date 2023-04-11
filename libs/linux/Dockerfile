# ubuntu 14.04 uses glibc, the same version used by the kotlin toolchain.
# https://youtrack.jetbrains.com/issue/KT-52464/Error-building-Sqlite3-C-Interop-ld.gold-command-returned-non-zero-exit-code-1#focus=Comments-27-6418731.0-0
FROM ubuntu:14.04
RUN apt-get update && apt-get install libsqlite3-dev -y
