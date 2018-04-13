#!/usr/bin/env bash
set -ex # e exit on error and x output commands to run prefixed with +
set -u

# pre-conditions
#if ! git diff-index --quiet HEAD --; then
#    echo "Outstanding changes, commit or revert with git before deploying the web-app"
#    exit -1
#fi

RELEASE_FOLDER=target/release-debian-package
ROOT_FOLDER=$(pwd)
TARGET_FOLDER=debian
TARGET_DISTRIBUTION=stable

# build package
sbt clean debian:packageBin

# download robotooling
echo "Downloading robotooling..."
rm -rf ${RELEASE_FOLDER}
mkdir -p ${RELEASE_FOLDER}
cd ${RELEASE_FOLDER}
git clone git@github.com:tobyweston/robotooling.git
cd robotooling
git checkout gh-pages

# copy package into robotooling ready to server via HTTP
mkdir -p ${TARGET_FOLDER}/${TARGET_DISTRIBUTION}
cp ${ROOT_FOLDER}/target/temperature-machine_*.deb ${TARGET_FOLDER}/${TARGET_DISTRIBUTION}
cp ${ROOT_FOLDER}/target/temperature-machine_*.changes ${TARGET_FOLDER}/${TARGET_DISTRIBUTION}

# git add the new files? 
# git add ${TARGET_FOLDER}

# create debian package file (perquisite required `brew install dpkg`)
cd ${TARGET_FOLDER}
dpkg-scanpackages -m . | gzip -c > Packages.gz

# run the robotooling update html and push scripts? ./update.sh

cd ${ROOT_FOLDER}
echo "Ok. Now just push the changes"