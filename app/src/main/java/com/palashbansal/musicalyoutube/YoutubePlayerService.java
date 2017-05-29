package com.palashbansal.musicalyoutube;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import static com.palashbansal.musicalyoutube.PlaybackController.NO_REPEAT;
import static com.palashbansal.musicalyoutube.PlaybackController.REPEAT_ONE;
import static com.palashbansal.musicalyoutube.YoutubePlayerView.BUFFERING;
import static com.palashbansal.musicalyoutube.YoutubePlayerView.CUED;
import static com.palashbansal.musicalyoutube.YoutubePlayerView.ENDED;
import static com.palashbansal.musicalyoutube.YoutubePlayerView.GONE;
import static com.palashbansal.musicalyoutube.YoutubePlayerView.INVISIBLE;
import static com.palashbansal.musicalyoutube.YoutubePlayerView.OnClickListener;
import static com.palashbansal.musicalyoutube.YoutubePlayerView.PAUSED;
import static com.palashbansal.musicalyoutube.YoutubePlayerView.PLAYING;
import static com.palashbansal.musicalyoutube.YoutubePlayerView.UNSTARTED;
import static com.palashbansal.musicalyoutube.YoutubePlayerView.VISIBLE;

/**
 * Created by Palash on 23-Nov-16.
 * This is the main service that holds the window manager for the Youtube popup player.
 * This will always be running in the background when the video is playing
 * This service can only be stopped by itself, when the queue finishes or user presses the x button.
 */

public class YoutubePlayerService extends Service {

	private static final int FOREGROUND_NOTIFICATION_ID = 4822678;
	private static final int CONTROL_HIDE_TIMEOUT = 4000;
	public static boolean isRunning = false, isPlayerReady = false;
	private VideoItem currentVideo;
	private boolean isControlsVisible = false, isPlaylistVisible;
	private int currentSeconds = 0, videoDuration = 1;
	private Handler secondsHandler = new Handler();
	private float videoBuffered = 0;
	private boolean isUserChangingTouch = false;
	private WindowManager windowManager;
	private YoutubePlayerView playerView;
	private WindowManager.LayoutParams params;
	private FrameLayout container;
	private View controlContainer;
	private View playlistContainer;
	private View seekBarContainer;
	private SeekBar seekBar;
	private View playerContainer;
	private long lastTouchTime;
	private PlaybackController controller;
	private ImageView closeButton;
	private ImageView playButton;
	private ImageView prevButton;
	private ImageView nextButton;
	private ImageView openListButton;
	private ProgressBar bufferingIndicator;
	private boolean isPaused = true;
	private ImageView modeButton;
	private TextView durationTextView;
	private ImageView replayButton;

	public static float dipToPixels(Context context, float dipValue) {
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
	}

	/**
	 * Static helper method to start the service if stopped
	 */
	public static boolean checkServiceAndStart(Context context) {
		if (!isRunning) {
			Log.d("Player", "StartingService");
			context.startService(new Intent(context, YoutubePlayerService.class));
			return false;
		}
		return true;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		isRunning = true;
		Log.d("Service", "onCreate");

		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

		controller = PlaybackController.getInstance(this);

		container = new FrameLayout(this) {
			@Override
			public boolean onInterceptTouchEvent(MotionEvent ev) {
				lastTouchTime = System.currentTimeMillis();
				if (!isControlsVisible) {
					showControlContainer();
				}
				return super.onInterceptTouchEvent(ev);
			}
		};
		LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.popup_player_layout, container);

		playerView = (YoutubePlayerView) view.findViewById(R.id.player_view);
		controlContainer = view.findViewById(R.id.control_container);
		playlistContainer = view.findViewById(R.id.playlist_container);
		seekBarContainer = view.findViewById(R.id.slider_container);
		playerContainer = view.findViewById(R.id.player_card);
		seekBar = (SeekBar) view.findViewById(R.id.seekBar);
		closeButton = (ImageView) view.findViewById(R.id.close_button);
		playButton = (ImageView) view.findViewById(R.id.play_button);
		replayButton = (ImageView) view.findViewById(R.id.replay_button);
		prevButton = (ImageView) view.findViewById(R.id.previous_button);
		nextButton = (ImageView) view.findViewById(R.id.next_button);
		openListButton = (ImageView) view.findViewById(R.id.open_activity_button);
		modeButton = (ImageView) view.findViewById(R.id.current_mode_button);
		durationTextView = (TextView) view.findViewById(R.id.duration_text);
		bufferingIndicator = (ProgressBar) view.findViewById(R.id.buffer_loading_indicator);

		playerView.initialize();

		isPlayerReady = false;
		isControlsVisible = true;
		isPlaylistVisible = false;

		lastTouchTime = System.currentTimeMillis();

