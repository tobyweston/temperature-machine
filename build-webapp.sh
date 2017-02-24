#!/usr/bin/env bash

# pre-conditions
if ! git diff-index --quiet HEAD --; then
    echo "Outstanding changes, commit or revert with git before deploying the web-app"
    exit -1
fi

UI=target/web-app
ROOT=`pwd`

# download latest web-app
rm -rf $UI
mkdir -p $UI
cd $UI
git clone https://github.com/tobyweston/temperature-machine-ui.git

# prepare web-app
cd temperature-machine-ui
npm update
npm run build

# copy into resources folder and sort git out
cd ${ROOT}/src/main/resources
git status | grep deleted | awk '{print $2}' | xargs git rm
cp -R build/ ${ROOT}/src/main/resources/
cd ${ROOT}/src/main/resources/
git add .
git commit -m "updated web-app from http://github.com/tobyweston/temperature-machine-ui"

cd ${ROOT}