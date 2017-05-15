#!/bin/bash

if [[ "$1" == "" ]]
then
	root=$HOME/.gedi/build
	target=main
else
	root=$1
	target=jar
fi



rm -rf $root
mkdir -p $root

echo In $root
workspace=`dirname $0`
curd=`pwd`

projects=`cd $workspace/; ls -d */`;

libs=`mktemp`
javas=`mktemp`

echo Discovering files

IFS=' ' read -a proarr <<< $projects
while [[ "${#proarr[@]}" -gt "0" ]]
do
	project=${proarr[0]}
	cpPath="$workspace/${project}.classpath"

if [[ -e "$cpPath" ]]; then
	grep 'kind="lib"' $cpPath | egrep -o 'path=\".*?\"' | cut -f2 -d'"' | sed -e "s!^\([^/]\)!$workspace/${project}\1!" >> $libs
	grep 'kind="src"' $cpPath | egrep -o 'path=\"[^/].*?\"' | cut -f2 -d'"' | sed -e "s!^\([^/]\)!$workspace/${project}\1!" | sed -e "s!src\$!src/*!" >> $javas
fi

proarr=("${proarr[@]:1}")
done


libfiles=`sort -u $libs | xargs`
javaroots=`sort -u $javas | xargs`
rm $libs
rm $javas

echo Copying libs
mkdir -p $root/lib
cp $libfiles $root/lib

echo Copying sourcecode
mkdir -p $root/src
cp -r $javaroots $root/src

cp $workspace/build.xml $root

cd $root
ant $target
cd $curd

