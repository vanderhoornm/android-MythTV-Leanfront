# leanfront: MythTV Experimental Android TV frontend

This is based on a clone of the sample Videos By Google app, designed to run on an Android TV device (such as the Shield or Amazon Fire Stick). It uses the Leanback Support library which enables you to easily develop beautiful Android TV apps with a user-friendly UI that complies with the UX guidelines of Android TV.


## Features

- 4K video plays successfully at 60fps with full 4K resolution. This is currently not achievable with the android port of mythfrontend.
- The application uses exoplayer, which is the player code used by youtube, Amazon Prime and others. As such it will be able to handle new capabilities that are released on Android TV.
- This application is in a state of development. New features are being added.
- Currently it will play recordings and videos from a MythTV backend. All recordings are presented in a way that is consistent with other leanback applications. The first screen shows a list of recording group. You can drill down to a list of titles in a recording group.
- This application uses the MythTV api to communicate with the backend. It needs no access to the database password, and will work on all versions of mythbackend.
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

## Main Screen

- A list of recording groups is displayed on the left with titles in the group in a scrolling row on the right. Select a group and press enter to open the screen with that group's contents.
- There is a row for "All" at the top.
- After the recording groups there is a row for "Videos", which shows the MythTV Videos by directory.
- There is a row labeled "Settings" at the bottom. select "Settings" and press enter to see and update program settings.
- There is a "Refresh" icon on the settings row to refresh the list of recordings and videos from the backend. Note that the list is also refreshed after using Settings.

## Playback

- Pressing Enter, up or down brings up the OSD playback controls. Note if you have enabled up/down jumping then up and down will cause a jump instead.
- Left and right arrow will skip back and forward. Holding down the arrow moves quickly through the video. The number of seconds for forward and back skip are customizable in Settings.
- Up and down arrow can be used for bigger jumps by setting a jump interval in settings. I recommend against using this because it interferes with navigation in the OSD. You can move very quickly through playback by holding down left or right arrow. Jumping can be disabled by setting blank or 0 in the jump interval in Settings. When jumping with up and down arrows, the arrow buttons are disabled for use in the OSD, and this can cause confusion.

## Playback controls (OSD)

The following controls are available when pressing enter during playback. Select an icon and press enter to apply it.

- Pause/Resume.
- Previous track. This plays the previous recording or video in the list without needing to exit from playback (see related videos, below).
- Rewind. This skips back by the time set in the settings.
- Fast Forward. This skips forward by the time set in the settings.
- Next track. This plays the next recording or video in the list without needing to exit from playback (see related videos, below).
- Progress bar. This shows playback position plus time played and total time. While this is focused you can use left and right arrows to skip back and forward. Holding the arrow down moves quickly through the recording. While this is focused, pressing Enter pauses and resumes.
- CC icon to turn on or off captions (subtitles). If there are multiple languages this rotates among them.
- Zoom Icon to change the picture size. Pressing this rotates among several standard zoom amounts.
- Aspect icon to stretch or squeeze the picture in case it is showing at the wrong aspect ratio.
- Pin icon (looks like PIP). This pins the picture to the top middle or bottom. For use when you want to cut off the top or bottom of the picture.
- Related videos (press down arrow to see them). Other videos / recordings in the current group. You can select one of these to play instead of the current playing video. That cancels the current playback.


## Restrictions

- Playback with the NVidia Shield needs a TV that supports AC3 (I believe all TVs should support that) as the shield is unable to decode AC3 in hardware. The amazon fire stick 4K will decode AC3 in hardware so works on a monitor without AC3 support. You must select surround sound or auto in the shield audio setup.
- There is no support for watching LiveTV at present. Recordings in progress can be viewed but may not play fully if you catch up to the recording process. This needs some work to be done.

## To Do List

Planned additions and fixes.

- Retrieve preview picture when there is none found the normal way.
- Sort out license. The sample app uses apache license.
- LiveTV and in progress recordings. I don't know if we can support these.
- Allow search from android home screen.
- Allow recommendations from android home screen.
- Amazon specific search and recommendations.

## Building

- Open the project in [Android Studio][studio].
- Compile and deploy to your Android TV device (such as a Shield or Amazon fire stick). 
- It can also be run with an android emulator, but the emulator that comes with android studio does not support MPEG2 or AC3 playback, so you need to play an h264 or h265 recording with non-ac3 audio.
- If you do not want to build this yourself, there is a package at https://dl.bintray.com/bennettpeter/generic/mythtv_leanfront/

## Running

Start up the app. There is an entry on the main screen at the end called "settings". There you need to enter the backend ip address. There are other options available here.

If using backend earlier than fixes/30 of Nov 12 2019 or Master of October 31 2019, make sure the backend is not set up for automatic shutdown when inactive. Otherwise it may shut down during playback.

Make sure you select surround sound or auto in the audio setup (On Shield).

## Additonal Resources

- [Android TV Introduction](http://www.android.com/tv/)
- [Android TV Developer Documentation](http://developer.android.com/tv)
- [Android TV Apps in Google Play Store][store-apps]


## Support

If you need additional help, our community might be able to help.

- Android TV Google+ Community: [https://g.co/androidtvdev](https://g.co/androidtvdev)
- Stack Overflow: [http://stackoverflow.com/questions/tagged/android-tv](http://stackoverflow.com/questions/tagged/android-tv)

## Dependencies

If you use Android Studio as recommended, the following dependencies will **automatically** be installed by Gradle.

- Android SDK v7 appcompat library
- Android SDK v17 leanback support library
- Android SDK v7 recyclerview library

## License

Licensed under the Apache 2.0 license. See the [LICENSE file][license] for details.

[store-apps]: https://play.google.com/store/apps/collection/promotion_3000e26_androidtv_apps_all
[studio]: https://developer.android.com/tools/studio/index.html
[getting-started]: https://developer.android.com/training/tv/start/start.html
[bugs]: https://github.com/googlesamples/androidtv-Leanback/issues/new
[contributing]: CONTRIBUTING.md
[license]: LICENSE
