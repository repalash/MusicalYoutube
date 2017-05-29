package com.palashbansal.musicalyoutube;

import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

/**
 * Created by Palash on 27-Nov-16.
 * This is the adapter for the recyclerview for the queue playback/playlist
 * It supports item drag and swipe to remove
 * It loads data directly from the PlaybackController and notifies it of any change
 */

public class PlaybackQueueAdapter extends RecyclerView.Adapter<PlaybackQueueAdapter.ViewHolder> {

	public PlaybackQueueAdapter() {
	}

	@Override
	public PlaybackQueueAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
															  int viewType) {
		View v = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.video_item, parent, false);
		ViewHolder vh = new ViewHolder(v);
		return vh;
	}

	@Override
	public void onBindViewHolder(final ViewHolder holder, final int position) {
		// - get element from your dataset at this position
		// - replace the contents of the view with that element
		final PlaybackController controller = PlaybackController.getInstance(holder.title.getContext());
		final VideoItem video = controller.getPlaylist().getVideos().get(position);
		Picasso.with(holder.thumbnail.getContext()).load(video.getThumbnailURL()).into(holder.thumbnail, new Callback() {
			@Override
			public void onSuccess() {
//				holder.thumbnail.setVisibility(View.VISIBLE);
			}

			@Override
			public void onError() {
//				holder.thumbnail.setImageDrawable(ResourcesCompat.getDrawable(holder.thumbnail.getResources(), R.drawable.music_video, null));
//				holder.thumbnail.setVisibility(View.VISIBLE);
			}
		});
		holder.title.setText(video.getTitle());
		holder.channelName.setText(video.getChannelTitle());
		holder.optionsButton.setVisibility(View.INVISIBLE);
		if (controller.getCurrentPosition() == position) {
			holder.container.setBackgroundColor(ResourcesCompat.getColor(holder.title.getContext().getResources(), R.color.colorPrimaryDark, null));
		} else {
			holder.container.setBackgroundColor(ResourcesCompat.getColor(holder.title.getContext().getResources(), R.color.colorPrimary, null));
		}
		holder.container.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				controller.playAtPosition(holder.getAdapterPosition());
			}
		});
	}

	public void onItemDismiss(int position) {
		PlaybackController controller = PlaybackController.getInstance(null);
		if (controller == null) return;
		controller.removeItem(position);
		notifyItemRemoved(position);
		Log.d("Adapter", "Remove " + position);
	}

	public boolean onItemMove(int fromPosition, int toPosition) {
		PlaybackController controller = PlaybackController.getInstance(null);
		if (controller == null) return false;
		controller.moveItem(fromPosition, toPosition);
		notifyItemMoved(fromPosition, toPosition);
		return true;
	}

	@Override
	public int getItemCount() {
		PlaybackController controller = PlaybackController.getInstance(null);
		if (controller == null || controller.getPlaylist() == null) return 0;
		return controller.getPlaylist().getVideos().size();
	}

	// Provide a reference to the views for each data item
	// Complex data items may need more than one view per item, and
	// you provide access to all the views for a data item in a view holder
	public static class ViewHolder extends RecyclerView.ViewHolder {
		// each data item is just a string in this case
		public ImageView thumbnail;
		public TextView title;
		public TextView channelName;
		public ImageView optionsButton;
		public FrameLayout container;

		//		public ProgressBar progressBar;
		public ViewHolder(View v) {
			super(v);
			thumbnail = (ImageView) v.findViewById(R.id.video_thumbnail);
			title = (TextView) v.findViewById(R.id.video_title);
			channelName = (TextView) v.findViewById(R.id.video_channel);
			optionsButton = (ImageView) v.findViewById(R.id.video_options);
			container = (FrameLayout) v.findViewById(R.id.main_video_container);
//			progressBar = (ProgressBar) v.findViewById(R.id.image_progress);
		}
	}
}