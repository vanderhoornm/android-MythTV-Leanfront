# leanfront: MythTV Experimental Android TV frontend

This is based on a clone of the sample Videos By Google app, designed to run on an Android TV device (such as the Shield or Amazon Fire Stick). It uses the Leanback Support library which enables you to easily develop beautiful Android TV apps with a user-friendly UI that complies with the UX guidelines of Android TV.


## Features

4K video plays successfully at 60fps with full 4K resolution. This is currently not achievable with the android port of mythfrontend.

This application is is a state of development, but parts are working. It still contains some "google videos" icons and banners and the user interface needs improvement.

Currently it will play recordings from a MythTV backend. All recordings are presented in a way that is consistent with other leanback applications. There is no support yet for recording groups. All recordings in all groups are shown together in the user interface.

This application uses the MythTV api to communicate with the backend. It needs no access to the database password, and will work on all versions of mythbackend. Note that the settings ask for the myth protocol port but the application currently is not using it.

Voice search within the application is supported.

## Restrictions

- Playback with the shield needs a TV that supports AC3 (I believe all TVs should support that) as the shield is unable to decode AC3 in hardware. The amazon fire stick 4K will decode AC3 in hardware so works on a monitor without AC3 support. You must select surround sound or auto in the shield audio setup.

## To Do List

Planned additions and fixes.

- Prevent backend shutdown, except if frontend minimized. Currently the backend could shut down in the middle of playback.
- Use service call to get preview image. Currently it is going direct to the image.
- Use bookmarks to stop and continue playback. Currently it always starts at the beginning and does not save the position on exit.
- Allow delete after watching.
- Recording groups support needed.
- Videos need to be supported (only recordings are currently shown).
- Periodically refresh program list. If recordings change while the app is running it does not know about the changes.
- LiveTV and in progress recordings. I don't know if we can support these.
- Subtitles.
- Clean up icons and images.
- Clean up unused code.
- Improve settings page.
- Do we need grid view? Currently it is there but maybe not useful.
- Sort out license. The sample app uses apache license.
- When exiting playback the display is not focused on the recording just played. It jumps to the top of the list.
- Better error handling, for example if the backend is down or cannot be contacted.
- Anamorphic content is not showing correctly.
- Allow search from android home screen.
- Allow recommendations from android home screen.

## Building

- If you do not want to build this yourself, there is a package at https://dl.bintray.com/bennettpeter/generic/mythtv_leanfront/
- Open the project in [Android Studio][studio].
- Compile and deploy to your Android TV device (such as a Shield or Amazon fire stick). 
- It can also be run with an android emulator, but the emulator that comes with android studio does not support MPEG2 or AC3 playback, so you need to play an h264 or h265 recording with non-ac3 audio.

## Running

Start up the app. There is an entry on the main screen called "Other" with an entry called "settings". There you need to enter the backend ip address. Only the backend ip address and backend port are currently used. Other entries here are for future use or may be removed.

Make sure the backend is not set up for automatic shutdown when inactive. Otherwise it may shut down during playback.

Note that when you exit the settings sidebar it refreshes the list of recordings. This is also a way to refresh the list until we add automatic refresh.

Make sure you select surround sound or auto in the audio setup.

## Features of the sample

- Choose a layout
  - Videos grouped by [category][mainfragment] (See BrowseFragment in [screenshots][screenshots])
  - Freeform [vertical grid][verticalgridfragment] of videos (See Vertical Grid Fragment in [screenshots][screenshots])
- Customize video cards with a [Card Presenter][cardpresenter] (See Card Views in [screenshots][screenshots])
- Display in-depth [details][detailsfragment] about your video
- Play a video
  - [Playback with ExoPlayer2][playbackfragment]
  - [Add extra buttons to control playback][videoplayerglue]
- [Display an error][errorfragment]
- Make your app globally searchable
  - Review searchable training [document][searchable]
     - Creating a [content provider][videoprovider]
     - Defining [searchable.xml][searchable.xml]
     - Receive search intent in [manifest][manifestsearch]
- [Search][searchfragment] within your app
- [Onboard][onboardingfragment] new users (explain new features)
- Customize [preference and settings][settingsfragment]
- Add a wizard with [guided steps][guidedstep]

