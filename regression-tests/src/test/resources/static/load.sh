#!/bin/bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd $SCRIPT_DIR
VYNEHOST=${1:-localhost}
help="possible envs are localhost, local, int, int2, preprod, prod"
tools_version=1.0.206193-R


for ff in $(ls -1 *.csv) ; do
   type=$(echo $ff | sed s/.csv//)

   echo "Loading $ff to $type to ${vyneurl}"
   request="http://${VYNEHOST}:9022/api/ingest/csv/${type}?delimiter=,&firstRecordAsHeader=true&containsTrailingDelimiters=false"
   accept="Accept: application/json, text/plain, */*"
   content="Content-Type: text/plain"
   curlcmd=$(echo curl -s --data-binary @${ff} -w "http_code=%{http_code}" -H "'"$accept"'" -H "'"$content"'" "'"$request"'")
   echo "${curlcmd}" > curls.sh
   . ./curls.sh
   echo ""
done
rm curls.sh
