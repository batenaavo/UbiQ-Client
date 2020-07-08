package com.example.ubiq;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//Fragmento que mostra as top musicas e os albuns de um artista.

public class ArtistFragment extends Fragment {

    private Artist artist;
    private ImageView artistImageView;
    private TextView artistNameView;
    private ListView albumListView;
    private ListView topTracksListView;
    String accessToken;

    public ArtistFragment(Artist artist, String accessToken){
        this.artist = artist;
        this.accessToken = accessToken;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_artist, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        this.albumListView = (ListView) getView().findViewById(R.id.album_list_view);
        this.topTracksListView = (ListView) getView().findViewById(R.id.track_list_view);
        this.artistImageView = getView().findViewById(R.id.artist_image);
        this.artistNameView =  getView().findViewById(R.id.artist_name_view);
        Picasso.get().load(this.artist.getImg()).into(this.artistImageView);
        this.artistNameView.setText(artist.getName());

        Toolbar toolbar = ((MainActivity) getActivity()).getToolbar();
        getActivity().setTitle(artist.getName());
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).startSearchFragment();
            }
        });

        TrackListAdapter tracksAdapter = new TrackListAdapter((getActivity().getApplicationContext()), artist.getTopTracks());
        topTracksListView.setAdapter(tracksAdapter);

        AlbumListAdapter albumsAdapter = new AlbumListAdapter((getActivity().getApplicationContext()), artist.getAlbums());
        albumListView.setAdapter(albumsAdapter);

        ListUtils.setDynamicHeight(albumListView);
        ListUtils.setDynamicHeight(topTracksListView);
    }

    //Método que inicia o Fragment de um álbum depois de obter da API do spotify
    //a informação necessária sobre o album
    public void getAlbumInfoAndStartFragment(MusicAlbum album, String img){
        String url = "https://api.spotify.com/v1/albums/" + album.getSpotifyId() + "/tracks";
        RequestQueue queue = Volley.newRequestQueue(getActivity());

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println(response);
                        album.setTracks(response, img);
                        System.out.println(album.getTracks());
                        ((MainActivity) getActivity()).startAlbumFragment(album);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String s = getString(R.string.unknown_err);
                if(error instanceof NoConnectionError){
                    s = getString(R.string.no_connection_err);
                }
                else if (error instanceof TimeoutError) {
                    getAlbumInfoAndStartFragment(album,img);
                }
                else if (error instanceof AuthFailureError) {
                    s = getString(R.string.auth_failure_err);
                } else if (error instanceof ServerError) {
                    s = new ServerErrorHandler().getErrorString(error);
                }
                System.out.println(error.toString());
                if(!(error instanceof TimeoutError))
                    Toast.makeText(getActivity(), s, Toast.LENGTH_SHORT).show();
            }
        })
        {
            @Override
            public Map getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                headers.put("Authorization", "Bearer " + accessToken);
                return headers;
            }
        };
        queue.add(stringRequest);
    }

    //Adaptador para criar uma ListView com os top tracks de um artista.
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

    //Adaptador para criar uma ListView com a lista de albuns de um artista.
    class AlbumListAdapter extends ArrayAdapter<MusicAlbum> {

        Context context;
        ArrayList<MusicAlbum> albums;
        AlbumListAdapter(Context c, ArrayList<MusicAlbum> albums){
            super(c, R.layout.search_row, albums);
            this.context = c;
            this.albums = albums;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
            LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = layoutInflater.inflate(R.layout.search_row, parent, false);
            ImageView image = row.findViewById(R.id.image);
            TextView mainName = row.findViewById(R.id.main_name_view);
            TextView secondaryName = row.findViewById(R.id.secondary_name_view);
            mainName.setSelected(true);
            secondaryName.setSelected(true);

            MusicAlbum album = albums.get(position);
            Picasso.get().load(album.getImg()).into(image);
            mainName.setText(album.getName());
            secondaryName.setText(String.valueOf(album.getYear()));

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getAlbumInfoAndStartFragment(albums.get(position), artist.getImg());
                }
            });

            return row;
        }
    }

    public static class ListUtils {
        public static void setDynamicHeight(ListView mListView) {
            ListAdapter mListAdapter = mListView.getAdapter();
            if (mListAdapter == null) {
                // when adapter is null
                return;
            }
            int height = 0;
            int desiredWidth = MeasureSpec.makeMeasureSpec(mListView.getWidth(), View.MeasureSpec.UNSPECIFIED);
            for (int i = 0; i < mListAdapter.getCount(); i++) {
                View listItem = mListAdapter.getView(i, null, mListView);
                listItem.measure(desiredWidth, MeasureSpec.UNSPECIFIED);
                height += listItem.getMeasuredHeight();
            }
            ViewGroup.LayoutParams params = mListView.getLayoutParams();
            params.height = height + (mListView.getDividerHeight() * (mListAdapter.getCount() - 1));
            mListView.setLayoutParams(params);
            mListView.requestLayout();
        }
    }

}
