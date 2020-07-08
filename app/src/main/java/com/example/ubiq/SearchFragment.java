package com.example.ubiq;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//Fragmento da MainActivity onde se pesquisa por música e artistas

public class SearchFragment extends Fragment {
    ListView resultListView;
    ImageButton searchButton;
    EditText searchInput;
    String accessToken;
    int queueId;
    String apiToken;
    HttpResponseManager responseManager;
    private Boolean canceled;

    public SearchFragment(String accessToken, int queueId, String apiToken){
        this.accessToken = accessToken;
        this.queueId = queueId;
        this.apiToken = apiToken;
        this.responseManager = new HttpResponseManager();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        canceled = false;
        this.resultListView = (ListView) getView().findViewById(R.id.resultListView);
        this.searchButton =  getView().findViewById(R.id.search_button);
        this.searchInput = getView().findViewById(R.id.search_input);
        ((MainActivity) getActivity()).getToolbar().setNavigationIcon(null);
        getActivity().setTitle("Search");

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = searchInput.getText().toString();
                resultListView.setVisibility(View.GONE);
                sendSearchRequest(accessToken, input);
            }
        });

        if(((MainActivity) getActivity()).getLastSearchResults().size() > 0)
            resetAdapter(((MainActivity) getActivity()).getLastSearchResults());
    }
    @Override
    public void onResume(){
        super.onResume();
        searchInput.setText("");
    }

    public void cancelRequests(){
        canceled = true;
    }

    public void resetAdapter(ArrayList<SearchResult> list){
        SearchAdapter adapter = new SearchAdapter((getActivity().getApplicationContext()), list);
        resultListView.setAdapter(adapter);
    }


    //Envia pedido de pesquisa à API do Spotify
    public void sendSearchRequest(String accessToken, String input){
        getView().findViewById(R.id.empty_search).setVisibility(View.GONE);
        getActivity().findViewById(R.id.loading_circle).setVisibility(View.VISIBLE);
        String url = makeSearchUrl(input);

        RequestQueue queue = Volley.newRequestQueue(((MainActivity) getActivity()).getApplicationContext());

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                            System.out.println(response);
                            processResponse(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String s = getString(R.string.unknown_err);
                if(error instanceof NoConnectionError){
                    s = getString(R.string.no_connection_err);
                }
                else if (error instanceof TimeoutError) {
                    sendSearchRequest(accessToken, input);
                }
                else if (error instanceof AuthFailureError) {
                    s = getString(R.string.auth_failure_err);
                    ((MainActivity) getActivity()).authenticateSpotifyClient();
                } else if (error instanceof ServerError) {
                    s = new ServerErrorHandler().getErrorString(error);
                }
                System.out.println(error.toString());
                if(!(error instanceof TimeoutError))
                    Toast.makeText(getActivity(), s, Toast.LENGTH_SHORT).show();
                getActivity().findViewById(R.id.loading_circle).setVisibility(View.GONE);
                System.out.println(error.toString());
            }
        })
        {
            /** Passing some request headers* */
            @Override
            public Map getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                headers.put("Authorization", "Bearer " + accessToken);
                return headers;
            }
        };
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private String makeSearchUrl(String input){
        String query = "https://api.spotify.com/v1/search?q=";
        String[] split = input.split(" ");
        for (String s: split) {
            query += "%20" + s;
        }
        query += "&type=track,artist";
        System.out.println(query);
        return query;
    }

    //Filtra os resultados da pesquisa por género e ordena a lista por popularidade de resultados
    //Mostra os resultados ao utilizador
    private void filterResultsAndDisplayList(ArrayList<SearchResult> results){
        String url = "https://ubiq.azurewebsites.net/api/Sala/Filtros/Lista?SalaId=" + queueId;

        RequestQueue queue = Volley.newRequestQueue(getActivity());

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        List filters = responseManager.responseToStringList(response);
                        List l;
                        if (filters.size() > 0) {
                            l = results.stream()
                                    .filter(r -> {
                                        Boolean ans = false;
                                        if (r instanceof Track) {
                                            for (Artist a : ((Track) r).getArtists()) {
                                                for (String g : a.getGenres()) {
                                                    if (containsGenre(filters, g))
                                                        ans = true;
                                                }
                                            }
                                        } else if (r instanceof Artist) {
                                            for (String g : ((Artist) r).getGenres()) {
                                                if (containsGenre(filters, g))
                                                    ans = true;
                                            }
                                        }
                                        return ans;
                                    })
                                    .sorted(Comparator.comparing(SearchResult::getPopularity).reversed())
                                    .collect(Collectors.toList());
                        } else
                            l = results.stream()
                                    .sorted(Comparator.comparing(SearchResult::getPopularity).reversed())
                                    .collect(Collectors.toList());
                        if (!canceled) {
                            if (l.size() == 0)
                                getView().findViewById(R.id.empty_search).setVisibility(View.VISIBLE);
                            getActivity().findViewById(R.id.loading_circle).setVisibility(View.GONE);
                            resultListView.setVisibility(View.VISIBLE);
                            ((MainActivity) getActivity()).setLastSearchResults((ArrayList<SearchResult>) l);
                            SearchAdapter adapter = new SearchAdapter((getActivity().getApplicationContext()), (ArrayList<SearchResult>) l);
                            resultListView.setAdapter(adapter);
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
                    filterResultsAndDisplayList(results);
                }
                else if (error instanceof AuthFailureError) {
                    s = getString(R.string.auth_failure_err);
                } else if (error instanceof ServerError) {
                    s = new ServerErrorHandler().getErrorString(error);
                }
                System.out.println(error.toString());
                if(!(error instanceof TimeoutError))
                    Toast.makeText(getActivity(), s, Toast.LENGTH_SHORT).show();
                getActivity().findViewById(R.id.loading_circle).setVisibility(View.GONE);
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

    private Boolean containsGenre(List filters, String genre){
        Boolean ans = false;
        if(filters.contains(genre)) return true;
        String[] parts = genre.split(" ");
        for(String s : parts){
            if(filters.contains(s))
                ans = true;
        }
        return ans;
    }

    //Adiciona os resultados do tipo "Artist" à lista de resultados
    private void getResultArtists(String result, ArrayList<SearchResult> results) throws JSONException {
        JSONObject obj = new JSONObject(result);
        if(obj.has("artists") && !obj.isNull("artists")) {
            JSONObject child = obj.getJSONObject("artists");
            JSONArray items = child.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                final JSONObject item = items.getJSONObject(i);
                String id = item.getString("id");
                String name = item.getString("name");
                int popularity = item.getInt("popularity");
                JSONArray genres = item.getJSONArray("genres");
                ArrayList<String> genreList = new ArrayList<>();
                for (int j = 0; j < genres.length(); j++) {
                    genreList.add(genres.getString(j));
                }
                JSONArray images = item.getJSONArray("images");
                String image = "unknown";
                if (images.length() > 0) {
                    JSONObject img = images.getJSONObject(0);
                    image = img.getString("url");
                }
                Artist artist = new Artist(id, name, image, genreList, popularity);
                results.add(artist);
            }
        }
    }

    public void getTracksInfo(int index, JSONArray items, ArrayList<SearchResult> results) throws  JSONException{
        if(index < items.length()) {
            final JSONObject item = items.getJSONObject(index);
            String id = item.getString("uri");
            String name = item.getString("name");
            int duration = item.getInt("duration_ms");
            JSONObject album = item.getJSONObject("album");
            String albumName = album.getString("name");
            int popularity = item.getInt("popularity");
            JSONArray images = album.getJSONArray("images");
            JSONObject img = images.getJSONObject(0);
            String image = img.getString("url");
            JSONArray artists = album.getJSONArray("artists");
            Track track = new Track(id, name, albumName, image, duration, popularity);
            addArtistsToTrack(0, index, track, items, artists, results);
        }
        else
            filterResultsAndDisplayList(results);
    }

    //Adiciona lista de artistas à faixa
    public void addArtistsToTrack(int indexA, int indexT, Track track, JSONArray items, JSONArray artists, ArrayList<SearchResult> results) throws JSONException{
        if(indexA < artists.length()) {
            String url = "https://api.spotify.com/v1/artists/" + artists.getJSONObject(indexA).getString("id");
            RequestQueue queue = Volley.newRequestQueue(getActivity());
            // Request a string response from the provided URL.
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject artist = new JSONObject(response);
                                String uri = artist.getString("id");
                                String name = artist.getString("name");
                                ArrayList<String> genreList = new ArrayList<>();
                                JSONArray genres = artist.getJSONArray("genres");
                                for(int i = 0; i < genres.length(); i++){
                                    genreList.add(genres.getString(i));
                                }
                                track.addArtist(new Artist(uri, name, genreList));
                                addArtistsToTrack(indexA + 1, indexT, track, items, artists, results);
                            } catch (JSONException e) { e.printStackTrace(); }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    String s = getString(R.string.unknown_err);
                    if(error instanceof NoConnectionError){
                        s = getString(R.string.no_connection_err);
                    }
                    else if (error instanceof TimeoutError) {
                        try {
                            addArtistsToTrack(indexA, indexT, track, items, artists, results);
                        } catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                    else if (error instanceof AuthFailureError) {
                        s = getString(R.string.auth_failure_err);
                    } else if (error instanceof ServerError) {
                        s = new ServerErrorHandler().getErrorString(error);
                    }
                    System.out.println(error.toString());
                    if(!(error instanceof TimeoutError))
                        Toast.makeText(getActivity(), s, Toast.LENGTH_SHORT).show();
                    System.out.println(error.toString());
                }
            }) {
                @Override
                public Map getHeaders() throws AuthFailureError {
                    HashMap headers = new HashMap();
                    headers.put("Authorization", "Bearer " + accessToken);
                    return headers;
                }
            };
            queue.add(stringRequest);
        }
        else{
            results.add(track);
            getTracksInfo(indexT + 1, items, results);
        }
    }

    //Processa o resultado da pesquisa
    private void processResponse(String response) {
        ArrayList<SearchResult> results = new ArrayList<>();
        try {
            getResultArtists(response, results);
            JSONObject obj = new JSONObject(response);
            if (obj.has("tracks") && !obj.isNull("tracks")) {
                JSONObject child = obj.getJSONObject("tracks");
                JSONArray items = child.getJSONArray("items");
                getTracksInfo(0, items, results);
            }
        }catch (JSONException e){e.printStackTrace();}
    }

    //Pede informação sobre um artista e envia pedido para
    //obter informação sobre os albuns e faixas desse artista e iniciar um ArtistFragment desse artista
    public void getArtistInfoAndStartFragment(Artist artist){
        String url = "https://api.spotify.com/v1/artists/" + artist.getSpotifyId() + "/albums";
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println(response);
                        artist.setAlbums(response);
                        getArtistTopTracksAndStartFragment(artist);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String s = getString(R.string.unknown_err);
                if(error instanceof NoConnectionError){
                    s = getString(R.string.no_connection_err);
                }
                else if (error instanceof TimeoutError) {
                    getArtistInfoAndStartFragment(artist);
                }
                else if (error instanceof AuthFailureError) {
                    s = getString(R.string.auth_failure_err);
                } else if (error instanceof ServerError) {
                    s = new ServerErrorHandler().getErrorString(error);
                }
                System.out.println(error.toString());
                if(!(error instanceof TimeoutError))
                    Toast.makeText(getActivity(), s, Toast.LENGTH_SHORT).show();
                getActivity().findViewById(R.id.loading_circle).setVisibility(View.GONE);
                System.out.println(error.toString());
            }
        })
        {
            /** Passing some request headers* */
            @Override
            public Map getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                headers.put("Authorization", "Bearer " + accessToken);
                return headers;
            }
        };
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    //Pede informação sobre as faixas e albuns de um artista
    //Se tiver sucesso inicia um ArtistFragment do artista
    public void getArtistTopTracksAndStartFragment(Artist artist){
        String url = "https://api.spotify.com/v1/artists/" + artist.getSpotifyId() + "/top-tracks?country=PT";
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println(response);
                        artist.setTopTracks(response);
                        ((MainActivity) getActivity()).startArtistFragment(artist);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String s = getString(R.string.unknown_err);
                if(error instanceof NoConnectionError){
                    s = getString(R.string.no_connection_err);
                }
                else if (error instanceof TimeoutError) {
                    getArtistTopTracksAndStartFragment(artist);
                }
                else if (error instanceof AuthFailureError) {
                    s = getString(R.string.auth_failure_err);
                } else if (error instanceof ServerError) {
                    s = new ServerErrorHandler().getErrorString(error);
                }
                System.out.println(error.toString());
                if(!(error instanceof TimeoutError))
                    Toast.makeText(getActivity(), s, Toast.LENGTH_SHORT).show();
                getActivity().findViewById(R.id.loading_circle).setVisibility(View.GONE);
                System.out.println(error.toString());
            }
        })
        {
            /** Passing some request headers* */
            @Override
            public Map getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                headers.put("Authorization", "Bearer " + accessToken);
                return headers;
            }
        };
        queue.add(stringRequest);
    }

    //Adaptador para a ListView que mostra a lista de resultados da pesquisa
    class SearchAdapter extends ArrayAdapter<SearchResult> {

        Context context;
        ArrayList<SearchResult> results;
        SearchAdapter(Context c, ArrayList<SearchResult> results){
            super(c, R.layout.search_row, results);
            this.context = c;
            this.results = results;
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

            if(results.get(position) instanceof Track){
                Track track = (Track) results.get(position);
                Picasso.get().load(track.getImg()).into(image);
                mainName.setText(track.getName());
                String artists = ((MainActivity) getActivity()).artistNamesFromList(track.getArtists());
                secondaryName.setText(artists);
                addButton.setVisibility(View.VISIBLE);
                addButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((MainActivity)getActivity()).sendPostTrackRequest(track);
                    }
                });
            }
            else if(results.get(position) instanceof Artist){
                Artist artist = (Artist) results.get(position);
                Picasso.get().load(artist.getImg()).into(image);
                mainName.setText(artist.getName());
                secondaryName.setText("Artist");
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getArtistInfoAndStartFragment(artist);
                    }
                });
            }
            return row;
        }
    }
}
