package com.example.ubiq;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

//Fragment que mostra as músicas de um album com possibilidade de as adicionar à queue.

public class AlbumFragment extends Fragment{
    private MusicAlbum album;
    private ImageView albumImageView;
    private TextView albumNameView;
    private TextView artistNameView;
    private ListView trackListView;
    String accessToken;

    public AlbumFragment(MusicAlbum album){
        this.album = album;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        this.accessToken = ((MainActivity) getActivity()).getSpotifyAccessToken();
        this.trackListView = (ListView) getView().findViewById(R.id.track_list_view);
        this.albumImageView = getView().findViewById(R.id.album_image);
        this.albumNameView =  getView().findViewById(R.id.album_name_view);
        this.artistNameView =  getView().findViewById(R.id.artist_name_view);
        Picasso.get().load(this.album.getImg()).into(this.albumImageView);
        this.albumNameView.setText(album.getName());
        this.artistNameView.setText(album.getArtist());

        Toolbar toolbar = ((MainActivity) getActivity()).getToolbar();
        getActivity().setTitle(album.getName());
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).goToPreviousFragment();
            }
        });

        AlbumFragment.TrackListAdapter tracksAdapter = new AlbumFragment.TrackListAdapter((getActivity().getApplicationContext()),
                album.getTracks());
        trackListView.setAdapter(tracksAdapter);
    }


    class TrackListAdapter extends ArrayAdapter<Track> {

        Context context;
        ArrayList<Track> tracks;
        TrackListAdapter(Context c, ArrayList<Track> tracks){
            super(c, R.layout.search_row, tracks);
            this.context = c;
            this.tracks = tracks;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
            LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = layoutInflater.inflate(R.layout.search_row, parent, false);
            ImageView image = row.findViewById(R.id.image);
            TextView mainName = row.findViewById(R.id.main_name_view);
            TextView secondaryName = row.findViewById(R.id.secondary_name_view);
            ImageButton addButton = row.findViewById(R.id.add_button);
            mainName.setSelected(true);
            secondaryName.setSelected(true);

            Track track = tracks.get(position);
            image.setVisibility(View.GONE);
            secondaryName.setVisibility(View.GONE);
            addButton.setVisibility(View.VISIBLE);
            mainName.setText(track.getName());
            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((MainActivity)getActivity()).sendPostTrackRequest(track);
                }
            });
            return row;
        }
    }
}


