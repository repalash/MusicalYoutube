package com.palashbansal.musicalyoutube;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.widget.Toast;

import org.apache.pig.impl.util.ObjectSerializer;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Palash on 24-Nov-16.
 * The singleton object of this class is responsible for controlling the playback of music.
 * This behaves like a bridge between the user and music playback
 * All user actions have to go through this class's object
 * This class also stores the current state of the playback
 */

public class PlaybackController {
	public static final String CURRENT_POSITION_STRING = "CURRENT_POSITION";
	public static final int REPEAT = 0;
	public static final int REPEAT_ONE = 1;
	public static final int NO_REPEAT = 2;
	public static final int NO_POSITION = -1;
	public static final String CURRENT_QUEUE_NAME = "CURRENT_QUEUE";
	private static PlaybackController controller;
	private Playlist playlist = null;
	private int currentPosition = NO_POSITION;
	private int mode = NO_REPEAT;
	private PlaybackQueueAdapter playbackQueueAdapter;

	private Handler playbackRetryHandler = new Handler(); //To try playback, whenever the player is ready.

	private Context context = null;

	private Runnable onCurrentVideoChanged;
	private SharedPreferences sharedPreferences;

	private PlaybackController(SharedPreferences sharedPrefs) {
		sharedPreferences = sharedPrefs;
		currentPosition = sharedPrefs.getInt(CURRENT_POSITION_STRING, 0);
		try {
			ArrayList<VideoItem> videos = (ArrayList<VideoItem>) ObjectSerializer.deserialize(sharedPrefs.getString(CURRENT_QUEUE_NAME, null));
			if (videos != null)
				playlist = new Playlist(videos, CURRENT_QUEUE_NAME);
			else
				playlist = new Playlist(new ArrayList<VideoItem>(), CURRENT_QUEUE_NAME);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized static PlaybackController getInstance(@Nullable Context context) {
		if (controller == null && context != null) {
			controller = new PlaybackController(context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE));
		}
		if (context != null) {
			if (!YoutubePlayerService.checkServiceAndStart(context)) {
				Toast.makeText(context, "Starting player, please wait.", Toast.LENGTH_SHORT).show();
			}
			controller.context = context;
		}
		return controller;
	}

	public void notifyVideoEnd(boolean fromUser) {
		if (currentPosition == NO_POSITION || playlist == null || playlist.getVideos() == null)
			return;
		if (mode != REPEAT_ONE) {
			currentPosition++;
			if (playbackQueueAdapter != null) {
				playbackQueueAdapter.notifyItemChanged(currentPosition - 1);
			}
		}
		if (currentPosition >= playlist.getVideos().size()) {
			if (mode == NO_REPEAT && !fromUser) {
				currentPosition--;
				return;
			}
			currentPosition = 0;
		}
		if (playbackQueueAdapter != null) {
			playbackQueueAdapter.notifyItemChanged(currentPosition);
		}
		callVideoChanged();
	}

	public void playPrevious() {
		if (currentPosition == NO_POSITION || playlist == null || playlist.getVideos() == null)
			return;
		if (currentPosition > 0) {
			currentPosition--;
			if (playbackQueueAdapter != null) {
				playbackQueueAdapter.notifyItemChanged(currentPosition + 1);
			}
		}
		if (currentPosition >= playlist.getVideos().size() || currentPosition < 0) {
			currentPosition = 0;
		}
		if (playbackQueueAdapter != null) {
			playbackQueueAdapter.notifyItemChanged(currentPosition);
		}
		callVideoChanged();
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public void playAtPosition(int position) {
		if (playlist == null || playlist.getVideos() == null) return;
		int last = currentPosition;
		currentPosition = position;
		if (playbackQueueAdapter != null) {
			playbackQueueAdapter.notifyItemChanged(currentPosition);
			playbackQueueAdapter.notifyItemChanged(last);
		}
		if (currentPosition >= playlist.getVideos().size() || currentPosition < 0) {
			currentPosition = 0;
		}
		callVideoChanged();
	}

	public void notifyVideoEnd() {
		notifyVideoEnd(false);
	}

	public VideoItem getCurrent() {
		if (currentPosition == NO_POSITION || playlist == null || playlist.getVideos() == null || currentPosition >= playlist.getVideos().size()) {
			currentPosition = NO_POSITION;
			return null;
		}
		return playlist.getVideos().get(currentPosition);
	}

	public void playNext(VideoItem video) {
		if (currentPosition == NO_POSITION || playlist == null) {
			playNow(video);
		} else {
			playlist.getVideos().add(currentPosition + 1, video);
			playlist.updateInDatabase();
			if (playbackQueueAdapter != null) {
				playbackQueueAdapter.notifyItemInserted(currentPosition + 1);
			}
		}
	}

	public void playNow(VideoItem video) {
		if (playlist == null) {
			playlist = new Playlist(new ArrayList<VideoItem>(), CURRENT_QUEUE_NAME);
			currentPosition = 0;
		}
		playlist.setName(CURRENT_QUEUE_NAME);
		if (currentPosition == NO_POSITION || currentPosition >= playlist.getVideos().size())
			currentPosition = 0;
		playlist.getVideos().add(currentPosition, video);
		playlist.updateInDatabase();
		callVideoChanged();
		if (playbackQueueAdapter != null) {
			playbackQueueAdapter.notifyItemInserted(currentPosition);
		}
	}

	public boolean saveCurrentAsPlaylist(String name) {
		if (!playlist.getName().equals(CURRENT_QUEUE_NAME))
			return false;
		playlist.setName(name);
		playlist.saveAsNew(currentPosition, sharedPreferences);
		return true;
	}

	public void addToPlaylist(VideoItem video, String playlistName) {
		if (playlistName.equals(playlist.getName())) {
			playlist.getVideos().add(video);
			if (playbackQueueAdapter != null) {
				playbackQueueAdapter.notifyItemInserted(playlist.getVideos().size() - 1);
			}
		}
		Playlist.insertIntoPlaylist(video, playlistName);
	}

	public Playlist getPlaylist() {
		return playlist;
	}

	public void setPlaylist(Playlist playlist) {
		this.playlist = playlist;
	}

	public Runnable getOnCurrentVideoChanged() {
		return onCurrentVideoChanged;
	}

	public void setOnCurrentVideoChanged(Runnable onCurrentVideoChanged) {
		this.onCurrentVideoChanged = onCurrentVideoChanged;
	}

	public int getCurrentPosition() {
		return currentPosition;
	}

	public int getMode() {
		return mode;
	}

	public int setNextMode() {
		if (mode == NO_REPEAT) mode = REPEAT_ONE;
		else if (mode == REPEAT_ONE) mode = REPEAT;
		else if (mode == REPEAT) mode = NO_REPEAT;
		return mode;
	}

	public void notifyServiceExiting() {
		onCurrentVideoChanged = null;
		saveCurrent();
	}

	public void saveCurrent() {
		saveCurrentAsPlaylist(CURRENT_QUEUE_NAME);
	}

	public boolean callVideoChanged() {
		playbackRetryHandler.removeCallbacksAndMessages(null);
		if (YoutubePlayerService.isPlayerReady) {
			if (onCurrentVideoChanged != null)
				onCurrentVideoChanged.run();
			return true;
		} else {
			playbackRetryHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					if (!callVideoChanged())
						playbackRetryHandler.postDelayed(this, 400);
				}
			}, 400);
			return false;
		}

	}

	public void setPlaybackQueueAdapter(PlaybackQueueAdapter playbackQueueAdapter) {
		this.playbackQueueAdapter = playbackQueueAdapter;
	}

	public void removeItem(int position) {
		if (position < 0 || position >= playlist.getVideos().size()) return;
		playlist.getVideos().remove(position);
		if (position < currentPosition) {
			currentPosition--;
		} else if (position == currentPosition) {
			notifyVideoEnd(true);
		}
	}

	public void moveItem(int fromPosition, int toPosition) {
		VideoItem v = playlist.getVideos().get(fromPosition);
		playlist.getVideos().remove(fromPosition);
		playlist.getVideos().add(toPosition, v);
		if (currentPosition == fromPosition) currentPosition = toPosition;
		else if ((currentPosition <= toPosition && currentPosition > fromPosition)) {
			currentPosition--;
		} else if (currentPosition >= toPosition && currentPosition < fromPosition) {
			currentPosition++;
		}
	}

	public void shufflePlaylist(boolean notifyChange) {
		Collections.shuffle(playlist.getVideos());
		playAtPosition(0);
		if (playbackQueueAdapter != null && notifyChange)
			playbackQueueAdapter.notifyDataSetChanged();
	}
}
