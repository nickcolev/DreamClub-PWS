#!/bin/bash
echo "Content-Type: text/plain"
echo
if [ "$REQUEST_METHOD" = "POST" ]; then
	read -n $CONTENT_LENGTH ans
fi
echo "Call to $0 with env"
echo "REQUEST_METHOD=$REQUEST_METHOD"
echo "CONTENT_TYPE=$CONTENT_TYPE"
echo "CONTENT_LENGTH=$CONTENT_LENGTH"
echo "QUERY_STRING=$QUERY_STRING"
echo "REMOTE_ADDR=$REMOTE_ADDR"
echo ""
echo "CONTENT=$ans"
