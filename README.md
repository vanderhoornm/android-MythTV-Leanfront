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
- Bookmarks are supported. Bookmarks can be stored on MythTV or on the local leanback frontend. In cases where there is no seektable the system stores the bookmark on MythTV based on an assumed frame rate. The frame rate can be set in the Settings page. If the frame rate set is different from the actual frame rate, the location of the bookmark set here will be incorrect when viewed from mythfrontend.
- The "Watched" flag is set if you get to the end of the recording during playback. To ensure it is set, press forward or down arrow to get to the end before exiting playback.
- There is a delete/undelete option so that you can delete shows after watching. Also set watched or unwatched and remove bookmark options.
- There is a zoom icon and an aspect icon so that you can expand letterbox rec ordings and correct wrongly stretched recordings.
- Videos do not currently support deletion or bookmarks stored on MythTV. Bookmarks for videos are stored locally on the android tv device.

## Restrictions

- Playback with the shield needs a TV that supports AC3 (I believe all TVs should support that) as the shield is unable to decode AC3 in hardware. The amazon fire stick 4K will decode AC3 in hardware so works on a monitor without AC3 support. You must select surround sound or auto in the shield audio setup.
- There is no support for watching LiveTV or Recordings in progress at present.

## To Do List

Planned additions and fixes.

- Add wakeup of master backend.
- Ability to move picture up and down to get rid of ticker messages at the bottom.
- Retrieve preview picture when there is none found the normal way.
- Customize recording sort order.
- Subtitles.
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
