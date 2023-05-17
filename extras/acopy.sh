#!/bin/bash
# Script to copy files from one android tv devoce to another

fromdev=
todev=
filename=
app=org.mythtv.leanfront
stage=/var/tmp/acopy
vacuum=0
db=0

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
            filenames="$filenames databases/leanback.db-wal databases/leanback.db-shm"
            db=1
            ;;
        --vacuum)
            vacuum=1
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

if [[ ( $fromdev == "" && $todev == "" || $filenames == "" )
        && $vacuum == 0  || $error == y ]] ; then
    echo "Copy android files from one device to another or to PC"
    echo "$0 <options> files ..."
    echo "List of Options:"
    echo "-f      xxx  ip address to copy from. If omitted this copies only from"
    echo "             the PC staging directory."
    echo "-t      xxx  ip address to copy to. If omitted this copies only to"
    echo "             the PC staging directory."
    echo "-a      xxx  application (default org.mythtv.leanfront)"
    echo "--db         Add leanfront database to list of files"
    echo "--vacuum     Vacuum leanfront database. Requires sqlite3."
    echo "--settings   Add leanfront settings to list of files"
    echo "--stage /xxx Staging directory on this PC, default /var/tmp/acopy" 
    echo "Filenames are relative paths below the android app's directory."
    echo "File or directory names with spaces are not supported."
    echo "Required:"
    echo "    -f -t or both"
    echo "    --db, --settings, or at least one file name"
    echo "    --vacuum can be supplied on its own or with the others"
    echo "Do not duplicate options. That may cause unexpected results."
    echo "Option --vacuum is recommended if uploading a database."
    exit 2
fi
androidtemp=/data/local/tmp
set -e
mkdir -p $stage
if [[ $fromdev != "" && $fromdev != emulator* ]] ; then
    adb connect $fromdev
fi
if [[ $todev != "" && $todev != emulator* ]] ; then
    adb connect $todev
fi

if [[ $fromdev != "" ]] ; then
    echo "Downloading data from $fromdev to $stage"
    for filename in $filenames ; do
        bname=$(basename "$filename")
        dname=$(dirname "$filename")
        mkdir -p $stage/$dname
        adb -s $fromdev shell run-as $app<<EOF > $stage/$dname/$bname
if [ -f $filename ] ; then cat $filename ; fi
EOF
        echo "File $fromdev:$filename pulled to $stage/$filename"
    done
fi

if [[ $todev != "" &&  $db == 1 && $vacuum == 0 ]] ; then
    sizes=$(echo $(stat -c %s $stage/databases/leanback.db-*))
    while [[ "$sizes" != "0 0 0" ]] ; do
        ls -l $stage/databases/
        echo "Database extra files may cause database failure"
        echo "Would you like to fix this by running vacuum (Y|N)?"
        read -e resp
        if  [[ "$resp" == Y ]] ; then
            vacuum=1
            break;
        elif  [[ "$resp" == N ]] ; then
            break;
        fi
    done
fi

if (( vacuum )) ; then
    echo "Vacuuming leanback.db"
    sqlite3 $stage/databases/leanback.db 'VACUUM;'
    rc=$?
    if [[ "$rc" != 0 ]] ; then
        echo "Vacuum failed rc=$rc"
        exit $rc
    fi
    # recreate these files so that they are emptied in the destination
    touch $stage/databases/leanback.db-journal
    touch $stage/databases/leanback.db-wal
    touch $stage/databases/leanback.db-shm
    ls -l $stage/databases/
fi

if [[ $todev != "" ]] ; then
    echo "Uploading data from $stage to $todev"
    for filename in $filenames ; do
        bname=$(basename "$filename")
        dname=$(dirname "$filename")
        if [[ ! -f $stage/$filename ]] ; then
            continue
        fi
        adb -s $todev shell <<EOF
mkdir -p $androidtemp
EOF
        adb -s $todev push $stage/$filename $androidtemp/
        adb -s $todev shell run-as $app<<EOF
mkdir -p $dname
cp -f $androidtemp/$bname $filename
EOF
        adb -s $todev shell<<EOF
rm -f $androidtemp/$bname
EOF
        echo "File $stage/$filename pushed to $todev:$filename"
    done
fi
adb disconnect
