#!/usr/bin/env bash
set -e # e exit on error and x output commands to run prefixed with +
set -u

# pre-conditions
if ! git diff-index --quiet HEAD --; then
    echo "Outstanding changes, commit or revert with git before deploying the web-app"
    exit -1
fi
if [ -z "$1" ] 
then
    echo "Usage: ./release_debian_package <pgp passphrase>"
    exit -1
fi

RELEASE_FOLDER=target/release-debian-package
ROOT_FOLDER=$(pwd)
TARGET_FOLDER=debian
TARGET_DISTRIBUTION=stable
APTLY_PUBLISH=debian/aptly/public

# build man pages (prerequisite `gem install ronn` and `gem install rdiscount`)
echo "Building man pages..."
cd src/linux/usr/share/man/man1/
ronn --roff temperature-machine*.md
cd ${ROOT_FOLDER}

# build .deb package
echo "Building debian package..."
sbt clean debian:packageBin

# update debian repository (prerequisite required `brew install aptly`)
# this assumes the repository has previously been created (`aptly create`)
echo "Adding package to repository..."
aptly -config=debian/.aptly.conf repo add badrobot-releases ${TARGET_FOLDER}

# update aptly database in git
git add debian/aptly/db
git commit -m "updating aptly db after adding new package to the repository" -- debian/aptly/db

echo "Publishing repository..."
aptly -config=debian/.aptly.conf -architectures=armhf -gpg-key=00258F48226612AE -passphrase=${1} publish update stable

# update aptly database in git
git add debian/aptly/db
git commit -m "updating aptly db after (re)publishing" -- debian/aptly/db

# download robotooling
echo "Downloading robotooling..."
rm -rf ${RELEASE_FOLDER}
mkdir -p ${RELEASE_FOLDER}
cd ${RELEASE_FOLDER}
git clone git@github.com:tobyweston/robotooling.git
cd robotooling
git checkout gh-pages

# copy repository into robotooling ready to serve via HTTP
echo "Copy repository into robotooling gh-pages..."
git rm -r debian
mkdir -p debian
cp -R ../debian/aptly/public/* debian
git add debian
git commit -m "updating debian repository (created by aptly)"

# run the robotooling scripts to update html and push the release to gh-pages
echo "Pushing robotooling to Github gh-pages..."
cd ${ROOT_FOLDER}/${RELEASE_FOLDER}/robotooling
./update.sh

echo "All done. Everything is Ok."