# MythTV leanfront: Android TV frontend for MythTV

This is based on a clone of the sample Videos By Google app, designed to run on an Android TV device (such as the Shield or Amazon Fire Stick). It uses the Leanback Support library which enables you to easily develop beautiful Android TV apps with a user-friendly UI that complies with the UX guidelines of Android TV.

## Features

- 4K video plays at 60fps with full 4K resolution. This is currently not achievable with the android port of mythfrontend.
- The application uses exoplayer, which is the player code used by youtube, Amazon Prime and others. As such it will be able to handle new capabilities that are released on Android TV.
- It plays recordings and videos from a MythTV backend. All recordings are presented in a way that is consistent with other leanback applications. The first screen shows a list of recording group. You can drill down to a list of titles in a recording group.
- This application uses the MythTV api to communicate with the backend. It needs no access to the database password, and will work on all versions of mythbackend from v29 onwards. It may work on older versions if the apis are available on the MythTV backend.
- Voice search within the application is supported.
- With backend on master or recent MythTV V30 this frontend will prevent idle shutdown on the backend. On older backends you need to take steps to ensure the backend does not shut down while playback is occurring.
- Bookmarks are supported. Bookmarks can be stored on MythTV (for recordings) or on the local leanback frontend (for recordings or videos). In cases where there is no seektable the system stores the bookmark on MythTV based on an assumed frame rate. The frame rate can be set in the Settings page. If the frame rate set is different from the actual frame rate, the location of the bookmark set here will be incorrect when viewed from mythfrontend. Not thet video bookmarks will always be stored locally.
- The "Watched" flag is set if you get to the end of the recording during playback. To ensure it is set, press forward to get to the end before exiting playback.
- There is a delete/undelete option so that you can delete shows after watching. Also set watched or unwatched and remove bookmark options.
- There is a zoom icon and an aspect icon so that you can expand letterbox recordings and correct wrongly stretched recordings.
- There is an icon to pin the enlargement to the top, middle or bottom. If you want to hide a ticker at the bottom, you can pin to the top then enlarge, which will leave the top in place and enlarge downwards so that the ticker is off screen.
- Videos do not currently support deletion or bookmarks stored on MythTV. Bookmarks for videos are stored locally on the android tv device.
- Wakeup of master backend is supported via setup.
- Sort order of recordings can be customized.
- Subtitles (Closed captions) are supported.
- At the end of a recording playback, you can advance to the next episode or any episode without returning to the main list.
- You can play in-progress recordings and the application will follow the progress as the recording continues. This way you almost have LiveTV support, if you first start a recording or a LiveTV session via mythfrontend, and then refresh the list in leanfront.
- Video playback is exlusively via hardware assisted mediacodec.
- Audio playback is supported using mediacodec (hardware) or ffmpeg (software). By default it will use mediacodec if it can, and will switch to ffmpeg if there is a media format not supported by mediacodec. There is a setting where you can change this default and force either mediacodec or ffmpeg.
- Audio playback supports digital passthrough for AC3 and other digital formats if they are supported on your sound system. It also supports downmix to stereo if you do not have a system that supports AC3.

## Main Screen

- A list of recording groups is displayed on the left with titles in the group in a scrolling row on the right. Select a group and press enter to open the screen with that group's contents.
- There is a row for "All" at the top.
- After the recording groups there is a row for "Videos", which shows the MythTV Videos by directory.
- There is a row labeled "Settings" at the bottom. select "Settings" and press enter to see and update program settings.
- There is a "Refresh" icon on the settings row to refresh the list of recordings and videos from the backend. Note that the list is also refreshed after using Settings.

## Playback

- Pressing Enter, up or down brings up the OSD playback controls. Note if you have enabled up/down jumping then up and down will cause a jump instead.
- Left and right arrow will skip back and forward. Holding down the arrow moves quickly through the video. The number of seconds for forward and back skip are customizable in Settings.
- Up and down arrow can be used for bigger jumps by setting a jump interval in settings. I recommend against using this because it interferes with navigation in the OSD. You can move very quickly through playback by holding down left or right arrow`, so jump is not really needed. Jumping can be disabled by setting blank or 0 in the jump interval in Settings. When jumping with up and down arrows, the arrow buttons are disabled for up/down use in the OSD, and this can cause confusion.

## Playback controls (OSD)

![](PlaybackExample.png)

The following controls are available when pressing enter during playback. Select an icon and press enter to apply it.

### Top Row of Controls

1. Pause/Resume: Switches between pause icon and play icon depending on the current state.
2. Previous track: This plays the previous recording or video in the list without needing to exit from playback (see related videos, below).
3. Rewind: This skips back by the time set in the settings.
4. Fast Forward: This skips forward by the time set in the settings.
5. Next track: This plays the next recording or video in the list without needing to exit from playback (see related videos, below).

### Progress Bar

* Progress bar: This shows playback position plus time played and total time. While this is focused you can use left and right arrows to skip back and forward. Holding the arrow down moves quickly through the recording. While this is focused, pressing Enter pauses and resumes.

### Bottom Row of controls

1. CC: Turns on or off captions (subtitles). If there are multiple languages this rotates among them.
2. Zoom: Changes the picture size. Pressing this rotates among several standard zoom amounts.
3. Aspect: Stretch or squeeze the picture in case it is showing at the wrong aspect ratio. Pressing this rotates between several common aspect ratios.
4. Up/down: If the picture has been resized, moves the picture up or down. There are three positions, aligned on top, middle, or bottom. For use when you want to cut off the top or bottom of the picture, after zooming to a bigger size.
5. Slow down: Slows playback speed by increments down to a minimum of 50%.
6. Speed up: Speeds up playback by increments to a maximum of 800%.

**Note:** To use *slow down* or *speed up* you have to disable digital audio passthrough, by either selecting *Stereo* in Android settings or selecting *FFmpeg* in leanfront settings.

### Related videos

- Related videos (press down arrow to see them). Other videos / recordings in the current group. You can select one of these to play instead of the current playing video. That cancels the current playback.

## Restrictions

- There is limited support for watching LiveTV at present.
- Slave backends are currently only supported if you use "Master backend override" or recordings are available on the master backend.

## To Do List

Possible additions.

Further development will continue. These are some possible additions.

- Allow search from android home screen.
- Allow recommendations from android home screen.
- Amazon specific search and recommendations.
- Support for slave backends.
- Program guide.
- Live TV.

## Building

- Download and install [Android Studio][studio]. Also download the latest ndk and Cmake from within android studio.
- In the $HOME/Android directory create a link to the ndk, for example android-ndk -> Sdk/ndk/21.0.6113669
- In the app/src/main/jni/ directory, run download_ffmpeg.sh and build_ffmpeg.sh.
- Open the project in [Android Studio][studio].
- Compile and deploy to your Android TV device (such as a Shield or Amazon fire stick). 
- It can also be run with an android emulator, but the emulator that comes with android studio does not support MPEG2 playback, so you need to play an h264 or h265 recording.
- If you do not want to build this yourself, there is a package at https://dl.bintray.com/bennettpeter/generic/mythtv_leanfront/

## Running

Start up the app. There is an entry on the main screen at the end called "settings". There you need to enter the backend ip address. There are other options available here.

If using backend earlier than fixes/30 of Nov 12 2019 or Master of October 31 2019, make sure the backend is not set up for automatic shutdown when inactive. Otherwise it may shut down during playback.

## License

Licensed under the GNU GPL version 3 or later. See the [LICENSE file][license] for details.

[studio]: https://developer.android.com/tools/studio/index.html
[license]: LICENSE
