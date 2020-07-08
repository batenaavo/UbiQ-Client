package com.example.ubiq;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//Fragmento da MainActivity onde se podem alterar as definições da queue.
// Exclusivo para hosts.

public class SettingsFragment extends Fragment {

    ListView settingsListView;
    ArrayList<String> settings;
    ArrayList<String> bannedUsers;
    HttpResponseManager responseManager;
    int queueId;
    String apiToken;
    private int selectedOption;
    private Boolean canceled;

    public SettingsFragment(int queueId, String apiToken){
        this.queueId = queueId;
        this.apiToken = apiToken;
        responseManager = new HttpResponseManager();
        settings = new ArrayList<>();
        bannedUsers = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        selectedOption = 0;
        canceled = false;
        this.settingsListView = (ListView) getView().findViewById(R.id.settings_list_view);
        getActivity().setTitle("Settings");
        ((MainActivity) getActivity()).getToolbar().setNavigationIcon(null);
        if(settings.size() == 0){
            settings.add("Set Genre Filters");
            settings.add("Change Queue Name");
            settings.add("Change Queue Password");
            settings.add("Banned Users");
            settings.add("Delete Queue");
        }
        sendGetBannedUsersRequest();
        SettingsAdapter adapter = new SettingsAdapter((getActivity().getApplicationContext()), settings);
        settingsListView.setAdapter(adapter);
    }

    public void resetSettingsAdapter(){
        SettingsAdapter adapter = new SettingsAdapter((getActivity().getApplicationContext()), settings);
        settingsListView.setAdapter(adapter);
    }

    public void cancelRequests(){
        canceled = true;
    }