		controller.setOnCurrentVideoChanged(new Runnable() {
			@Override
			public void run() {
				if (!isControlsVisible)
					showControlContainer();
				if (controller.getCurrent() == null) {
					playerView.seekTo(0, true);
					playerView.pause();
					return;
				}
				if (currentVideo != null && controller.getCurrent().getId().equals(currentVideo.getId())) {
					playerView.seekTo(0, true);
					playerView.play();
					return;
				}
				currentVideo = controller.getCurrent();
				playerView.play();
				playerView.loadVideoById(currentVideo.getId());
				container.setVisibility(View.VISIBLE);
				NotificationCompat.Builder mBuilder =
						new NotificationCompat.Builder(YoutubePlayerService.this)
								.setSmallIcon(R.drawable.headset)
								.setContentTitle(currentVideo.getTitle())
								.setContentText(currentVideo.getChannelTitle());
				startForeground(FOREGROUND_NOTIFICATION_ID, mBuilder.build());
				Log.d("Service", "Video Changed");
			}
		});

		params = new WindowManager.LayoutParams(
				(int) (Resources.getSystem().getDisplayMetrics().widthPixels / 1.75),
				ViewGroup.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);
		params.gravity = Gravity.TOP | Gravity.START;
		params.x = 140;
		params.y = 900;

		setDragListeners();

		playerView.setCurrentTimeListener(new YoutubePlayerView.NumberReceivedListener() {
			@Override
			public void onReceive(float n) {
				currentSeconds = (int) n;
			}
		}); //TODO: Put this in player view itself
		playerView.setDurationListener(new YoutubePlayerView.NumberReceivedListener() {
			@Override
			public void onReceive(float n) {
				videoDuration = (int) n;
			}
		}); //TODO: Put this in player view itself
		playerView.setVideoLoadedFractionListener(new YoutubePlayerView.NumberReceivedListener() {
			@Override
			public void onReceive(float n) {
				videoBuffered = n;
			}
		}); //TODO: Put this in player view itself

		Runnable r = new Runnable() {
			@Override
			public void run() {
				isControlsVisible = seekBarContainer.getVisibility() == View.VISIBLE;
				if (!isUserChangingTouch && isControlsVisible) {
					updateSeekUI();
					//playerView.getCurrentTime();
					//playerView.getDuration(); //TODO: Test performance of this vs the auto js calling approach.
					//playerView.getVideoLoadedFraction();
				}
				if (System.currentTimeMillis() - lastTouchTime > CONTROL_HIDE_TIMEOUT) {
					hideControlContainer();
				}
				secondsHandler.postDelayed(this, 1000);
			}
		};
		secondsHandler.postDelayed(r, 200);

		windowManager.addView(container, params);
		container.setVisibility(View.GONE);

		setupPlayerView();

		setButtonListeners();