[screenshots]: https://github.com/googlesamples/androidtv-Leanback#screenshots

[manifestsearch]: https://github.com/googlesamples/androidtv-Leanback/blob/master/app/src/main/AndroidManifest.xml#L79

[searchfragment]: https://github.com/googlesamples/androidtv-Leanback/blob/master/app/src/main/java/com/example/android/tvleanback/ui/SearchFragment.java

[cardpresenter]: https://github.com/googlesamples/androidtv-Leanback/blob/master/app/src/main/java/com/example/android/tvleanback/presenter/CardPresenter.java

[searchable.xml]: https://github.com/googlesamples/androidtv-Leanback/blob/master/app/src/main/res/xml/searchable.xml

[searchable]: https://developer.android.com/training/tv/discovery/searchable.html

[videoprovider]: https://github.com/googlesamples/androidtv-Leanback/blob/master/app/src/main/java/com/example/android/tvleanback/data/VideoProvider.java

[errorfragment]: https://github.com/googlesamples/androidtv-Leanback/blob/master/app/src/main/java/com/example/android/tvleanback/ui/BrowseErrorFragment.java

[mainfragment]: https://github.com/googlesamples/androidtv-Leanback/blob/master/app/src/main/java/com/example/android/tvleanback/ui/MainFragment.java

[detailsfragment]: https://github.com/googlesamples/androidtv-Leanback/blob/master/app/src/main/java/com/example/android/tvleanback/ui/VideoDetailsFragment.java

[verticalgridfragment]: https://github.com/googlesamples/androidtv-Leanback/blob/master/app/src/main/java/com/example/android/tvleanback/ui/VerticalGridFragment.java

[guidedstep]: https://github.com/googlesamples/androidtv-Leanback/blob/master/app/src/main/java/com/example/android/tvleanback/ui/GuidedStepActivity.java

[onboardingfragment]: https://github.com/googlesamples/androidtv-Leanback/blob/master/app/src/main/java/com/example/android/tvleanback/ui/OnboardingFragment.java

[settingsfragment]: https://github.com/googlesamples/androidtv-Leanback/blob/master/app/src/main/java/com/example/android/tvleanback/ui/SettingsFragment.java

[videoplayerglue]: https://github.com/googlesamples/androidtv-Leanback/blob/master/app/src/main/java/com/example/android/tvleanback/player/VideoPlayerGlue.java

[playbackfragment]: https://github.com/googlesamples/androidtv-Leanback/blob/master/app/src/main/java/com/example/android/tvleanback/ui/PlaybackFragment.java

## Additonal Resouroces

- [Android TV Introduction](http://www.android.com/tv/)
- [Android TV Developer Documentation](http://developer.android.com/tv)
- [Android TV Apps in Google Play Store][store-apps]


## Screenshots

[![Screenshot](screenshots/atv-leanback-all.png)](https://raw.githubusercontent.com/googlesamples/androidtv-Leanback/master/screenshots/atv-leanback-all.png)

## Support

If you need additional help, our community might be able to help.

- Android TV Google+ Community: [https://g.co/androidtvdev](https://g.co/androidtvdev)
- Stack Overflow: [http://stackoverflow.com/questions/tagged/android-tv](http://stackoverflow.com/questions/tagged/android-tv)

## Dependencies

If you use Android Studio as recommended, the following dependencies will **automatically** be installed by Gradle.

- Android SDK v7 appcompat library
- Android SDK v17 leanback support library
- Android SDK v7 recyclerview library

## Contributing

We love contributions! :smile: Please follow the steps in the [CONTRIBUTING guide][contributing] to get started. If you found a bug, please file it [here][bugs].

## License

Licensed under the Apache 2.0 license. See the [LICENSE file][license] for details.

[store-apps]: https://play.google.com/store/apps/collection/promotion_3000e26_androidtv_apps_all
[studio]: https://developer.android.com/tools/studio/index.html
[getting-started]: https://developer.android.com/training/tv/start/start.html
[bugs]: https://github.com/googlesamples/androidtv-Leanback/issues/new
[contributing]: CONTRIBUTING.md
[license]: LICENSE
