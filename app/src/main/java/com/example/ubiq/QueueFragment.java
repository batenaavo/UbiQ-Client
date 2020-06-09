package com.example.ubiq;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class QueueFragment extends Fragment {
    ListView queueListView;
    String apiToken;
    int queueId;
    private TextView userCountText;
    Boolean canceled;
    public ArrayList<Track> queueTracks;
    public int currentTrack;

    public QueueFragment(String apiToken, int queueId){
        this.apiToken = apiToken;
        this.queueId = queueId;
        this.queueTracks = new ArrayList<>();
        this.currentTrack = -1;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_queue, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        canceled = false;
        this.queueListView = (ListView) getView().findViewById(R.id.queueListView);
        this.userCountText = getView().findViewById(R.id.user_count_text);
        getActivity().setTitle(((MainActivity) getActivity()).getQueueName());
        ((MainActivity) getActivity()).getToolbar().setNavigationIcon(null);
        resetAdapter();
    }


    public void resetAdapter() {
        userCountText.setText(((MainActivity) getActivity()).getConnectedUsersCount() + " connected users");
        if(queueTracks.size() > 0){
            getView().findViewById(R.id.empty_text).setVisibility(View.GONE);
        }
        else{
            getView().findViewById(R.id.empty_text).setVisibility(View.VISIBLE);
            //TODO remover playback com signalR
            ((MainActivity) getActivity()).setPlaybackVisible(false);
        }
        QueueAdapter adapter = new QueueAdapter(getActivity(), queueTracks);
        queueListView.setAdapter(adapter);
    }


    public void cancelRequests(){
        canceled = true;
    }


    public void sendDeleteTrackRequest(int index){
        System.out.println(queueId);
        System.out.println(index);
        System.out.println(queueTracks.get(index).getName());
        System.out.println(queueTracks.get(index).getSpotifyId());
        int i2 = index + 1;
        String url = "https://ubiq.azurewebsites.net/api/Sala/Musicas/Remover?SalaId="
                + this.queueId + "&URI=" + queueTracks.get(index).getSpotifyId() + "&posicao=" + i2;

        RequestQueue queue = Volley.newRequestQueue(getActivity());

        StringRequest stringRequest = new StringRequest(Request.Method.DELETE, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        ((MainActivity) getActivity()).removeTrackFromQueue(index);
                        resetAdapter();
                        Toast.makeText(getActivity(), "Track removed with success", Toast.LENGTH_SHORT).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error.toString());
            }
        })
        {
            @Override
            public Map getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                headers.put("Authorization", "Bearer " + apiToken);
                return headers;
            }
        };
        queue.add(stringRequest);
    }

    class QueueAdapter extends ArrayAdapter<Track> {
        Context context;
        ArrayList<Track> rTracks;

        QueueAdapter(Context c, ArrayList<Track> tracks){
            super(c, R.layout.queue_row, R.id.track_name_view, tracks);
            this.context = c;
            this.rTracks = tracks;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
            LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = layoutInflater.inflate(R.layout.queue_row, parent, false);
            TextView trackName = row.findViewById(R.id.track_name_view);
            TextView artistName = row.findViewById(R.id.artist_name_view);
            ImageButton deleteButton = row.findViewById(R.id.delete_track_button);
            trackName.setSelected(true);
            artistName.setSelected(true);

            if(position == currentTrack){
                trackName.setTextColor((ContextCompat.getColor((getActivity()).getApplicationContext(),
                        R.color.colorPrimary)));
                artistName.setTextColor((ContextCompat.getColor((getActivity()).getApplicationContext(),
                        R.color.colorPrimary)));
            }

            trackName.setText(rTracks.get(position).getName());
            String artists = ((MainActivity) getActivity()).artistNamesFromList(rTracks.get(position).getArtists());
            artistName.setText(artists);

            if(((MainActivity) getActivity()).getUserType().equals("host")) {
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((MainActivity) getActivity()).loadTrack(position);
                        ((MainActivity) getActivity()).playTrack(position);
                        ((MainActivity) getActivity()).setPlaybackVisible(true);
                    }
                });
                deleteButton.setVisibility(View.VISIBLE);
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendDeleteTrackRequest(position);
                    }
                });
            }
            return row;
        }
    }
}
