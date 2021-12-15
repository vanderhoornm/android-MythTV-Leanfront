#!/bin/bash
# Script to copy files from one android tv devoce to another

fromdev=
todev=
filename=
app=org.mythtv.leanfront
stage=/var/tmp/acopy

while (( "$#" >= 1 )) ; do
    case $1 in
        -f)
            if [[ "$2" == "" || "$2" == -* ]] ; then echo "ERROR Missing value for $1" ; error=y
            else
                fromdev="$2"
                shift||rc=$?
            fi
            ;;
        -t)
            if [[ "$2" == "" || "$2" == -* ]] ; then echo "ERROR Missing value for $1" ; error=y
            else
                todev="$2"
                shift||rc=$?
            fi
            ;;
        -a)
            if [[ "$2" == "" || "$2" == -* ]] ; then echo "ERROR Missing value for $1" ; error=y
            else
                app="$2"
                shift||rc=$?
            fi
            ;;
        --stage)
            if [[ "$2" == "" || "$2" == -* ]] ; then echo "ERROR Missing value for $1" ; error=y
            else
                stage="$2"
                shift||rc=$?
            fi
            ;;
        --db)
            filenames="$filenames databases/leanback.db databases/leanback.db-journal"
            ;;
        --settings)
            filenames="$filenames shared_prefs/org.mythtv.leanfront_preferences.xml"
            ;;
        -*)
            error=y
            ;;
        *)
            filenames="$*"
            break
            ;;
    esac
    shift||rc=$?
done

if [[ $fromdev == "" && $todev == "" || $filenames == "" || $error == y ]] ; then
    echo "Copy android files from one device to another or to PC"
    echo "$0 <options> files ..."
    echo "List of Options:"
    echo "-f      xxx  ip address to copy from. If omitted this copies only from"
    echo "             the PC staging directory."
    echo "-t      xxx  ip address to copy to. If omitted this copies only to"
    echo "             the PC staging directory."
    echo "-a      xxx  application (default org.mythtv.leanfront)"
    echo "--db         Add leanfront database to list of files"
    echo "--settings   Add leanfront settings to list of files"
    echo "--stage /xxx Staging directory on this PC, default /var/tmp/acopy" 
    echo "Filenames are relative paths below the android app's directory."
    echo "File or directory names with spaces are not supported."
    echo "Required:"
    echo "    -f -t or both"
    echo "    --db, --settings, or at least one file name"
    echo "Do not duplicate options. That may cause unexpected results."
    exit 2
fi
androidtemp=/sdcard/tmp
set -e
mkdir -p $stage
for filename in $filenames ; do
    bname=$(basename "$filename")
    dname=$(dirname "$filename")
    if [[ $fromdev != "" ]] ; then
        adb disconnect
        adb connect $fromdev
        adb shell run-as $app<<EOF
mkdir -p $androidtemp
rm -f $androidtemp/$bname
cp -f $filename $androidtemp/$bname
EOF
        mkdir -p $stage/$dname
        adb pull $androidtemp/$bname $stage/$dname/
        adb shell run-as $app<<EOF
rm -f $androidtemp/$bname
EOF
        adb disconnect
        echo "File $fromdev:$filename pulled to $stage/$filename"
    fi
    if [[ $todev != "" ]] ; then
        adb connect $todev
        adb shell run-as $app<<EOF
mkdir -p $androidtemp
EOF
        adb push $stage/$filename $androidtemp/
        adb shell run-as $app<<EOF
mkdir -p $dname
cp -f $androidtemp/$bname $filename
rm -f $androidtemp/$bname
EOF
        adb disconnect
        echo "File $stage/$filename pushed to $todev:$filename"
    fi
done
