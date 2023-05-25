#!/bin/sh
docker build --platform linux/amd64 -t sqliteextracter .
docker cp $(docker create --platform linux/amd64 -t sqliteextracter):/usr/lib/x86_64-linux-gnu/libsqlite3.a libsqlite3.a
