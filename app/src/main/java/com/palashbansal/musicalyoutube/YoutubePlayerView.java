package com.palashbansal.musicalyoutube;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * Created by Palash on 23-Nov-16.
 * The main PlayerView that inherits a Webview, loads a custom specified HTML/CSS/JS code and provides callback and helper functions to control the video playback
 */

public class YoutubePlayerView extends WebView {

	public static final int BUFFERING = 3;
	public static final int CUED = 5;
	public static final int ENDED = 0;
	public static final int PAUSED = 2;
	public static final int PLAYING = 1;
	public static final int UNKNOWN = -2;
	public static final int UNSTARTED = -1;
	public boolean isDragging = false;
	private Runnable onPlayerReadyRunnable = null, onPlaybackStateChange = null;
	private NumberReceivedListener currentTimeListener, durationListener, VideoLoadedFractionListener;
	private int playbackState = UNKNOWN;

	public YoutubePlayerView(Context context) {
		super(context);
	}

	public YoutubePlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public YoutubePlayerView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void initialize() {
		String data = "<!DOCTYPE html>\n" +
				"<html>\n" +
				"  <head>\n" +
				"    <style type=\"text/css\">\n" +
				"      html {\n" +
				"        height: 100%;\n" +
				"      }\n" +
				"      body {\n" +
				"        min-height: 100%;\n" +
				"        margin: 0;\n" +
				"      }\n" +
				"      iframe {\n" +
				"        position: absolute;\n" +
				"        border: none;\n" +
				"        height: 100%;\n" +
				"        width: 100%;\n" +
				"        top: 0;\n" +
				"        left: 0;\n" +
				"        bottom: 0;\n" +
				"        right: 0;\n" +
				"      }\n" +
				"      #overlay { position: absolute; z-index: 3; opacity: 0.5; filter: alpha(opacity = 50); top: 0; bottom: 0; left: 0; right: 0; width: 100%; height: 100%; background-color: Black; color: White; display: none;}\n" +
				"    </style>\n" +
				"  </head>\n" +
				"  <body>\n" +
				"    <div id=\"player\"></div>\n" +
				"    <script>\n" +
				"      var tag = document.createElement('script');\n" +
				"      tag.src = \"https://www.youtube.com/iframe_api\";\n" +
				"      var firstScriptTag = document.getElementsByTagName('script')[0];\n" +
				"      firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);\n" +
				"      var player;\n" +
				"      function onYouTubeIframeAPIReady() {\n" +
				"        player = new YT.Player('player', {\n" +
				"          playerVars: {'controls': 0, 'iv_load_policy': 3 },    \n" +
				"          events: {\n" +
				"            'onReady': onPlayerReady,\n" +
				"            'onStateChange': onPlayerStateChange\n" +
				"          }\n" +
				"        });\n" +
				"      }\n" +
				"      function onPlayerReady(event) {\n" +
				"        setTimeout(function(){\n" +
				"          Android.playerReady();\n" +
				"          player.setVolume(100)\n" +
				"        }, 200);\n" +
				"      }\n" +
				"      var done = false;\n" +
				"      function onPlayerStateChange(event) {\n" +
				"        Android.playerStateChange(event.data);\n" +
				"      }\n" +
				"      function stopVideo() {\n" +
				"        player.stopVideo();\n" +
				"      }\n" +
				"      function showOverlay() {\n" +
				"        document.getElementById('overlay').style.display = 'block';\n" +
				"      }\n" +
				"      function hideOverlay() {\n" +
				"        document.getElementById('overlay').style.display = 'none';\n" +
				"      }\n" +
				"      function playFullscreen (){\n" +
				"        iframe = document.getElementById('player');\n" +
				"        var requestFullScreen = iframe.webkitRequestFullScreen;\n" +
				"        Android.log(\"Playing fullscreen\");\n" +
				"        if (requestFullScreen) {\n" +
				"          requestFullScreen.bind(iframe)();\n" +
				"        }\n" +
				"      }\n" +
				"      function getCurrentTime(){\n" +
				"        Android.notifyCurrentTime(player.getCurrentTime());\n" +
				"      }\n" +
				"      function getDuration(){\n" +
				"        Android.notifyDuration(player.getDuration());\n" +
				"      }\n" +
				"      function getVideoLoadedFraction(){\n" +
				"        Android.notifyVideoLoadedFraction(player.getVideoLoadedFraction());\n" +
				"      }\n" +
				"      window.setInterval(getCurrentTime, 1000);\n" +
				"      window.setInterval(getDuration, 1000);\n" + //TODO: Test performance of this vs the function calling approach.
				"      window.setInterval(getVideoLoadedFraction, 1000);\n" +
				"    </script>\n" +
				"    <div id=\"overlay\"></div>\n" +
				"  </body>\n" +
				"</html>\n";

		setTag("YTPlayer");
		setWebChromeClient(new WebChromeClient());
		clearCache(true);
		clearHistory();
//		if (Build.VERSION.SDK_INT >= 19) {
//			// chromium, enable hardware acceleration
//			setLayerType(View.LAYER_TYPE_HARDWARE, null);
//		} else {
//			// older android version, disable hardware acceleration
//			setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//		}
//		getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
		getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		getSettings().setJavaScriptEnabled(true);
		getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
		addJavascriptInterface(this, "Android");
		loadDataWithBaseURL("http://www.youtube.com", data, "text/html", "UTF-8", "http://www.youtube.com");
	}

