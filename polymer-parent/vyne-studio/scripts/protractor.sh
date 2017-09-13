#!/usr/bin/env bash

cd ./node_modules/protractor/node_modules

if [ ! -d "./webdriver-manager" ]; then
  ln -fs ../../webdriver-manager/ ./
fi

exit 0;
