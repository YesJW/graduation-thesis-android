package com.example.cameratest;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class SignInActivity extends AppCompatActivity {

    EditText edit_id, edit_pw;
    Button login_btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        edit_id = findViewById(R.id.login_id);
        edit_pw = findViewById(R.id.login_pw);

        login_btn = findViewById(R.id.login_btn);

        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    void login(){
        String id = edit_id.getText().toString();
        String pw = edit_pw.getText().toString();


        try {
            loadData loadData = new loadData();
            String result = loadData.execute(id, pw).get();
            Log.d("리턴값", result);
            if (result.equals("1")) {
                Log.d("리턴값2","성공");
                Intent intent = new Intent(SignInActivity.this, ScanActivity.class);
                intent.putExtra("userId", id);
                startActivity(intent);
                finish();

            }
        } catch (Exception e) {

        }
    }

    class loadData extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            try {
                String url = "http://192.168.35.58:8081/signIn";
                URL serverUrl = new URL(url);

                String postData = "id=" + URLEncoder.encode(strings[0], "UTF-8")
                        + "&pw=" + URLEncoder.encode(strings[1], "UTF-8");
                Log.d("id", URLEncoder.encode(strings[0]));

                HttpURLConnection connection = (HttpURLConnection) serverUrl.openConnection();
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(postData.getBytes());
                outputStream.flush();
                outputStream.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                if(connection.getResponseCode() == connection.HTTP_OK) {
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                }
                else {
                    Log.d("통신 실패", connection.getResponseCode()+"에러");
                }
                return response.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

    }
}
