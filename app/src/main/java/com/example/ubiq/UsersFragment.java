package com.example.ubiq;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
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

public class UsersFragment extends Fragment {
    ListView usersListView;
    int queueId;
    String apiToken;
    String userType;
    private TextView userCountText;
    private Boolean canceled;

    public UsersFragment(int queueId, String apiToken, String userType){
        this.queueId = queueId;
        this.apiToken = apiToken;
        this.userType = userType;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_users, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        canceled = false;
        this.usersListView = (ListView) getView().findViewById(R.id.usersListView);
        this.userCountText = getView().findViewById(R.id.user_count_text);
        getActivity().setTitle("Connected Users");
        userCountText.setText(((MainActivity) getActivity()).getConnectedUsersCount() + " connected users");
        ((MainActivity) getActivity()).getToolbar().setNavigationIcon(null);
        resetAdapter();
    }

    public void cancelRequests(){
        canceled = true;
    }

    public void resetAdapter() {
        ArrayList<String> users = ((MainActivity) getActivity()).getConnectedUsers();
        userCountText.setText(((MainActivity) getActivity()).getConnectedUsersCount() + " connected users");
        if(users.size() > 0){
            getView().findViewById(R.id.empty_users).setVisibility(View.GONE);
        }
        else{
            getView().findViewById(R.id.empty_users).setVisibility(View.VISIBLE);
        }

        UserAdapter adapter = new UserAdapter((getActivity().getApplicationContext()), users);
        usersListView.setAdapter(adapter);
    }

    private void showBanConfirmForm(View v, String user) {
        Dialog confirmBanForm = new Dialog(getActivity());
        confirmBanForm.setContentView(R.layout.form_confirm_action);
        TextView title = confirmBanForm.findViewById(R.id.form_title);
        Button deleteQueueButton = (Button) confirmBanForm.findViewById(R.id.submit_button);
        String banText = "Do you want to ban " + user + "? You can readmit users you have banned at any time";
        title.setText(banText);

        deleteQueueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBanUserRequest(user);
                confirmBanForm.dismiss();
            }
        });

        confirmBanForm.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        confirmBanForm.show();
    }

    private void sendBanUserRequest(String user){
        String url = "https://ubiq.azurewebsites.net/api/Sala/Utilizadores/Banir";
        String requestBody;

        try {
            JSONObject obj = new JSONObject();
            obj.put("SalaId", queueId);
            obj.put("Username", user);
            requestBody = obj.toString();

            RequestQueue queue = Volley.newRequestQueue(getActivity());

            StringRequest stringRequest = new StringRequest(1, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    if(!canceled) {
                        resetAdapter();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    System.out.println(error);
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


    class UserAdapter extends ArrayAdapter<String> {

        Context context;
        ArrayList<String> users;

        UserAdapter(Context c, ArrayList<String> users){
            super(c, R.layout.users_row, R.id.user_id, users);
            this.context = c;
            this.users = users;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
            LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = layoutInflater.inflate(R.layout.users_row, parent, false);
            TextView userId = row.findViewById(R.id.user_id);
            ImageButton banButton = row.findViewById(R.id.ban_user_button);
            userId.setText(users.get(position));

            if(userType.equals("host")){
               banButton.setVisibility(View.VISIBLE);

               banButton.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View v) {
                       showBanConfirmForm(row, users.get(position));
                   }
               });
            }

            return row;
        }
    }
}