    private void showDeleteQueueDialog(View v) {
        Dialog deleteQueueForm = new Dialog(getActivity());
        deleteQueueForm.setContentView(R.layout.form_confirm_action);
        TextView title = deleteQueueForm.findViewById(R.id.form_title);
        Button deleteQueueButton = (Button) deleteQueueForm.findViewById(R.id.submit_button);

        title.setText(R.string.delete_confirm);

        deleteQueueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDeleteQueueRequest();
            }
        });

        deleteQueueForm.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        deleteQueueForm.show();
    }

    //Abre um pop-up com a lista de uitlizadores banidos com opção de os readmitir
    public void showBannedUsersDialog(View v) {
        Dialog bannedUsersForm = new Dialog(getActivity());
        bannedUsersForm.setContentView(R.layout.form_simple_list);
        ListView bannedUsersListView = bannedUsersForm.findViewById(R.id.list_view);

        BannedUsersAdapter adapter = new BannedUsersAdapter((getActivity().getApplicationContext()), bannedUsers);
        bannedUsersListView.setAdapter(adapter);

        bannedUsersForm.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        bannedUsersForm.show();
    }

    //Pedido para obter lista de utilizadores banidos da queue
    private void sendGetBannedUsersRequest(){
        String url = "https://ubiq.azurewebsites.net/api/Sala/Utilizadores/Banidos?SalaId=" + queueId;

        RequestQueue queue = Volley.newRequestQueue(getActivity());

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(!canceled) {
                            bannedUsers = new HttpResponseManager().responseToStringList(response);
                            resetSettingsAdapter();
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
                    sendGetBannedUsersRequest();
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
                headers.put("Authorization", "Bearer " + apiToken);
                return headers;
            }
        };
        //Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    //Pedido para mudar o nome da queue
    private void sendChangeQueueNameRequest(String newName){
        String url = "https://ubiq.azurewebsites.net/api/Sala/Nome/Alterar";
        String requestBody;

        try {
            JSONObject obj = new JSONObject();
            obj.put("SalaId", queueId);
            obj.put("Nome", newName);
            requestBody = obj.toString();

            RequestQueue queue = Volley.newRequestQueue(getActivity());

            StringRequest stringRequest = new StringRequest(1, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    if(!canceled) {
                        Toast.makeText(getActivity(), R.string.change_name_success, Toast.LENGTH_SHORT).show();
                        ((MainActivity) getActivity()).setQueueName(newName);
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
                        sendChangeQueueNameRequest(newName);
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

    //pedido para mudar a password da queue
    private void sendChangeQueuePwdRequest(String newPwd){
        String url = "https://ubiq.azurewebsites.net/api/Sala/Password/Alterar";
        String requestBody;

        try {
            JSONObject obj = new JSONObject();
            obj.put("SalaId", queueId);
            obj.put("Password", newPwd);
            requestBody = obj.toString();

            RequestQueue queue = Volley.newRequestQueue(getActivity());

            StringRequest stringRequest = new StringRequest(1, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    if(!canceled) {
                        Toast.makeText(getActivity(), R.string.change_pwd_success, Toast.LENGTH_SHORT).show();
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
                        sendChangeQueuePwdRequest(newPwd);
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

    //pedido para eliminar uma queue
    private void sendDeleteQueueRequest(){
        String url = "https://ubiq.azurewebsites.net/api/Sala/Eliminar?SalaId=" + this.queueId;

        RequestQueue queue = Volley.newRequestQueue(getActivity());

        StringRequest stringRequest = new StringRequest(Request.Method.DELETE, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(!canceled) {
                            ((MainActivity) getActivity()).deleteQueue();
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
                    sendDeleteQueueRequest();
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

    //pedido para readmitir um utilizador banido
   private void sendReadmitUserRequest(String user, ImageButton add, ImageView check){
        String url = "https://ubiq.azurewebsites.net/api/Sala/Utilizadores/Readmitir";
        String requestBody;

        try {
            JSONObject obj = new JSONObject();
            obj.put("SalaId", queueId);
            obj.put("Username", user);
            requestBody = obj.toString();

            RequestQueue queue = Volley.newRequestQueue(getActivity());

            StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    add.setVisibility(View.GONE);
                    check.setVisibility(View.VISIBLE);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    String s = getString(R.string.unknown_err);
                    if(error instanceof NoConnectionError){
                        s = getString(R.string.no_connection_err);
                    }
                    else if (error instanceof TimeoutError) {
                        sendReadmitUserRequest(user, add, check);
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

    //Adaptador da ListView das opções das definições
    class SettingsAdapter extends ArrayAdapter<String> {

        Context context;
        ArrayList<String> settings;

        SettingsAdapter(Context c, ArrayList<String> settings) {
            super(c, R.layout.settings_row, settings);
            this.context = c;
            this.settings = settings;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = layoutInflater.inflate(R.layout.settings_row, parent, false);
            TextView name = row.findViewById(R.id.name_view);
            LinearLayout submitForm = row.findViewById(R.id.submit_form);
            EditText editText = row.findViewById(R.id.edit_text);
            ImageButton confirm = row.findViewById(R.id.confirm_button);
            name.setText(settings.get(position));
            if(bannedUsers.size() == 0 && position == 3)
                name.setTextColor(ContextCompat.getColor(context, R.color.common_google_signin_btn_text_light_disabled));

            switch(selectedOption){
                case 1:
                    if(position == 1){
                       name.setVisibility(View.GONE);
                       submitForm.setVisibility(View.VISIBLE);
                       editText.setHint("New Name");
                       confirm.setOnClickListener(new View.OnClickListener() {
                           @Override
                           public void onClick(View v) {
                               sendChangeQueueNameRequest(editText.getText().toString());
                               name.setVisibility(View.VISIBLE);
                               submitForm.setVisibility(View.GONE);
                               selectedOption = 0;
                           }
                       });
                    }
                    break;
                case 2:
                    if(position == 2) {
                        name.setVisibility(View.GONE);
                        submitForm.setVisibility(View.VISIBLE);
                        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        editText.setHint("New Password");
                        confirm.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                sendChangeQueuePwdRequest(editText.getText().toString());
                                name.setVisibility(View.VISIBLE);
                                submitForm.setVisibility(View.GONE);
                                selectedOption = 0;
                            }
                        });
                    }
                    break;
                case 0:
                    break;
            }

                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (selectedOption != 0) {
                            selectedOption = 0;
                            resetSettingsAdapter();
                        } else {
                            switch (settings.get(position)) {
                                case "Set Genre Filters":
                                    ((MainActivity) getActivity()).startFiltersFragment();
                                    break;
                                case "Delete Queue":
                                    showDeleteQueueDialog(row);
                                    break;
                                case "Change Queue Name":
                                    selectedOption = 1;
                                    resetSettingsAdapter();
                                    break;
                                case "Change Queue Password":
                                    selectedOption = 2;
                                    resetSettingsAdapter();
                                    break;
                                case "Banned Users":
                                    if(bannedUsers.size() > 0)
                                        showBannedUsersDialog(row);
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                });
            return row;
        }
    }

    //Adaptador da ListView de utilizadores banidos
    class BannedUsersAdapter extends ArrayAdapter<String> {
        Context context;
        ArrayList<String> users;

        BannedUsersAdapter(Context c, ArrayList<String> users) {
            super(c, R.layout.add_element_row, users);
            this.context = c;
            this.users = users;
        }
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = layoutInflater.inflate(R.layout.add_element_row, parent, false);
            TextView name = row.findViewById(R.id.name_view);
            ImageButton addButton = (ImageButton) row.findViewById(R.id.add_button);
            ImageView check = (ImageView) row.findViewById(R.id.checked_image);
            name.setText(users.get(position));

            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendReadmitUserRequest(users.get(position), addButton, check);
                }
            });
            return row;
        }
    }
}