package com.example.ubiq;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.fragment.app.Fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.microsoft.signalr.Action;
import com.microsoft.signalr.Action1;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;
import com.microsoft.signalr.TransportEnum;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    private PrefManager preferences;
    private int queueId;
    private int connectedUsersCount;
    private String queueName;
    private String userType;
    private String apiToken;
    private SpotifyAppRemote mSpotifyAppRemote;
    private ArrayList<SearchResult> lastSearchResults;
    private ArrayList<String> connectedUsers;
    private ContentLoadingProgressBar progressBar;
    private ValueAnimator progressBarAnimator;
    private SettingsFragment settingsFragment;
    private QueueFragment queueFragment;
    private UsersFragment usersFragment;
    private SearchFragment searchFragment;
    private FiltersFragment filtersFragment;
    private Fragment selectedFragment;
    private ToggleButton pausePlayButton;
    private ImageButton skipButton;
    private TextView controllerArtist;
    private TextView controllerTrack;
    private TextView viewerArtist;
    private TextView viewerTrack;
    private Toolbar toolbar;
    private BottomNavigationView bottomNav;
    private Boolean isPlayVisible;
    private Boolean isPlaying;
    private Boolean appRemoteConnected;
    private HubConnection hubConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.preferences = new PrefManager(this);
        this.queueName = preferences.getQueueName();
        this.queueId = preferences.getQueueId();
        this.apiToken = preferences.getAPIAccessToken();
        this.userType = preferences.getUserType();
        this.lastSearchResults = new ArrayList<>();
        this.connectedUsers = new ArrayList<>();
        this.connectedUsersCount = 0;
        this.pausePlayButton = (ToggleButton) findViewById(R.id.pause_play_button);
        this.skipButton = (ImageButton) findViewById(R.id.skip_button);
        this.controllerArtist = (TextView) findViewById(R.id.controller_artist_name);
        this.controllerTrack = (TextView) findViewById(R.id.controller_track_name);
        this.viewerArtist = (TextView) findViewById(R.id.viewer_artist_name);
        this.viewerTrack = (TextView) findViewById(R.id.viewer_track_name);
        this.progressBar = (ContentLoadingProgressBar) findViewById(R.id.progbar);
        this.progressBarAnimator = ValueAnimator.ofInt(0, progressBar.getMax());
        this.appRemoteConnected = false;
        this.isPlayVisible = false;
        this.isPlaying = false;
        this.settingsFragment = new SettingsFragment(queueId,apiToken);
        this.filtersFragment = new FiltersFragment(queueId, apiToken);
        this.queueFragment = new QueueFragment(apiToken, queueId);
        this.usersFragment = new UsersFragment(queueId, apiToken, userType);
        this.searchFragment = new SearchFragment(getSpotifyAccessToken(), queueId, apiToken);
        this.selectedFragment = queueFragment;
        this.toolbar =  (Toolbar) findViewById(R.id.toolbar);

        if(userType.equals("host")){
            bottomNav = findViewById(R.id.bottom_navigation_host);
        }
        else {
            bottomNav = findViewById(R.id.bottom_navigation_guest);
        }
        bottomNav.setVisibility(View.VISIBLE);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        setSupportActionBar(toolbar);
        setTitle(getQueueName());

        hubConnection = HubConnectionBuilder.create("http://192.168.1.69:5000/updatehub")
                .withTransport(TransportEnum.LONG_POLLING)
                .build();

        if(hubConnection.getConnectionState() == HubConnectionState.DISCONNECTED)
            hubConnection.start();

        hubConnection.on("ReceiveNewUserList", new Action() {
            @Override
            public void invoke() {
                sendGetUsersRequest();
            }
        });

        hubConnection.on("ReceiveNewQueue", new Action() {
            @Override
            public void invoke() {
                sendGetQueueRequest();
            }
        });

        hubConnection.on("QueueDeleted", new Action() {
            @Override
            public void invoke() {
                preferences.setQueueId(0);
                if(hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
                    hubConnection.send("RemoveFromQueue", queueName);
                }
                Intent i = new Intent(getApplicationContext(), HomeActivity.class);
                i.putExtra("DELETED", true);
                startActivity(i);
                finish();
            }
        });

        hubConnection.on("TrackLoaded", new Action() {
            @Override
            public void invoke() {
                sendGetCurrentTrackRequest();
            }
        });

        hubConnection.on("UserBanned", new Action1<String>() {
            @Override
            public void invoke(String user) {
                System.out.println("myself: " + preferences.getUsername().toLowerCase());
                System.out.println("banned: " + user);
                if(preferences.getUsername().toLowerCase().equals(user)){
                    System.out.println("okay");
                    if(hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
                        hubConnection.send("UpdateUserList", queueName);
                        hubConnection.send("RemoveFromQueue", queueName);
                    }
                    preferences.setQueueId(0);
                    Intent i = new Intent(getApplicationContext(), HomeActivity.class);
                    i.putExtra("BANNED", true);
                    startActivity(i);
                    finish();
                }
            }
        }, String.class);


        if(System.currentTimeMillis() - preferences.getSpotifyTokenTime() >= 3500000)
            authenticateSpotifyClient();


        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, queueFragment).commit();
        }

        pausePlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPlaying){
                    pausePlayback();
                }
                else{
                    resumePlayback();
                }
            }
        });

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    loadTrack(queueFragment.currentTrack+1,1);
            }
        });

        System.out.println("Queue: " + queueId + " User: " + apiToken + "-----------");
        sendGetQueueRequest();
        sendGetUsersRequest();
        sendGetCurrentTrackRequest();
        while(hubConnection.getConnectionState() == HubConnectionState.DISCONNECTED);
        sendUpdateUsers();
    }


    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    findViewById(R.id.loading_circle).setVisibility(View.GONE);
                    switch (item.getItemId()) {
                        case R.id.nav_queue:
                            startQueueFragment();
                            break;
                        case R.id.nav_add_song:
                            startSearchFragment();
                            break;
                        case R.id.nav_users:
                            startUsersFragment();
                            break;
                        case R.id.nav_settings:
                            startSettingsFragment();
                            break;
                    }
                    return true;
                }
            };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(userType.equals("guest")) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.toolbar_menu_guest, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.quit_queue:
                sendQuitQueueRequest();
            case R.id.filters:
                sendGetFiltersRequest();
            default:
                return false;
        }
    }

    public ArrayList<String> getConnectedUsers(){
        return this.connectedUsers;
    }

    public Toolbar getToolbar(){
        return this.toolbar;
    }

    public ArrayList<SearchResult> getLastSearchResults(){
        return this.lastSearchResults;
    }

    public void setLastSearchResults(ArrayList<SearchResult> list){
        this.lastSearchResults = list;
    }

    public int getConnectedUsersCount(){
        return this.connectedUsersCount;
    }

    public String getUserType(){
        return this.userType;
    }

    public String getQueueName(){
        return this.queueName;
    }

    public String getUsername(){
        return preferences.getUsername();
    }

    private void pausePlayback(){
        //mSpotifyAppRemote.getPlayerApi().pause();
        pausePlayButton.setChecked(false);
        progressBarAnimator.pause();
        isPlaying = false;
    }

    private void resumePlayback(){
        //mSpotifyAppRemote.getPlayerApi().resume();
        pausePlayButton.setChecked(true);
        progressBarAnimator.resume();
        isPlaying = true;
    }

    public void cancelPendingRequests(){
        queueFragment.cancelRequests();
        searchFragment.cancelRequests();
        settingsFragment.cancelRequests();
        filtersFragment.cancelRequests();
        usersFragment.cancelRequests();
    }

    public void setPlaybackVisibility(Boolean visible){
        if(visible && !isPlayVisible && userType.equals("host")){
            findViewById(R.id.playback_controller).setVisibility(View.VISIBLE);
            isPlayVisible = true;
        }
        else if(!visible && isPlayVisible && userType.equals("host")){
            findViewById(R.id.playback_controller).setVisibility(View.GONE);
            isPlayVisible = false;
        }
        else if(visible && !isPlayVisible && userType.equals("guest")){
            findViewById(R.id.playback_viewer).setVisibility(View.VISIBLE);
            isPlayVisible = true;
        }
        else if(!visible && isPlayVisible && userType.equals("guest")){
            findViewById(R.id.playback_viewer).setVisibility(View.GONE);
            isPlayVisible = false;
        }
    }

    public void goToPreviousFragment(){
        cancelPendingRequests();
        getSupportFragmentManager().popBackStack();
    }

    public void startQueueFragment(){
        cancelPendingRequests();
        selectedFragment = queueFragment;
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                selectedFragment).commit();
    }

    public void startSearchFragment(){
        cancelPendingRequests();
        selectedFragment = searchFragment;
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                selectedFragment).commit();
    }

    public void startUsersFragment(){
        cancelPendingRequests();
        selectedFragment = usersFragment;
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                selectedFragment).commit();
    }

    public void startSettingsFragment(){
        cancelPendingRequests();
        selectedFragment = settingsFragment;
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                selectedFragment).commit();
    }

    public void startFiltersFragment(){
        cancelPendingRequests();
        selectedFragment = filtersFragment;
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                selectedFragment).commit();
    }

    public void startArtistFragment(Artist artist){
        selectedFragment = new ArtistFragment(artist, getSpotifyAccessToken());
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                selectedFragment).addToBackStack("last").commit();
    }

    public void startAlbumFragment(MusicAlbum album){
        selectedFragment = new AlbumFragment(album);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                selectedFragment).addToBackStack("last").commit();
    }

    public void authenticateSpotifyClient(){
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(preferences.getSpotifyClientId(), AuthenticationResponse.Type.TOKEN, preferences.getSpotifyRedirectUri());

        builder.setScopes(new String[]{"app-remote-control", "user-read-private"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, preferences.getRequestCode(), request);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == preferences.getRequestCode()) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    preferences.setSpotifyAccessToken(response.getAccessToken());
                    preferences.setSpotifyTokenTime(System.currentTimeMillis());
                    break;

                // Auth flow returned an error
                case ERROR:
                    System.out.println(response.toString());
                    break;

                // Most likely auth flow was cancelled
                default:
                    System.out.println(response.toString());
            }
        }
    }


    public String getSpotifyAccessToken(){
        return preferences.getSpotifyAccessToken();
    }

    public void connectSpotifyAppRemote(String uri){
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(preferences.getSpotifyClientId())
                        .setRedirectUri(preferences.getSpotifyRedirectUri())
                        .showAuthView(true)
                        .build();

        SpotifyAppRemote.connect(this, connectionParams,
                new Connector.ConnectionListener() {

                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d("MainActivity", "Connected! Yay!");
                        appRemoteConnected = true;
                        mSpotifyAppRemote.getPlayerApi().play(uri);
                        System.out.println("Now playing " + uri);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("MainActivity", throwable.getMessage(), throwable);
                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });
    }

    public void banUser(String user){
        if(hubConnection.getConnectionState() == HubConnectionState.CONNECTED)
            hubConnection.send("banFromQueue", queueName, user);
    }

    public String artistNamesFromList(ArrayList<Artist> list){
        String str = list.get(0).getName();
        for(int i = 1; i < list.size(); i++){
            str += ", " + list.get(i).getName();
        }
        return str;
    }

    public void loadTrack(int position, int mode){
        if(position >= queueFragment.queueTracks.size()){
            position = 0;
        }
        if(queueFragment.queueTracks.size() > 0 && position >= 0) {
            Track track = queueFragment.queueTracks.get(position);
            queueFragment.currentTrack = position;

            if (selectedFragment == queueFragment) {
                queueFragment.resetAdapter();
            }

            if (userType.equals("host")) {
                controllerArtist.setText(artistNamesFromList(track.getArtists()));
                controllerTrack.setText(track.getName());
                controllerArtist.setSelected(true);
                controllerTrack.setSelected(true);
                progressBarAnimator.setDuration(track.getDuration());
                progressBarAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        progressBar.setProgress((Integer) animation.getAnimatedValue());
                    }
                });
                progressBarAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        System.out.println("animation end");
                        loadTrack(queueFragment.currentTrack + 1, 1);
                    }
                });
                progressBarAnimator.start();
                progressBarAnimator.pause();
                if(mode == 1)
                    playTrack(position);
                sendPostCurrentTrackRequest(position);
            } else if (userType.equals("guest")) {
                viewerArtist.setText(artistNamesFromList(track.getArtists()));
                viewerTrack.setText(track.getName());
                viewerArtist.setSelected(true);
                viewerTrack.setSelected(true);
            }
            setPlaybackVisibility(true);
        }
        else{
            queueFragment.currentTrack = -1;
            if(userType.equals("host"))
                sendPostCurrentTrackRequest(-1);
            setPlaybackVisibility(false);
        }
    }

    public void playTrack(int position){
        Track track = queueFragment.queueTracks.get(position);
        if(appRemoteConnected){
            //mSpotifyAppRemote.getPlayerApi().play(track.getSpotifyId());
        }
        else{
            //connectSpotifyAppRemote(track.getSpotifyId());
        }
        pausePlayButton.setChecked(true);
        this.isPlaying = true;
        progressBarAnimator.resume();
    }

    public void setQueueName(String name){
        this.queueName = name;
        preferences.setQueueName(name);
    }


    private ArrayList<Track> stringToQueue(String str){
        ArrayList<Track> tracks = new ArrayList<>();
        try {
            JSONArray items = new JSONArray(str);
            for (int i = 0; i < items.length(); i++) {
                final JSONObject item = items.getJSONObject(i);
                String id = item.getString("URI");
                String name = item.getString("Nome");
                String album = item.getString("Album");
                ArrayList<Artist> artistList = new ArrayList<>();
                JSONArray artists = item.getJSONArray("Artistas");
                for(int j = 0; j < artists.length(); j++) {
                    Artist artist = new Artist(artists.getString(j));
                    artistList.add(artist);
                }
                int duration = item.getInt("Duracao_ms");
                String image = "unknown";
                Track track = new Track(id, name, artistList, album, image, duration);
                tracks.add(track);
            }
        } catch (Exception e) {}
        return tracks;
    }

    public void showfiltersDialog(ArrayList<String> filters){
        Dialog filtersForm = new Dialog(this);
        filtersForm.setContentView(R.layout.form_simple_list);
        ListView bannedUsersListView = filtersForm.findViewById(R.id.list_view);
        TextView title = filtersForm.findViewById(R.id.title);
        TextView empty = filtersForm.findViewById(R.id.empty_text);
        title.setText("Allowed Genres");

        if(filters.size() == 0){
            empty.setVisibility(View.VISIBLE);
            empty.setText(R.string.no_filters);
        }
        else {
            FiltersAdapter adapter = new FiltersAdapter(getApplicationContext(), filters);
            bannedUsersListView.setAdapter(adapter);
        }

        filtersForm.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        filtersForm.show();
    }

    public void sendGetQueueRequest() {
        String url = "https://ubiq.azurewebsites.net/api/Sala/Musicas/Lista?SalaId=" + queueId;

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                            findViewById(R.id.loading_circle).setVisibility(View.GONE);
                            queueFragment.queueTracks = stringToQueue(response);
                            if (queueFragment.queueTracks.size() == 0)
                                setPlaybackVisibility(false);
                            if(selectedFragment == queueFragment)
                                queueFragment.resetAdapter();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String s = getString(R.string.unknown_err);
                if(error instanceof NoConnectionError){
                    s = getString(R.string.no_connection_err);
                }
                else if (error instanceof TimeoutError) {
                    sendGetQueueRequest();
                }
                else if (error instanceof AuthFailureError) {
                    s = getString(R.string.auth_failure_err);
                } else if (error instanceof ServerError) {
                    s = new ServerErrorHandler().getErrorString(error);
                }
                System.out.println(error.toString());
                if(!(error instanceof TimeoutError))
                    Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                findViewById(R.id.loading_circle).setVisibility(View.GONE);
            }
        }) {
            @Override
            public Map getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                headers.put("Authorization", "Bearer " + apiToken);
                return headers;
            }
        };
        //Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    public void sendPostTrackRequest(Track track) {
        String url = "https://ubiq.azurewebsites.net/api/Sala/Musicas/Adicionar?SalaId=" + queueId;
        String requestBody;

        try {
            JSONArray jsonArray = new JSONArray();
            for(int i = 0; i < track.getArtists().size(); i++){
                jsonArray.put(track.getArtists().get(i).getName());
               }
            JSONObject jsonObject2 = new JSONObject();
            jsonObject2.put("URI", track.getSpotifyId());
            jsonObject2.put("Nome", track.getName());
            jsonObject2.put("Duracao_ms", track.getDuration());
            jsonObject2.put("Album", track.getAlbumName());
            jsonObject2.put("Url_imagem", track.getImg());
            jsonObject2.put("Artistas", jsonArray);
            requestBody = jsonObject2.toString();

            RequestQueue queue = Volley.newRequestQueue(this);

            StringRequest stringRequest = new StringRequest(1, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    queueFragment.queueTracks.add(track);
                    Toast.makeText(MainActivity.this, "Track added to queue", Toast.LENGTH_SHORT).show();
                    if(hubConnection.getConnectionState() == HubConnectionState.CONNECTED)
                        hubConnection.send("UpdateQueue", queueName);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    String s = getString(R.string.unknown_err);
                    if(error instanceof NoConnectionError){
                        s = getString(R.string.no_connection_err);
                    }
                    else if (error instanceof TimeoutError) {
                        sendPostTrackRequest(track);
                    }
                    else if (error instanceof AuthFailureError) {
                        s = getString(R.string.auth_failure_err);
                    } else if (error instanceof ServerError) {
                        s = new ServerErrorHandler().getErrorString(error);
                    }
                    System.out.println(error.toString());
                    if(!(error instanceof TimeoutError))
                        Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                    findViewById(R.id.loading_circle).setVisibility(View.GONE);
                }
            }) {
                @Override
                public Map getHeaders() throws AuthFailureError {
                    HashMap headers = new HashMap();
                    headers.put("Authorization", "Bearer " + apiToken);
                    return headers;
                }
                @Override
                public String getBodyContentType() {
                    return String.format("application/json; charset=utf-8");
                }

                @Override
                public byte[] getBody() throws AuthFailureError {
                    try {
                        return requestBody == null ? null : requestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s",
                                requestBody, "utf-8");
                        return null;
                    }
                }
            };
            queue.add(stringRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendQuitQueueRequest(){
       String url = "https://ubiq.azurewebsites.net/api/Sala/Sair?SalaId=" + this.queueId;

       RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.DELETE, url,
            new Response.Listener<String>() {
                 @Override
                 public void onResponse(String response) {
                     preferences.setQueueId(0);
                     if(hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
                         hubConnection.send("UpdateUserList", queueName);
                         hubConnection.send("RemoveFromQueue", queueName);
                     }
                     startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                     finish();
                 }
            }, new Response.ErrorListener() {
                  @Override
                  public void onErrorResponse(VolleyError error) {
                      String s = getString(R.string.unknown_err);
                      if(error instanceof NoConnectionError){
                          s = getString(R.string.no_connection_err);
                      }
                      else if (error instanceof TimeoutError) {
                          sendQuitQueueRequest();
                      }
                      else if (error instanceof AuthFailureError) {
                          s = getString(R.string.auth_failure_err);
                      } else if (error instanceof ServerError) {
                          s = new ServerErrorHandler().getErrorString(error);
                      }
                      System.out.println(error.toString());
                      if(!(error instanceof TimeoutError))
                          Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                      findViewById(R.id.loading_circle).setVisibility(View.GONE);
                  }
              }) {
                  @Override
                  public Map getHeaders() throws AuthFailureError {
                      HashMap headers = new HashMap();
                      headers.put("Authorization", "Bearer " + apiToken);
                      return headers;
                  }
              };                                                                                                                                               
              queue.add(stringRequest);
    }

    /*public void sendPutTrackRequest(int from, int to){
        //CHANGE URL WHEN IMPLEMENTED ON SERVER
        String url = "https://ubiq.azurewebsites.net/api/Sala/Users?SalaId=" + queueId;

        RequestQueue queue = Volley.newRequestQueue(this);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.PUT, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Track track = queueTracks.get(from);
                        queueTracks.remove(from);
                        queueTracks.add(to, track);
                        queueFragment.resetAdapter();
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
        //Add the request to the RequestQueue.
        queue.add(stringRequest);
    }*/

    public void sendGetUsersRequest(){
        String url = "https://ubiq.azurewebsites.net/api/Sala/Utilizadores/Lista?SalaId=" + queueId;

        RequestQueue queue = Volley.newRequestQueue(this);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        findViewById(R.id.loading_circle).setVisibility(View.GONE);
                        connectedUsers = new HttpResponseManager().responseToStringList(response);
                        connectedUsersCount = connectedUsers.size();
                        if(selectedFragment == usersFragment) {
                            usersFragment.resetAdapter();
                        }
                        if(selectedFragment == queueFragment) {
                            queueFragment.resetAdapter();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String s = getString(R.string.unknown_err);
                if(error instanceof NoConnectionError){
                    s = getString(R.string.no_connection_err);
                }
                else if (error instanceof TimeoutError) {
                    sendGetUsersRequest();
                }
                else if (error instanceof AuthFailureError) {
                    s = getString(R.string.auth_failure_err);
                } else if (error instanceof ServerError) {
                    s = new ServerErrorHandler().getErrorString(error);
                }
                System.out.println(error.toString());
                Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                findViewById(R.id.loading_circle).setVisibility(View.GONE);
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

    public void sendGetCurrentTrackRequest(){
        String url = "https://ubiq.azurewebsites.net/api/Sala/Musicas/MusicaAtual?SalaId=" + queueId;

        RequestQueue queue = Volley.newRequestQueue(this);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        int pos = Integer.parseInt(response);
                        System.out.println("current: " + pos);
                        loadTrack(pos - 1, 0);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String s = getString(R.string.unknown_err);
                if(error instanceof NoConnectionError){
                    s = getString(R.string.no_connection_err);
                }
                else if (error instanceof TimeoutError) {
                    sendGetCurrentTrackRequest();
                }
                else if (error instanceof AuthFailureError) {
                    s = getString(R.string.auth_failure_err);
                } else if (error instanceof ServerError) {
                    s = new ServerErrorHandler().getErrorString(error);
                }
                System.out.println(error.toString());
                if(!(error instanceof TimeoutError))
                    Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                findViewById(R.id.loading_circle).setVisibility(View.GONE);
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
        //Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    public void sendPostCurrentTrackRequest(int pos){
        String url = "https://ubiq.azurewebsites.net/api/Sala/Musicas/MusicaAtual?SalaId=" + queueId +
                "&Posicao=" + (pos + 1);

        RequestQueue queue = Volley.newRequestQueue(this);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
                            hubConnection.send("LoadTrack", queueName);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String s = getString(R.string.unknown_err);
                if(error instanceof NoConnectionError){
                    s = getString(R.string.no_connection_err);
                }
                else if (error instanceof TimeoutError) {
                    sendGetCurrentTrackRequest();
                }
                else if (error instanceof AuthFailureError) {
                    s = getString(R.string.auth_failure_err);
                } else if (error instanceof ServerError) {
                    s = new ServerErrorHandler().getErrorString(error);
                }
                System.out.println(error.toString());
                if(!(error instanceof TimeoutError))
                    Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
                findViewById(R.id.loading_circle).setVisibility(View.GONE);
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
        //Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void sendGetFiltersRequest(){
        String url = "https://ubiq.azurewebsites.net/api/Sala/Filtros/Lista?SalaId=" + queueId;

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        ArrayList<String> l = new HttpResponseManager().responseToStringList(response);
                        showfiltersDialog(l);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String s = getString(R.string.unknown_err);
                if(error instanceof NoConnectionError){
                    s = getString(R.string.no_connection_err);
                }
                else if (error instanceof TimeoutError) {
                    sendGetFiltersRequest();
                }
                else if (error instanceof AuthFailureError) {
                    s = getString(R.string.auth_failure_err);
                } else if (error instanceof ServerError) {
                    s = new ServerErrorHandler().getErrorString(error);
                }
                System.out.println(error.toString());
                if(!(error instanceof TimeoutError))
                    Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
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

    public void sendUpdateUsers(){
        if(hubConnection.getConnectionState() == HubConnectionState.CONNECTED){
            hubConnection.send("UpdateUserList", queueName);
        }
    }
                                                            
    public void removeTrackFromQueue(int position){
        queueFragment.queueTracks.remove(position);
        if(position == queueFragment.currentTrack){
            if(isPlaying){
               pausePlayback();         
            }
            loadTrack(position, 0);
        }
        else if(position < queueFragment.currentTrack) {
            queueFragment.currentTrack--;
            sendPostCurrentTrackRequest(queueFragment.currentTrack);
        }

        if(hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            hubConnection.send("UpdateQueue", queueName);
        }
    }

    public void deleteQueue(){
        preferences.setQueueId(0);
        if(hubConnection.getConnectionState() == HubConnectionState.CONNECTED)
            hubConnection.send("DeleteQueue", queueName);
        Intent startIntent = new Intent(getApplicationContext(), HomeActivity.class);
        startActivity(startIntent);
        finish();
    }

    class FiltersAdapter extends ArrayAdapter<String> {
        Context context;
        ArrayList<String> filters;

        FiltersAdapter(Context c, ArrayList<String> filters) {
            super(c, R.layout.filter_row, filters);
            this.context = c;
            this.filters = filters;
        }
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = layoutInflater.inflate(R.layout.filter_row, parent, false);
            TextView name = row.findViewById(R.id.name_view);
            row.findViewById(R.id.remove_button).setVisibility(View.GONE);
            name.setText(filters.get(position));

            return row;
        }
    }
}
