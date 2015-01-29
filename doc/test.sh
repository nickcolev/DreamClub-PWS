#!/bin/sh
echo "Content-Type: text/plain"
echo
echo "Call to $0 with env"
echo "REQUEST_METHOD=$REQUEST_METHOD"
echo "CONTENT_TYPE=$CONTENT_TYPE"
echo "CONTENT_LENGTH=$CONTENT_LENGTH"
echo "CONTENT=$CONTENT"
