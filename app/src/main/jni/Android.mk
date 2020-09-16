#
# Copyright (C) 2016 The Android Open Source Project
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

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := libavcodec
LOCAL_SRC_FILES := ffmpeg/android-libs/$(TARGET_ARCH_ABI)/$(LOCAL_MODULE).so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := libswresample
LOCAL_SRC_FILES := ffmpeg/android-libs/$(TARGET_ARCH_ABI)/$(LOCAL_MODULE).so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := libavutil
LOCAL_SRC_FILES := ffmpeg/android-libs/$(TARGET_ARCH_ABI)/$(LOCAL_MODULE).so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := ffmpeg_jni
LOCAL_SRC_FILES := ffmpeg_jni.cc
LOCAL_C_INCLUDES := $(LOCAL_PATH)/ffmpeg
LOCAL_SHARED_LIBRARIES := libavcodec libswresample libavutil
LOCAL_LDLIBS := -Lffmpeg/android-libs/$(TARGET_ARCH_ABI) -llog
include $(BUILD_SHARED_LIBRARY)
