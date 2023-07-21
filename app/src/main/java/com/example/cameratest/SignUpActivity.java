package com.example.cameratest;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

public class SignUpActivity extends AppCompatActivity {

    public EditText e_id,e_pw, e_pw2, e_name;

    public Button signUpBtn;

    boolean pw_check;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        e_id = findViewById(R.id.sign_up_id);
        e_pw = findViewById(R.id.sign_up_pw1);
        e_pw2 = findViewById(R.id.sign_up_pw2);
        e_name = findViewById(R.id.sign_up_name);
        signUpBtn = findViewById(R.id.sign_up_btn);
        pw_check = false;


        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!pw_check){
                    Toast.makeText(getApplicationContext(), "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                }
                else {
                    if(signUp()){
                        Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
                        startActivity(intent);
                        finish();
                    }

                }
            }
        });

        e_pw.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (e_pw.getText().toString().equals(e_pw2.getText().toString())) {
                    pw_check = true;
                }
                else {
                    pw_check = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        e_pw2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (e_pw2.getText().toString().equals(e_pw.getText().toString())) {
                    pw_check = true;
                }
                else {
                    pw_check = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }


    boolean signUp(){
        String name = e_name.getText().toString();
        String id = e_id.getText().toString();
        String pw = e_pw.getText().toString();
        try {
            saveData saveData = new saveData();
            String result = saveData.execute(name, id, pw).get();
            Log.d("리턴값", result);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    class saveData extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            try {
                String url = "http://192.168.35.58:8081/signUp";

                URL serverUrl = new URL(url);
                String postData = "name= " + URLEncoder.encode(strings[0], "UTF-8")
                        + "&id=" + URLEncoder.encode(strings[1], "UTF-8")
                        + "&pw=" + URLEncoder.encode(strings[2], "UTF-8");
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

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Toast.makeText(getApplicationContext(), "회원가입에 성공했습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "회원가입에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
