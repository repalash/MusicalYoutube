package com.palashbansal.musicalyoutube;

import android.content.Context;
import android.util.Log;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import java.io.IOException;
import java.util.List;


/**
 * Created by Palash on 22-Oct-16.
 * This provides all the API functions and callbacks needed to connect with youtube and fetch data, like search, top songs, etc
 */

public class YoutubeConnector {
	private YouTube youtube;
	private YouTube.Search.List query;

	public YoutubeConnector(Context context) {
		youtube = new YouTube.Builder(new NetHttpTransport(),
				new JacksonFactory(), new HttpRequestInitializer() {

			@Override
			public void initialize(HttpRequest hr) throws IOException {
			}
		}).setApplicationName(context.getString(R.string.app_name)).build();

		try {
			query = youtube.search().list("id,snippet");
			query.setKey(APIKeys.YOUTUBE_KEY); //TODO: Create a class in the same package with your Youtube API key as a static string.
			query.setType("video");
			query.setMaxResults(50L);
			query.setFields("items(id/videoId,snippet/title,snippet/description,snippet/thumbnails/default/url,snippet/channelTitle)");
		} catch (IOException e) {
			Log.d("YoutubeConnector", "Could not initialize: " + e);
		}
	}

	public void search(String keywords, List<VideoItem> items) throws IOException {
		query.setQ(keywords);
		try {
			SearchListResponse response = query.execute();
			List<SearchResult> results = response.getItems();
			items.clear();
			for (SearchResult result : results) {
				VideoItem item = new VideoItem();
				item.setTitle(result.getSnippet().getTitle());
				item.setChannelTitle(result.getSnippet().getChannelTitle());
				item.setDescription(result.getSnippet().getDescription());
				item.setThumbnailURL(result.getSnippet().getThumbnails().getDefault().getUrl());
				item.setId(result.getId().getVideoId());
				items.add(item);
			}
		} catch (IOException e) {
			Log.d("YoutubeConnector", "Could not search: " + e);
			throw e;
		}
	}

	public void autocomplete(String keywords, List<String> items) {
		//TODO: implement autocomplete: http://suggestqueries.google.com/complete/search?client=youtube&ds=yt&q=Query
		throw new UnsupportedOperationException("Not implemented");
	}

}
