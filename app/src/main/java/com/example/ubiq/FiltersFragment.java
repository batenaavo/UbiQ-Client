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
import android.widget.RelativeLayout;
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
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FiltersFragment extends Fragment {
    private TabLayout tabLayout;
    ListView filtersListView;
    ArrayList<String> filters;
    ArrayList<String> addFilters;
    private int queueId;
    private String apiToken;
    HttpResponseManager responseManager;
    TextView emptyText;
    private Toolbar searchBar;
    private ImageButton searchButton;
    private TextView searchInput;
    private Boolean canceled;
    RelativeLayout clearFilters;

    public FiltersFragment(int queueId, String apiToken){
        this.queueId = queueId;
        this.apiToken = apiToken;
        filters = new ArrayList<>();
        addFilters = new ArrayList<>();
        responseManager = new HttpResponseManager();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_filters, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        canceled = false;
        this.filtersListView = (ListView) getView().findViewById(R.id.filters_list_view);
        this.emptyText = getView().findViewById(R.id.empty_text);
        this.searchBar = getView().findViewById(R.id.search_bar);
        this.searchInput = (TextView) getView().findViewById(R.id.search_input);
        this.searchButton = (ImageButton) getView().findViewById(R.id.search_button);
        this.tabLayout = (TabLayout) getView().findViewById(R.id.simpleTabLayout);
        TabLayout.Tab nearTab = tabLayout.newTab();
        TabLayout.Tab searchTab = tabLayout.newTab();
        nearTab.setText(R.string.filters_tab);
        searchTab.setText(R.string.add_filters_tab);
        tabLayout.addTab(nearTab);
        tabLayout.addTab(searchTab);
        clearFilters = getView().findViewById(R.id.clear_filters);
        Toolbar toolbar =  ((MainActivity) getActivity()).getToolbar();
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).startSettingsFragment();
            }
        });
        getActivity().setTitle(((MainActivity) getActivity()).getQueueName());
        getActivity().setTitle("Queue Filters");
        sendGetFiltersRequest();

        this.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    searchBar.setVisibility(View.GONE);
                    sendGetFiltersRequest();
                } else {
                    clearFilters.setVisibility(View.GONE);
                    searchBar.setVisibility(View.VISIBLE);
                    searchInput.setText("");
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                filtersListView.setVisibility(View.GONE);
                getView().findViewById(R.id.loading_circle).setVisibility(View.GONE);
                emptyText.setVisibility(View.GONE);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        this.searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = searchInput.getText().toString();
                getView().findViewById(R.id.loading_circle).setVisibility(View.VISIBLE);
                sendGetFiltersByNameRequest(input);
            }
        });

        clearFilters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDeleteFiltersRequest();
            }
        });
    }

    public void cancelRequests(){
        canceled = true;
    }

    private void setFiltersAdapter(){
        filtersListView.setVisibility(View.VISIBLE);
        FiltersAdapter adapter = new FiltersAdapter((getActivity().getApplicationContext()), filters);
        filtersListView.setAdapter(adapter);
        if(filters.size() == 0) {
            clearFilters.setVisibility(View.GONE);
            emptyText.setText(R.string.no_filters);
            emptyText.setVisibility(View.VISIBLE);
        }
        else
            clearFilters.setVisibility(View.VISIBLE);
    }

    private void setAddFiltersAdapter(){
        AddFiltersAdapter adapter = new AddFiltersAdapter((getActivity().getApplicationContext()), addFilters);
        filtersListView.setAdapter(adapter);
        filtersListView.setVisibility(View.VISIBLE);
        if(addFilters.size() == 0) {
            emptyText.setText(R.string.empty_search);
            emptyText.setVisibility(View.VISIBLE);
        }

    }

    private void sendPostFiltersRequest(){
        String url = "https://ubiq.azurewebsites.net/api/Sala/Filtros/Alterar?SalaId=" + queueId;
        String requestBody;

        try {
            JSONArray jsonArray = new JSONArray();
            for(String f : filters){
                jsonArray.put(f);
            }
            requestBody = jsonArray.toString();

            RequestQueue queue = Volley.newRequestQueue(getActivity());

            StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    if(!canceled) {
                        if (tabLayout.getSelectedTabPosition() == 0)
                            setFiltersAdapter();
                        else
                            setAddFiltersAdapter();
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
                        sendPostFiltersRequest();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendDeleteFiltersRequest(){
        String url = "https://ubiq.azurewebsites.net/api/Sala/Filtros/Alterar?SalaId=" + queueId;
        String requestBody;

        try {
            JSONArray jsonArray = new JSONArray();
            requestBody = jsonArray.toString();

            RequestQueue queue = Volley.newRequestQueue(getActivity());

            StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    if(!canceled) {
                        filters.clear();
                        setFiltersAdapter();
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
                        sendDeleteFiltersRequest();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendGetFiltersRequest(){
        filtersListView.setVisibility(View.GONE);
        getView().findViewById(R.id.loading_circle).setVisibility(View.VISIBLE);
        String url = "https://ubiq.azurewebsites.net/api/Sala/Filtros/Lista?SalaId=" + queueId;

        RequestQueue queue = Volley.newRequestQueue(getActivity());

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(!canceled) {
                            filters = responseManager.responseToStringList(response);
                            getView().findViewById(R.id.loading_circle).setVisibility(View.GONE);
                            setFiltersAdapter();
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
                    sendGetFiltersRequest();
                }
                else if (error instanceof AuthFailureError) {
                    s = getString(R.string.auth_failure_err);
                } else if (error instanceof ServerError) {
                    s = new ServerErrorHandler().getErrorString(error);
                }
                System.out.println(error.toString());
                if(!(error instanceof TimeoutError))
                    Toast.makeText(getActivity(), s, Toast.LENGTH_SHORT).show();
                getView().findViewById(R.id.loading_circle).setVisibility(View.GONE);
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

    private void sendGetFiltersByNameRequest(String name){
        filtersListView.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);
        getView().findViewById(R.id.loading_circle).setVisibility(View.VISIBLE);
        String url = "https://ubiq.azurewebsites.net/api/Filtros?Nome=" + name;

        RequestQueue queue = Volley.newRequestQueue(getActivity());

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(!canceled) {
                            addFilters = responseManager.responseToStringList(response);
                                addFilters.sort((s1, s2) -> s1.length() - s2.length());
                            getView().findViewById(R.id.loading_circle).setVisibility(View.GONE);
                            setAddFiltersAdapter();
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
                    sendGetFiltersRequest();
                }
                else if (error instanceof AuthFailureError) {
                    s = getString(R.string.auth_failure_err);
                } else if (error instanceof ServerError) {
                    s = new ServerErrorHandler().getErrorString(error);
                }
                System.out.println(error.toString());
                if(!(error instanceof TimeoutError)) {
                    Toast.makeText(getActivity(), s, Toast.LENGTH_SHORT).show();
                    getView().findViewById(R.id.loading_circle).setVisibility(View.GONE);
                }
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
            ImageButton removeButton = (ImageButton) row.findViewById(R.id.remove_button);
            name.setText(filters.get(position));

            removeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    filters.remove(position);
                    sendPostFiltersRequest();
                }
            });
            return row;
        }
    }

    class AddFiltersAdapter extends ArrayAdapter<String> {
        Context context;
        ArrayList<String> addFilters;

        AddFiltersAdapter(Context c, ArrayList<String> addFilters) {
            super(c, R.layout.add_element_row, addFilters);
            this.context = c;
            this.addFilters = addFilters;
        }
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = layoutInflater.inflate(R.layout.add_element_row, parent, false);
            TextView name = row.findViewById(R.id.name_view);
            ImageButton addButton = (ImageButton) row.findViewById(R.id.add_button);
            ImageView check = (ImageView) row.findViewById(R.id.checked_image);

            if(filters.contains(addFilters.get(position))){
                addButton.setVisibility(View.GONE);
                check.setVisibility(View.VISIBLE);
            }

            name.setText(addFilters.get(position));

            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    filters.add(addFilters.get(position));
                    sendPostFiltersRequest();
                    addButton.setVisibility(View.GONE);
                    check.setVisibility(View.VISIBLE);
                }
            });
            return row;
        }
    }
}
