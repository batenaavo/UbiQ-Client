package com.example.ubiq;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

//Activity que corresponde à página inicial da aplicação (depois de autenticado)
//Nesta atividade estão os métodos de obter listas de queues por proximidade e nome
//e os métodos para entrar numa queue e criar uma nova queue

public class HomeActivity extends AppCompatActivity {

    private PrefManager preferences;
    private Toolbar searchBar;
    private ImageButton searchButton;
    private TextView searchInput;
    private TextView emptyText;
    private TextView allowLocationBtn;
    private ListView queuesListView;
    private TabLayout tabLayout;
    private ArrayList<String> queues;
    private String selectedQueue;
    private Button createQueue;
    private String newQueueName;
    private String newQueuePwd;
    private int maxTracks;
    private int maxTime;
    private String apiToken;
    private FusedLocationProviderClient mFusedLocationClient;
    private Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        this.preferences = new PrefManager(this);
        this.loadingDialog = new Dialog(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        this.apiToken = preferences.getAPIAccessToken();
        this.createQueue = (Button) findViewById(R.id.open_form);
        this.queuesListView = findViewById(R.id.queues_list_view);
        this.queues = new ArrayList<>();
        this.searchBar = findViewById(R.id.search_bar);
        this.emptyText = (TextView) findViewById(R.id.empty_text);
        this.allowLocationBtn = (TextView) findViewById(R.id.allow_location_btn);
        this.searchInput = (TextView) findViewById(R.id.search_input);
        this.searchButton = (ImageButton) findViewById(R.id.search_button);
        this.tabLayout = (TabLayout) findViewById(R.id.simpleTabLayout);
        TabLayout.Tab nearTab = tabLayout.newTab();
        TabLayout.Tab searchTab = tabLayout.newTab();
        nearTab.setText("Queues near you");
        searchTab.setText("Search for a Queue");
        tabLayout.addTab(nearTab);
        tabLayout.addTab(searchTab);

        setSupportActionBar(findViewById(R.id.toolbar));
        setTitle("UbiQ");

        if(getIntent().hasExtra("BANNED")){
            if(getIntent().getExtras().getBoolean("BANNED")) {
                showConfirmDialog(getResources().getString(R.string.banned));
            }
        }
        else if(getIntent().hasExtra("DELETED")){
            if(getIntent().getExtras().getBoolean("DELETED")) {
                showConfirmDialog(getResources().getString(R.string.deleted_queue));
            }
        }

        this.createQueue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(preferences.getSpotifyAccountType() == null){
                    startActivity(new Intent(getApplicationContext(), SplashActivity.class));
                    finish();
                }
                else if(preferences.getSpotifyAccountType().equals("premium")) {
                    showCreateQueueDialog(v);
                }
                else
                    showConfirmDialog(getResources().getString(R.string.not_premium_msg));
            }
        });

        this.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    searchBar.setVisibility(View.GONE);
                    if(preferences.getLocationAccess() < 1){
                        emptyText.setText(R.string.gps_denied);
                        allowLocationBtn.setVisibility(View.VISIBLE);
                        emptyText.setVisibility(View.VISIBLE);
                    }
                    else{
                        getLocationAndSendRequest("get");
                    }
                } else {
                    searchBar.setVisibility(View.VISIBLE);
                    searchInput.setText("");
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                queuesListView.setVisibility(View.GONE);
                findViewById(R.id.loading_circle).setVisibility(View.GONE);
                emptyText.setVisibility(View.GONE);
                allowLocationBtn.setVisibility(View.GONE);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    searchBar.setVisibility(View.GONE);
                    if(preferences.getLocationAccess() < 1){
                        emptyText.setText(R.string.gps_denied);
                        allowLocationBtn.setVisibility(View.VISIBLE);
                        emptyText.setVisibility(View.VISIBLE);
                    }
                    else{
                        getLocationAndSendRequest("get");
                    }
                } else {
                    searchBar.setVisibility(View.VISIBLE);
                    searchInput.setText("");
                }
            }
        });

        this.searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = searchInput.getText().toString();
                findViewById(R.id.loading_circle).setVisibility(View.VISIBLE);
                sendGetQueuesByNameRequest(input);
            }
        });

        allowLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(preferences.getLocationAccess() == 0) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}
                            , 10);
                }
                else if(preferences.getLocationAccess() == -1)
                    openAppSettings();
            }
        });

        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}
                , 10);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.logout_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.logout:
                sendLogoutRequest();
            default:
                return false;
        }
    }

    //Método que lida com pedidos de permissoes, neste caso de localização
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //pedido autorizado -> pedir lista de queues por proximidade
                    preferences.setLocationAccess(1);
                    emptyText.setVisibility(View.GONE);
                    getLocationAndSendRequest("get");
                }
                else if(!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                    //pedido negado permanentemente
                    preferences.setLocationAccess(-1);
                }
                else{
                    //pedido negado
                    preferences.setLocationAccess(0);
                    emptyText.setText(R.string.gps_denied);
                    allowLocationBtn.setVisibility(View.VISIBLE);
                    emptyText.setVisibility(View.VISIBLE);
                }
                break;
            default:
                break;
        }
    }

    //Quando a aplicação é retomada após interrupção e a permissão de localização foi entretanto garantida
    //exteriormente à aplicação, efetuar pedido de queues por proximidade
    @Override
    public void onResume(){
        super.onResume();
        int permissionLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if(permissionLocation == PackageManager.PERMISSION_GRANTED) {
            emptyText.setVisibility(View.GONE);
            allowLocationBtn.setVisibility(View.GONE);
            preferences.setLocationAccess(1);
            getLocationAndSendRequest("get");
        }
    }

    //abrir os settings da app para mudar permissão de localização exteriormente
    public void openAppSettings() {
        Uri packageUri = Uri.fromParts( "package", getApplicationContext().getPackageName(), null );

        Intent applicationDetailsSettingsIntent = new Intent();

        applicationDetailsSettingsIntent.setAction( Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        applicationDetailsSettingsIntent.setData( packageUri );
        applicationDetailsSettingsIntent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );

        getApplicationContext().startActivity( applicationDetailsSettingsIntent );

    }


    public void showCreateQueueDialog(View v) {
        Dialog newQueueForm = new Dialog(this);
        newQueueForm.setContentView(R.layout.form_new_queue);
        TextView queueName = newQueueForm.findViewById(R.id.queue_name);
        TextView queuePwd = newQueueForm.findViewById(R.id.queue_pwd);
        ImageButton closeButton = (ImageButton) newQueueForm.findViewById(R.id.close_button);
        Button createQueueButton = (Button) newQueueForm.findViewById(R.id.create_queue);
        NumberPicker np = newQueueForm.findViewById(R.id.number_picker);
        NumberPicker tp = newQueueForm.findViewById(R.id.time_picker);
        np.setMinValue(1);
        np.setMaxValue(999);
        np.setValue(50);
        tp.setMinValue(1);
        tp.setMaxValue(24);
        tp.setValue(5);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newQueueForm.dismiss();
            }
        });

        createQueueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                maxTracks = np.getValue();
                maxTime = tp.getValue();
                newQueueName = queueName.getText().toString();
                newQueuePwd = queuePwd.getText().toString();

                if(newQueueName.isEmpty() || newQueuePwd.isEmpty())
                    Toast.makeText(HomeActivity.this, R.string.empty_user_pwd, Toast.LENGTH_SHORT).show();
                else if(preferences.getLocationAccess() == 1)
                    getLocationAndSendRequest("post");
                else
                    sendCreateQueueRequest(newQueueName, newQueuePwd, maxTracks, maxTime);
            }
        });
        newQueueForm.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        newQueueForm.show();
    }

    public void showJoinQueueDialog(View v) {
        Dialog joinQueueForm = new Dialog(this);
        joinQueueForm.setContentView(R.layout.form_join_queue);
        TextView queuePwd = joinQueueForm.findViewById(R.id.queue_pwd);
        TextView queueName = joinQueueForm.findViewById(R.id.queue_name);
        queueName.setText(selectedQueue);
        ImageButton closeButton = (ImageButton) joinQueueForm.findViewById(R.id.close_button);
        Button joinQueueButton = (Button) joinQueueForm.findViewById(R.id.join_queue);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                joinQueueForm.dismiss();
            }
        });

        joinQueueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password = queuePwd.getText().toString();
                if(!password.isEmpty())
                    sendJoinRequest(selectedQueue, password, "guest", false);
                else
                    Toast.makeText(HomeActivity.this, R.string.empty_pwd, Toast.LENGTH_SHORT).show();
            }
        });

        joinQueueForm.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        joinQueueForm.show();
    }

    public void showLoadingDialog(){
        loadingDialog.setContentView(R.layout.loading_popup);
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        loadingDialog.show();
    }

    public void showConfirmDialog(String s){
        Dialog banned = new Dialog(this);
        banned.setContentView(R.layout.form_confirm_action);
        TextView t = banned.findViewById(R.id.form_title);
        t.setText(s);
        Button b = banned.findViewById(R.id.submit_button);
        b.setText(R.string.close);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                banned.dismiss();
            }
        });
        banned.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        banned.show();
    }

    private ArrayList<String> getNearQueuesFromJSONString(String result){
        ArrayList<String> queueNames = new ArrayList<>();
        try{
            JSONArray queues = new JSONArray(result);
            for(int i = 0; i < queues.length(); i++){
                final JSONObject item = queues.getJSONObject(i);
                String name = item.getString("Nome");
                queueNames.add(name);
            }
        }
        catch (Exception e){e.printStackTrace();}
        return queueNames;
    }

    //pede a localização do dispositivo e envia o pedido cuja referencia é passada como argumento
    private void getLocationAndSendRequest(String req){
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                if(req.equals("get"))
                    sendGetNearQueuesRequest(location.getLatitude(), location.getLongitude(), 20);
                else if (req.equals("post"))
                    sendCreateQueueRequest(newQueueName, newQueuePwd, location.getLatitude(), location.getLongitude(),
                maxTracks, maxTime);
            }
            else{
                findViewById(R.id.loading_circle).setVisibility(View.GONE);
                emptyText.setText(R.string.gps_error);
                emptyText.setVisibility(View.VISIBLE);
            }
        });
    }

    //pede ao servidor a lista de queues por proximidade e atualiza a ListView de queues
    private void sendGetNearQueuesRequest(double coordX, double coordY, int limit){
        emptyText.setVisibility(View.GONE);
        queuesListView.setVisibility(View.GONE);
        findViewById(R.id.loading_circle).setVisibility(View.VISIBLE);
        String url = "https://ubiq.azurewebsites.net/api/Sala/Procurar/Localizacao/?Xcoord="
                + coordX + "&Ycoord=" + coordY + "&NumeroSalas=" + limit;

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        queues = getNearQueuesFromJSONString(response);
                        findViewById(R.id.loading_circle).setVisibility(View.GONE);
                        if(queues.size() == 0){
                            emptyText.setText(R.string.no_queues_near);
                            emptyText.setVisibility(View.VISIBLE);
                        }
                        else {
                            queuesListView.setVisibility(View.VISIBLE);
                            HomeActivity.SearchAdapter adapter = new HomeActivity.SearchAdapter(getApplicationContext(), queues);
                            queuesListView.setAdapter(adapter);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String s = "Unknown error ocurred";
                if(error instanceof NoConnectionError){
                    s = getString(R.string.no_connection_err);
                }
                else if (error instanceof TimeoutError) {
                    sendGetNearQueuesRequest(coordX,  coordY, limit);
                }
                else if (error instanceof AuthFailureError) {
                    s = getString(R.string.auth_failure_err);
                } else if (error instanceof ServerError) {
                    s = new ServerErrorHandler().getErrorString(error);
                }
                if(!(error instanceof TimeoutError))
                    Toast.makeText(HomeActivity.this, s, Toast.LENGTH_SHORT).show();
                findViewById(R.id.loading_circle).setVisibility(View.GONE);
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

    //pede ao servidor a lista de queues que correspondem ao nome passado como argumento
    // e atualiza a ListView de queues com o resultado
    private void sendGetQueuesByNameRequest(String name){
        queuesListView.setVisibility(View.GONE);
        findViewById(R.id.loading_circle).setVisibility(View.VISIBLE);
        String url = "https://ubiq.azurewebsites.net/api/Sala/Procurar?Nome=" + name;

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        queues = new HttpResponseManager().responseToStringList(response);
                        findViewById(R.id.loading_circle).setVisibility(View.GONE);
                        if(queues.size() == 0){
                            emptyText.setText(R.string.empty_search);
                            emptyText.setVisibility(View.VISIBLE);
                        }
                        else {
                            queuesListView.setVisibility(View.VISIBLE);
                            HomeActivity.SearchAdapter adapter = new HomeActivity.SearchAdapter(getApplicationContext(), queues);
                            queuesListView.setAdapter(adapter);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String s = "Unknown error ocurred";
                if(error instanceof NoConnectionError){
                    s = getString(R.string.no_connection_err);
                }
                else if (error instanceof TimeoutError) {
                    sendGetQueuesByNameRequest(name);
                }
                else if (error instanceof AuthFailureError) {
                    s = getString(R.string.auth_failure_err);
                } else if (error instanceof ServerError) {
                    s = new ServerErrorHandler().getErrorString(error);
                }
                if(!(error instanceof TimeoutError))
                    Toast.makeText(HomeActivity.this, s, Toast.LENGTH_SHORT).show();
                findViewById(R.id.loading_circle).setVisibility(View.GONE);
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

    //pedido ao servidor para entrar numa queue
    //se tiver sucesso passa para a MainActivity
    public void sendJoinRequest(String name, String password, String userType, Boolean loading){
        if(!loading)
            showLoadingDialog();
        String url = "https://ubiq.azurewebsites.net/api/Sala/Entrar";

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println(response);
                        System.out.println(name);
                        int queueId = Integer.parseInt(response);
                            preferences.setQueueName(name);
                            preferences.setQueueId(queueId);
                            preferences.setUserType(userType);
                            Intent startIntent = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(startIntent);
                            finish();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String s = "Unknown error ocurred";
                if(error instanceof NoConnectionError){
                    s = getString(R.string.no_connection_err);
                }
                else if (error instanceof TimeoutError) {
                    sendJoinRequest(name, password,userType,false);
                }
                else if (error instanceof AuthFailureError) {
                    s = getString(R.string.auth_failure_err);
                } else if (error instanceof ServerError) {
                    s = new ServerErrorHandler().getErrorString(error);
                }
                if(!(error instanceof TimeoutError))
                    Toast.makeText(HomeActivity.this, s, Toast.LENGTH_SHORT).show();
                findViewById(R.id.loading_circle).setVisibility(View.GONE);
                System.out.println(error.toString());
                loadingDialog.dismiss();
            }
        })
        {
            @Override
            public Map getHeaders() throws AuthFailureError {
                HashMap headers = new HashMap();
                headers.put("Authorization", "Bearer " + apiToken);
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
            @Override
            public Map getParams() throws AuthFailureError{
                HashMap params = new HashMap();
                params.put("Nome", name);
                params.put("Password", password);
                return params;
            }
        };
        queue.add(stringRequest);
    }

    //pedido ao servidor para criar uma nova queue com os parametros localizaçao, tempo maximo e maximo de faixas
    //se tiver sucesso efetua um pedido ao servidor para entrar na queue
    public void sendCreateQueueRequest(String name, String password, double coordX, double coordY, int maxT, int maxH){
        showLoadingDialog();
        String url = "https://ubiq.azurewebsites.net/api/Sala/Criar";
        String requestBody;

        try {
            JSONObject jsonObject2 = new JSONObject();
            jsonObject2.put("Nome", name);
            jsonObject2.put("Password", password);
            jsonObject2.put("XCoord", coordX);
            jsonObject2.put("YCoord", coordY);
            jsonObject2.put("LimiteMusicas", maxT);
            jsonObject2.put("LimiteHorario", maxH);
            requestBody = jsonObject2.toString();

            RequestQueue queue = Volley.newRequestQueue(this);

            StringRequest stringRequest = new StringRequest(1, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    System.out.println(response);
                    sendJoinRequest(name, password, "host", true);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    String s = "Unknown error ocurred";
                    if(error instanceof NoConnectionError){
                        s = getString(R.string.no_connection_err);
                    }
                    else if (error instanceof TimeoutError) {
                        sendCreateQueueRequest(name, password, coordX, coordY,maxT, maxH);
                    }
                    else if (error instanceof AuthFailureError) {
                        s = getString(R.string.auth_failure_err);
                    } else if (error instanceof ServerError) {
                        s = new ServerErrorHandler().getErrorString(error);
                    }
                    if(!(error instanceof TimeoutError))
                        Toast.makeText(HomeActivity.this, s, Toast.LENGTH_SHORT).show();
                    findViewById(R.id.loading_circle).setVisibility(View.GONE);
                    System.out.println(error.toString());
                    loadingDialog.dismiss();
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

    //pedido ao servidor para criar uma nova queue sem os parametros de localizaçao
    //se tiver sucesso efetua um pedido ao servidor para entrar na queue
    public void sendCreateQueueRequest(String name, String password, int maxT, int maxH){
        showLoadingDialog();
        String url = "https://ubiq.azurewebsites.net/api/Sala/Criar";
        String requestBody;

        try {
            JSONObject obj = new JSONObject();
            obj.put("Nome", name);
            obj.put("Password", password);
            obj.put("LimiteMusicas", maxH);
            obj.put("LimiteHorario", maxT);
            requestBody = obj.toString();

            RequestQueue queue = Volley.newRequestQueue(this);

            StringRequest stringRequest = new StringRequest(1, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    sendJoinRequest(name, password, "host", true);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    String s = "Unknown error ocurred";
                    if(error instanceof NoConnectionError){
                        s = getString(R.string.no_connection_err);
                    }
                    else if (error instanceof TimeoutError) {
                        sendCreateQueueRequest(name, password, maxT, maxH);
                    }
                    else if (error instanceof AuthFailureError) {
                        s = getString(R.string.auth_failure_err);
                    } else if (error instanceof ServerError) {
                        s = new ServerErrorHandler().getErrorString(error);
                    }
                    if(!(error instanceof TimeoutError))
                        Toast.makeText(HomeActivity.this, s, Toast.LENGTH_SHORT).show();
                    findViewById(R.id.loading_circle).setVisibility(View.GONE);
                    System.out.println(error.toString());
                    loadingDialog.dismiss();
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

    //pedido para efetuar logout
    //se tiver sucesso elimina os dados guardados em SharedPreferences e passa para a RegisterActivity
    private void sendLogoutRequest(){
        String url = "https://ubiq.azurewebsites.net/api/Account/Logout";

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        preferences.clearSession();
                        startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
                        finish();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String s = "Unknown error ocurred";
                if(error instanceof NoConnectionError){
                    s = getString(R.string.no_connection_err);
                }
                else if (error instanceof TimeoutError) {
                    sendLogoutRequest();
                }
                else if (error instanceof AuthFailureError) {
                    s = getString(R.string.auth_failure_err);
                } else if (error instanceof ServerError) {
                    s = new ServerErrorHandler().getErrorString(error);
                }
                if(!(error instanceof TimeoutError))
                    Toast.makeText(HomeActivity.this, s, Toast.LENGTH_SHORT).show();
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

    //Adaptador para a ListView que mostra as queues de uma lista de queues
    class SearchAdapter extends ArrayAdapter<String> {
        Context context;
        ArrayList<String> queues;

        SearchAdapter(Context c, ArrayList<String> queues){
            super(c, R.layout.users_row, R.id.user_id, queues);
            this.context = c;
            this.queues = queues;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
            LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = layoutInflater.inflate(R.layout.users_row, parent, false);
            TextView queueName = row.findViewById(R.id.user_id);

            queueName.setText(queues.get(position));

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedQueue = queues.get(position);
                    showJoinQueueDialog(v);
                }
            });
            return row;
        }
    }
}