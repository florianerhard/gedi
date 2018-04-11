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

libs=`mktemp tmp.XXXXXX`
javas=`mktemp tmp.XXXXXX`

echo Discovering files

for project in `cd $workspace/; ls -d */`; do
	cpPath="$workspace/${project}.classpath"
eworkspace=`echo $workspace | sed -e 's/\//\\\\\//'`
eproject=`echo $project | sed -e 's/\//\\\\\//'`

if [[ -e "$cpPath" ]]; then
	grep 'kind="lib"' $cpPath | egrep -o 'path=\".*?\"' | cut -f2 -d'"' | sed -e "s/^\([^/]\)/$eworkspace\\/${eproject}\1/" >> $libs
	grep 'kind="src"' $cpPath | egrep -o 'path=\"[^/].*?\"' | cut -f2 -d'"' | sed -e "s/^\([^/]\)/$eworkspace\\/${eproject}\1/" | sed -e "s/src\$/src\\/*/" >> $javas
fi

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
cp $workspace/gedi $root

cd $root
ant $target
cd $curd

