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

cd "$scriptpath"

cd ..
rm -rf ffmpeg
git clone git@github.com:FFmpeg/FFmpeg.git ffmpeg
cd ffmpeg
git checkout release/6.0
