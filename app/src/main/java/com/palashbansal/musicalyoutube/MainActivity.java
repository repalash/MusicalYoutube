package com.palashbansal.musicalyoutube;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/**
 * Created by Palash on 24-Nov-16.
 * This is the main activity that the user is presented when the app launches
 * This attempts to start the service with itself and loads the last video that was played
 * This activity holds a RecyclerView which contains the current queue that is been played
 * The items in the RecyclerView can be order and removed with swipe
 */

public class MainActivity extends AppCompatActivity {
	MediaPlayer player;
	private RecyclerView recyclerView;
	private PlaybackQueueAdapter adapter;
	private PlaybackController controller;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(MainActivity.this, YoutubeSearchActivity.class);
				startActivity(intent);
			}
		});

		recyclerView = (RecyclerView) findViewById(R.id.queue_recycler_view);
		recyclerView.setHasFixedSize(true);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		adapter = new PlaybackQueueAdapter();
		recyclerView.setAdapter(adapter);

		ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
				new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
						ItemTouchHelper.START | ItemTouchHelper.END) {

					@Override
					public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
						return adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
					}

					@Override
					public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
						adapter.onItemDismiss(viewHolder.getAdapterPosition());
					}
				});

		itemTouchHelper.attachToRecyclerView(recyclerView);

		controller = PlaybackController.getInstance(this);
		controller.setPlaybackQueueAdapter(adapter);
	}

	@Override
	protected void onStart() {
		super.onStart();
		adapter.notifyDataSetChanged();
		YoutubePlayerService.checkServiceAndStart(this);
	}

	protected void onStop() {
		super.onStop();
		controller.saveCurrent();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		} else if (id == R.id.action_shuffle) {
			controller.shufflePlaylist(false);
			recyclerView.animate().alpha(0f).setDuration(600).start(); //Animate the visibility so that the change is not so sudden
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					adapter.notifyDataSetChanged();
					recyclerView.animate().alpha(1f).setDuration(600).start();
				}
			}, 600);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		controller.setPlaybackQueueAdapter(null);
		super.onDestroy();
	}
}
