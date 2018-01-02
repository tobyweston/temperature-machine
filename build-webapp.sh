#!/usr/bin/env bash
set -ex
set -u

# pre-conditions
if ! git diff-index --quiet HEAD --; then
    echo "Outstanding changes, commit or revert with git before deploying the web-app"
    exit -1
fi

UI=target/web-app
ROOT=$(pwd)

# download latest web-app
echo "Downloading web-app..."
rm -rf $UI
mkdir -p $UI
cd $UI
git clone https://github.com/tobyweston/temperature-machine-ui.git

# prepare web-app
echo "Building web-app..."
cd temperature-machine-ui
npm update
npm run build

# remove old version
cd ${ROOT}
git rm -r src/main/resources/images
git rm -r src/main/resources/static
git rm -r src/main/resources/asset-manifest.json
git rm -r src/main/resources/favicon.png
git rm -r src/main/resources/index.html
git commit -m "removing old version of web-app during deployment of new version"

# copy into resources folder and sort git out
mkdir -p ${ROOT}/src/main/resources
cp -R ${ROOT}/target/web-app/temperature-machine-ui/build/ ${ROOT}/src/main/resources/
git add ${ROOT}/src/main/resources/
git commit -m "adding latest version of the web-app (from http://github.com/tobyweston/temperature-machine-ui) $@"

cd ${ROOT}
echo "Ok"