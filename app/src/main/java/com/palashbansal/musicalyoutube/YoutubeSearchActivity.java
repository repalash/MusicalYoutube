package com.palashbansal.musicalyoutube;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Palash on 22-Oct-16.
 * The search activity, where videos can be searched from the action bar and results are displayed in a ListView.
 */

public class YoutubeSearchActivity extends AppCompatActivity {
	public static final String SEARCH_QUERY = "SEARCH_QUERY";
	public static YoutubeSearchActivity object;
	private ListView videosFound;
	private List<VideoItem> searchResults = new ArrayList<>();
	private Handler handler = new Handler();
	private ArrayAdapter<VideoItem> adapter;
	/**
	 * ATTENTION: This was auto-generated to implement the App Indexing API.
	 * See https://g.co/AppIndexing/AndroidStudio for more information.
	 */
	private GoogleApiClient client;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_youtube_search);

		videosFound = (ListView) findViewById(R.id.videos_found);
		setVideoClickListener();

		object = this;

		YoutubePlayerService.checkServiceAndStart(this);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		adapter = new ArrayAdapter<VideoItem>(getApplicationContext(), R.layout.video_item, searchResults) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				if (convertView == null) {
					convertView = getLayoutInflater().inflate(R.layout.video_item, parent, false);
				}
				ImageView thumbnail = (ImageView) convertView.findViewById(R.id.video_thumbnail);
				TextView title = (TextView) convertView.findViewById(R.id.video_title);
				TextView channelTitle = (TextView) convertView.findViewById(R.id.video_channel);
				ImageView moreButton = (ImageView) convertView.findViewById(R.id.video_options);

				final VideoItem searchResult = searchResults.get(position);

				Picasso.with(getApplicationContext()).load(searchResult.getThumbnailURL()).into(thumbnail);
				title.setText(searchResult.getTitle());
				channelTitle.setText(searchResult.getChannelTitle());

				final PopupMenu pum = new PopupMenu(YoutubeSearchActivity.this, moreButton);
				pum.inflate(R.menu.video_more_popup);
				pum.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						switch (item.getItemId()) {
							case R.id.play_next:
								PlaybackController.getInstance(YoutubeSearchActivity.this).playNext(searchResult);
								break;
							case R.id.add_playlist:
								// TODO: add to playlist
								break;
							case R.id.add_queue:
								PlaybackController.getInstance(YoutubeSearchActivity.this).addToPlaylist(searchResult, PlaybackController.CURRENT_QUEUE_NAME);
								break;
						}
						return false;
					}
				});
				moreButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						pum.show();
					}
				});

				return convertView;
			}
		};
		videosFound.setAdapter(adapter);

		String searchQuery = getIntent().getStringExtra(SEARCH_QUERY);
		if (searchQuery == null) searchQuery = "Songs";
		searchOnYoutube(searchQuery);
		client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
	}

	private void setVideoClickListener() {
		videosFound.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
				PlaybackController.getInstance(YoutubeSearchActivity.this).playNow(searchResults.get(pos));
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_youtube_search, menu);
		MenuItem searchMenuItem = menu.findItem(R.id.action_search);
		android.support.v7.widget.SearchView searchView = (android.support.v7.widget.SearchView) searchMenuItem.getActionView();
		searchView.setOnQueryTextListener(new android.support.v7.widget.SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				searchOnYoutube(query);
				return false;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				//TODO: Show search suggestions.
				return false;
			}
		});
		return true;
	}

	private void updateVideosFound() {
		adapter.notifyDataSetChanged();
	}

	private void searchOnYoutube(final String keywords) {
		setTitle(keywords);
		searchResults.clear();
		updateVideosFound();
		new Thread() {
			public void run() {
				YoutubeConnector yc = new YoutubeConnector(YoutubeSearchActivity.this);
				try {
					yc.search(keywords, searchResults);
					handler.post(new Runnable() {
						public void run() {
							updateVideosFound();
						}
					});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		this.finish();
	}

	/**
	 * ATTENTION: This was auto-generated to implement the App Indexing API.
	 * See https://g.co/AppIndexing/AndroidStudio for more information.
	 */
	public Action getIndexApiAction() {
		Thing object = new Thing.Builder()
				.setName("YoutubeSearch Page")
				.setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
				.build();
		return new Action.Builder(Action.TYPE_VIEW)
				.setObject(object)
				.setActionStatus(Action.STATUS_TYPE_COMPLETED)
				.build();
	}

	@Override
	public void onStart() {
		super.onStart();

		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		client.connect();
		AppIndex.AppIndexApi.start(client, getIndexApiAction());
	}

	@Override
	public void onStop() {
		super.onStop();

		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		AppIndex.AppIndexApi.end(client, getIndexApiAction());
		client.disconnect();
	}
}