		Log.d("Player", "Created");
	}

	/**
	 * It initializes all the buttons and the seekbar, also holds the listeners
	 */
	private void setButtonListeners() {
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser && videoDuration != 0) {
					lastTouchTime = System.currentTimeMillis();
					currentSeconds = (int) ((progress / 100f) * videoDuration);
					if (isUserChangingTouch) {
						playerView.seekTo(currentSeconds, false);
					} else {
						playerView.seekTo(currentSeconds, true);
					}
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				isUserChangingTouch = true;
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				isUserChangingTouch = false;
				currentSeconds = (int) ((seekBar.getProgress() / 100f) * videoDuration);
				playerView.seekTo(currentSeconds, true);
			}
		});

		playButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isPaused) {
					playerView.play();
				} else {
					playerView.pause();
				}
			}
		});
		replayButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				playerView.seekTo(0, true);
				playerView.play();
			}
		});
		prevButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (currentSeconds > 5) controller.callVideoChanged();
				else controller.playPrevious();
			}
		});
		nextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				controller.notifyVideoEnd(true);
			}
		});
		closeButton.setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						isRunning = false;
						YoutubePlayerService.this.stopSelf();
					}
				}
		);
		openListButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(YoutubePlayerService.this, MainActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);
			}
		});
		modeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				changeModeButtonImage(controller.setNextMode());
			}
		});
		changeModeButtonImage(controller.getMode());
	}

	/**
	 * Sets up the player view
	 */
	public void setupPlayerView() {
		playerView.setOnPlayerReadyRunnable(new Runnable() {
			@Override
			public void run() {
				controller.callVideoChanged();
				playerView.pause();
				isPaused = true;
				bufferingIndicator.setVisibility(GONE);
				playButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.play, null));
				replayButton.setVisibility(VISIBLE);
				isPlayerReady = true;
			}
		});

		playerView.setOnPlaybackStateChange(new Runnable() {
			@Override
			public synchronized void run() {
				switch (playerView.getPlaybackState()) {
					case BUFFERING:
						if (!isPaused) bufferingIndicator.setVisibility(VISIBLE);
						break;
					case CUED:
						bufferingIndicator.setVisibility(VISIBLE);
						break;
					case ENDED:
						bufferingIndicator.setVisibility(GONE);
						playButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.play, null));
						replayButton.setVisibility(VISIBLE);
						controller.notifyVideoEnd();
						break;
					case PAUSED:
						bufferingIndicator.setVisibility(GONE);
						playButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.play, null));
						replayButton.setVisibility(INVISIBLE);
						isPaused = true;
						break;
					case PLAYING:
						bufferingIndicator.setVisibility(GONE);
						replayButton.setVisibility(INVISIBLE);
						playButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.pause, null));
						isPaused = false;
						break;
					case UNSTARTED:
						break;
				}

			}
		});
	}

	/**
	 * It is used to update the UI of mode button
	 */
	void changeModeButtonImage(int mode) {
		modeButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), mode == REPEAT_ONE ? R.drawable.repeat_one : R.drawable.repeat, null));
		if (mode == NO_REPEAT) modeButton.setImageAlpha(80);
		else modeButton.setImageAlpha(255);
	}

	/**
	 * It is used to update seek bar UI
	 */
	private void updateSeekUI() {
		Log.d("Service", "Updating SeekUI, " + videoDuration);
		if (isUserChangingTouch || videoDuration == 0) {
			seekBar.setProgress(0);
			seekBar.setSecondaryProgress(0);
			currentSeconds = 0;
			updateTextUI();
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			seekBar.setProgress(100 * currentSeconds / videoDuration, true);
		} else {
			seekBar.setProgress(100 * currentSeconds / videoDuration);
		}
		seekBar.setSecondaryProgress((int) (100 * videoBuffered));
		updateTextUI();
	}

	private void updateTextUI() {
		String text = currentSeconds / 60 + ":" + currentSeconds % 60;
		durationTextView.setText(text);
	}

	/**
	 * Animates to hide the Control container and the seekbar container
	 */
	private void hideControlContainer() {
		isControlsVisible = false;
		seekBarContainer.animate().translationY(-dipToPixels(YoutubePlayerService.this, 80)).setDuration(200).setListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				seekBarContainer.setVisibility(View.GONE);
				seekBarContainer.setTranslationY(0);
			}
		}).setStartDelay(80).start();
		controlContainer.animate().translationY(-dipToPixels(YoutubePlayerService.this, 120)).setDuration(300).setListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				controlContainer.setVisibility(View.GONE);
				controlContainer.setTranslationY(0);
			}
		}).start();
	}

	/**
	 * Animates to show the Control container and the seekbar container
	 */
	private void showControlContainer() {
		if (isControlsVisible || seekBarContainer.getVisibility() == View.VISIBLE) {
			isControlsVisible = true;
			return;
		}
		isControlsVisible = true;
		seekBarContainer.setTranslationY(-dipToPixels(YoutubePlayerService.this, 80));
		seekBarContainer.setVisibility(View.VISIBLE);
		seekBarContainer.animate().translationY(0).setDuration(200).setListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
			}
		}).start();
		controlContainer.setTranslationY(-dipToPixels(YoutubePlayerService.this, 140));
		controlContainer.setVisibility(View.VISIBLE);
		controlContainer.animate().translationY(0).setDuration(300).setListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
			}
		}).start();
	}

	/**
	 * To be used in the next version, will hide the popup playlist container
	 */
	private void hidePlaylistContainer() {
		isPlaylistVisible = false;
		playlistContainer.animate().alpha(0).setDuration(1000).setListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				playlistContainer.setVisibility(View.GONE);
				playlistContainer.setAlpha(0);
			}
		}).start();
	}

	/**
	 * To be used in the next version, will show the popup playlist container
	 */
	private void showPlaylistContainer() {
		isPlaylistVisible = true;
		playlistContainer.setVisibility(View.VISIBLE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (container != null) {
			playerView.removePage();
			windowManager.removeView(container);
		}
		if (secondsHandler != null)
			secondsHandler.removeCallbacksAndMessages(null);
		stopForeground(true);
		controller.notifyServiceExiting();
		isRunning = false;
		isPlayerReady = false;
		Log.d("Player", "Destroyed");
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("Player", "startCommand");

		if (intent == null) return super.onStartCommand(null, flags, startId);

		VideoItem video = (VideoItem) intent.getSerializableExtra("Video");
		if (video != null)
			controller.playNow(video);


		return START_STICKY;
	}

	/**
	 * Defines the logic of what happens when we drag the player on the screen
	 */
	private void setDragListeners() {
		playerView.setOnLongClickListener(
				new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						playerView.showOverlay();
						Log.d("LongClick", "click");
						return true;
					}
				}
		);

		playerView.setOnTouchListener(new View.OnTouchListener() {
			private int initialX;
			private int initialY;
			private float initialTouchX;
			private float initialTouchY;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						initialX = params.x;
						initialY = params.y;
						initialTouchX = event.getRawX();
						initialTouchY = event.getRawY();
						return false;
					case MotionEvent.ACTION_UP:
						playerView.hideOverlay();
						return false;
					case MotionEvent.ACTION_MOVE:
						if (!playerView.isDragging) {
							return false;
						}
						params.x = initialX
								+ (int) (event.getRawX() - initialTouchX);
						params.y = initialY
								+ (int) (event.getRawY() - initialTouchY);
						windowManager.updateViewLayout(container, params);
						return true;
				}
				return false;
			}
		});
	}

}
