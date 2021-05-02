#!/bin/bash
#
# Copyright (C) 2019 The Android Open Source Project
# Copyright (c) 2019-2020 Peter Bennett
#
# Code from "Exoplayer"
# <https://github.com/android/Exoplayer>
# Modified by Peter Bennett
#
# This file is part of MythTV-leanfront.
#
# MythTV-leanfront is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# MythTV-leanfront is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with MythTV-leanfront.  If not, see <https://www.gnu.org/licenses/>.
#

scriptname=`readlink -e "$0"`
scriptpath=`dirname "$scriptname"`
scriptname=`basename "$scriptname" .sh`
set -e

cd "$scriptpath"

# Clear old builds
rm -rf ffmpeg/android-libs/*

FFMPEG_EXT_PATH="$PWD"
NDK_PATH=$HOME/Android/android-ndk
HOST_PLATFORM="linux-x86_64"
ENABLED_DECODERS=(mp3 aac ac3 eac3 dca truehd mlp vorbis opus flac alac pcm_mulaw pcm_alaw)
JOBS=$(nproc 2> /dev/null || sysctl -n hw.ncpu 2> /dev/null || echo 4)
echo "Using $JOBS jobs for make"
COMMON_OPTIONS="
    --target-os=android
    --enable-static
    --disable-shared
    --disable-doc
    --disable-programs
    --disable-everything
    --disable-avdevice
    --disable-avformat
    --disable-swscale
    --disable-postproc
    --disable-avfilter
    --disable-symver
    --disable-avresample
    --enable-swresample
    --extra-ldexeflags=-pie
    "
TOOLCHAIN_PREFIX="${NDK_PATH}/toolchains/llvm/prebuilt/${HOST_PLATFORM}/bin"
for decoder in "${ENABLED_DECODERS[@]}"
do
    COMMON_OPTIONS="${COMMON_OPTIONS} --enable-decoder=${decoder}"
done
cd "${FFMPEG_EXT_PATH}"
# (git -C ffmpeg pull || git clone git://source.ffmpeg.org/ffmpeg ffmpeg)
cd ffmpeg
git checkout release/4.2
./configure \
    --libdir=android-libs/armeabi-v7a \
    --arch=arm \
    --cpu=armv7-a \
    --cross-prefix="${TOOLCHAIN_PREFIX}/armv7a-linux-androideabi16-" \
    --nm="${TOOLCHAIN_PREFIX}/arm-linux-androideabi-nm" \
    --ar="${TOOLCHAIN_PREFIX}/arm-linux-androideabi-ar" \
    --ranlib="${TOOLCHAIN_PREFIX}/arm-linux-androideabi-ranlib" \
    --strip="${TOOLCHAIN_PREFIX}/arm-linux-androideabi-strip" \
    --extra-cflags="-march=armv7-a -mfloat-abi=softfp" \
    --extra-ldflags="-Wl,--fix-cortex-a8" \
    ${COMMON_OPTIONS}
make -j$JOBS
make install-libs
make clean
./configure \
    --libdir=android-libs/arm64-v8a \
    --arch=aarch64 \
    --cpu=armv8-a \
    --cross-prefix="${TOOLCHAIN_PREFIX}/aarch64-linux-android21-" \
    --nm="${TOOLCHAIN_PREFIX}/aarch64-linux-android-nm" \
    --ar="${TOOLCHAIN_PREFIX}/aarch64-linux-android-ar" \
    --ranlib="${TOOLCHAIN_PREFIX}/aarch64-linux-android-ranlib" \
    --strip="${TOOLCHAIN_PREFIX}/aarch64-linux-android-strip" \
    ${COMMON_OPTIONS}
make -j$JOBS
make install-libs
make clean
./configure \
    --libdir=android-libs/x86 \
    --arch=x86 \
    --cpu=i686 \
    --cross-prefix="${TOOLCHAIN_PREFIX}/i686-linux-android16-" \
    --nm="${TOOLCHAIN_PREFIX}/i686-linux-android-nm" \
    --ar="${TOOLCHAIN_PREFIX}/i686-linux-android-ar" \
    --ranlib="${TOOLCHAIN_PREFIX}/i686-linux-android-ranlib" \
    --strip="${TOOLCHAIN_PREFIX}/i686-linux-android-strip" \
    --disable-asm \
    ${COMMON_OPTIONS}
make -j$JOBS
make install-libs
make clean
./configure \
    --libdir=android-libs/x86_64 \
    --arch=x86_64 \
    --cpu=x86_64 \
    --cross-prefix="${TOOLCHAIN_PREFIX}/x86_64-linux-android21-" \
    --nm="${TOOLCHAIN_PREFIX}/x86_64-linux-android-nm" \
    --ar="${TOOLCHAIN_PREFIX}/x86_64-linux-android-ar" \
    --ranlib="${TOOLCHAIN_PREFIX}/x86_64-linux-android-ranlib" \
    --strip="${TOOLCHAIN_PREFIX}/x86_64-linux-android-strip" \
    --disable-asm \
    ${COMMON_OPTIONS}
make -j$JOBS
make install-libs
make clean

cd "$scriptpath"
rm -rf ../jnilibs
mkdir ../jnilibs
cp -a ffmpeg/android-libs/* ../jnilibs/

echo "ffmpeg build successfully completed"
