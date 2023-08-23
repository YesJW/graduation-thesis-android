package com.example.graduationthesis;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.graduationthesis.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

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

    @SuppressLint("CheckResult")
    void login(){
        String id = edit_id.getText().toString();
        String pw = edit_pw.getText().toString();

        LoadData loadData = new LoadData();
        loadData.signIn(id, pw)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    if(response.equals(("1"))){
                        Log.d("리턴값2","성공");
                        Intent intent = new Intent(SignInActivity.this, ScanActivity.class);
                        intent.putExtra("userId", id);
                        startActivity(intent);
                        finish();
                    }

                }, throwable -> {Toast.makeText(SignInActivity.this, "로그인에 실패하였습니다.", Toast.LENGTH_LONG).show();});

    }


    public class LoadData {
        public Observable<String> signIn(String id, String pw) {
            return Observable.fromCallable(() -> {
                        try {
                            String url = "http://10.0.2.2:8081/signIn";
                            URL serverUrl = new URL(url);

                            String postData = "id=" + URLEncoder.encode(id, "UTF-8")
                                    + "&pw=" + URLEncoder.encode(pw, "UTF-8");

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
                            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                while ((line = reader.readLine()) != null) {
                                    response.append(line);
                                }
                                reader.close();
                            } else {
                                Log.d("통신 실패", connection.getResponseCode() + "에러");
                            }
                            return response.toString();
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw e; // rethrow the exception to be caught by RxJava
                        }
                    })

                    .subscribeOn(Schedulers.io());
        }

    }
}
