#!/bin/sh
echo "Content-Type: text/plain"
echo
# How about /system/bin/ls -l $QUERY_STRING
/system/bin/ls -l "$DOCUMENT_ROOT/$QUERY_STRING"
