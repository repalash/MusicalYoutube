# MusicalYoutube
A Youtube PIP player for Android.

An innovative Android app that plays Youtube videos in a small always on top player, which can be dragged around on the screen. 
The app also supports a real-time playlist queue control, in which videos/music can be added from youtube search.

If you find a bug or would like to suggest some feature, create an issue or send a pull request.

This app was made as a fun project over a weekend.

## How it works
The app starts a foreground service, where it plays the video in a webview with the youtube embedded player, this window can be dragged, dropped on the screen and can run without an Activity.

To compile the code, put your youtube API key in YoutubeConnector.java. The API key is needed for search functionality, not for playing the videos.

The open source version is slightly different from the one on play store as it allows some functionality, that is restricted for play store.

## Playstore link
https://play.google.com/store/apps/details?id=com.palashbansal.musicalyoutube


## Demo Video
[![Musical youtube](https://img.youtube.com/vi/gyhTcseulTs/0.jpg)](https://www.youtube.com/watch?v=gyhTcseulTs)
