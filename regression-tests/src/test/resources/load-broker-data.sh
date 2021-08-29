#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd $SCRIPT_DIR
VYNEHOST=${1:-localhost}

postCsvData() {
   type=$1
   for ff in $(ls -1 *.csv) ; do
     now=$(date)
     echo "$now Loading $ff to $type to ${VYNEHOST}"
     request="http://${VYNEHOST}:9022/api/ingest/csv/${type}?delimiter=,&firstRecordAsHeader=true&containsTrailingDelimiters=false"
     accept="Accept: application/json, text/plain, */*"
     content="Content-Type: text/plain"
     curlcmd=$(echo curl -s --data-binary @${ff} -w "http_code=%{http_code}" -H "'"$accept"'" -H "'"$content"'" "'"$request"'")
     echo "${curlcmd}" > curl.sh
     . ./curl.sh
     echo ""
  done
  rm curl.sh
}

postXmlData() {
   type=$1
   for ff in $(ls -1 *.xml) ; do
     echo "Loading $ff to $type to ${VYNEHOST}"
     request="http://${VYNEHOST}:9022/api/ingest/xml/${type}"
     accept="Accept: application/json, text/plain, */*"
     content="Content-Type: text/plain"
     curlcmd=$(echo curl -s --data-binary @${ff} -w "http_code=%{http_code}" -H "'"$accept"'" -H "'"$content"'" "'"$request"'")
     echo "${curlcmd}" > curl.sh
     . ./curl.sh
     echo ""
  done
  rm curl.sh
}

postJsonData() {
   type=$1
   for ff in $(ls -1 *.json) ; do
     echo "Loading $ff to $type to ${VYNEHOST}"
     request="http://${VYNEHOST}:9022/api/ingest/json/${type}"
     accept="Accept: application/json, text/plain, */*"
     content="Content-Type: application/json"
     curlcmd=$(echo curl -s --data-binary @${ff} -w "http_code=%{http_code}" -H "'"$accept"'" -H "'"$content"'" "'"$request"'")
     echo "${curlcmd}" > curl.sh
     . ./curl.sh
     echo ""
  done
  rm curl.sh
}

niceaType=nicea.rfq.RfqCbIngestion
cd $SCRIPT_DIR/nicea
postCsvData $niceaType

troyType=troy.orders.Order
cd $SCRIPT_DIR/troy
postCsvData $troyType

smyrnaType=smyrna.orders.Order
cd $SCRIPT_DIR/smyrna
postCsvData $smyrnaType

persepolisType=persepolis.orders.Order
cd $SCRIPT_DIR/persepolis
postXmlData $persepolisType

philadelphiaType=philadelphia.orders.Order
cd $SCRIPT_DIR/philadelphia
postJsonData $philadelphiaType

magnesiaType=magnesia.priceQuote.PriceQuote
cd $SCRIPT_DIR/magnesia
postCsvData $magnesiaType

knidosType=knidos.orders.Order
cd $SCRIPT_DIR/knidos
postCsvData $knidosType

tenedosTradesType=tenedos.trade.Trade
cd $SCRIPT_DIR/tenedos/trades
postJsonData $tenedosTradesType

tenedosOrdersType=tenedos.orders.Order
cd $SCRIPT_DIR/tenedos/orders
postJsonData $tenedosOrdersType

rfqCbType=bank.rfq.RfqConvertibleBond
cd $SCRIPT_DIR/rfq/cb
postJsonData $rfqCbType

rfqIrdType=bank.rfq.RfqIrdIngestion
cd $SCRIPT_DIR/rfq/ird
postJsonData $rfqIrdType

lesbosFills=lesbos.orders.OrderFilled
cd $SCRIPT_DIR/lesbos/OrderFilled
postCsvData $lesbosFills

lesbosSent=lesbos.orders.OrderSent
cd $SCRIPT_DIR/lesbos/OrderSent
postCsvData $lesbosSent