	public void pause() {
		loadUrl("javascript:player.pauseVideo();");
	}

	public void play() {
		loadUrl("javascript:player.playVideo();");
	}

	@JavascriptInterface
	public void playerReady() {
		if (onPlayerReadyRunnable != null) {
			post(onPlayerReadyRunnable);
		}
		Log.d("Player", "Ready");
	}

	@JavascriptInterface
	public void log(String s) {
		Log.d("Javascript", s);
	}

	@JavascriptInterface
	public void notifyCurrentTime(int secs) {
		if (currentTimeListener != null)
			currentTimeListener.onReceive(secs);
	}

	@JavascriptInterface
	public void notifyDuration(int secs) {
		if (durationListener != null)
			durationListener.onReceive(secs);
	}

	@JavascriptInterface
	public void notifyVideoLoadedFraction(float fraction) {
		if (VideoLoadedFractionListener != null)
			VideoLoadedFractionListener.onReceive(fraction);
	}


	@JavascriptInterface
	public void playerStateChange(int state) {
		playbackState = state;
		if (onPlaybackStateChange != null) post(onPlaybackStateChange);
		switch (state) {
			case BUFFERING:
				log("BUFFERING");
				break;
			case CUED:
				log("CUED");
				break;
			case ENDED:
				log("ENDED");
				break;
			case PAUSED:
				log("PAUSED");
				break;
			case PLAYING:
				log("PLAYING");
				break;
			case UNSTARTED:
				log("UNSTARTED");
				break;
		}
	}


	public void getCurrentTime() {
		loadUrl("javascript:getCurrentTime()");
	}

	public void showOverlay() {
		isDragging = true;
		loadUrl("javascript:showOverlay()");
	}

	public void hideOverlay() {
		isDragging = false;
		loadUrl("javascript:hideOverlay()");
	}

	public void getVideoLoadedFraction() {
		loadUrl("javascript:getVideoLoadedFraction()");
	}

	public void getDuration() {
		loadUrl("javascript:getDuration()");
	}

	public void setVolume(float volume) {
		loadUrl("javascript:player.setVolume(" + volume + ")");
	}

	public void mute() {
		loadUrl("javascript:player.mute()");
	}

	public void unMute() {
		loadUrl("javascript:player.unMute()");
	}

	public void seekTo(float seconds, boolean allowSeekAhead) {
		loadUrl("javascript:player.seekTo(" + seconds + "," + allowSeekAhead + ")");
	}

	public void loadVideoById(String videoId, float startSeconds, String suggestedQuality) {
		loadUrl("javascript:player.loadVideoById(\"" + videoId + "\"," + startSeconds + ",\"" + suggestedQuality + "\")");
	}

	public void loadVideoById(String videoId) {
		loadVideoById(videoId, 0, "default");
	}

	public void setOnPlayerReadyRunnable(Runnable onPlayerReadyRunnable) {
		this.onPlayerReadyRunnable = onPlayerReadyRunnable;
	}

	public void setOnPlaybackStateChange(Runnable onPlaybackStateChange) {
		this.onPlaybackStateChange = onPlaybackStateChange;
	}

	public void setCurrentTimeListener(NumberReceivedListener currentTimeListener) {
		this.currentTimeListener = currentTimeListener;
	}

	public void setDurationListener(NumberReceivedListener durationListener) {
		this.durationListener = durationListener;
	}

	public void setVideoLoadedFractionListener(NumberReceivedListener videoLoadedFractionListener) {
		VideoLoadedFractionListener = videoLoadedFractionListener;
	}

	public void removePage() {
		loadUrl("about:blank");
	}

	public int getPlaybackState() {
		return playbackState;
	}

	public interface NumberReceivedListener {
		/**
		 * Called when a number has been received.
		 *
		 * @param n The number received.
		 */
		void onReceive(float n);
	}
}
