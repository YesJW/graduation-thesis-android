package com.example.graduationthesis;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ResultActivity extends AppCompatActivity {

    TextView text_time, lecture_title, student_id;
    Date date;
    long mNow;
    SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm");

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        text_time = findViewById(R.id.text_Time);
        lecture_title = findViewById(R.id.lecture_title);
        student_id = findViewById(R.id.student_id);
        Intent intent = getIntent();
        String id = intent.getStringExtra("userId");
        String lectureCode = intent.getStringExtra("lectureCode");
        student_id.setText(student_id.getText().toString() + id);
        mNow = System.currentTimeMillis();
        date = new Date(mNow);
        text_time.setText(text_time.getText().toString() + mFormat.format(date));

        GetLectureTitle getLectureTitle = new GetLectureTitle();
        getLectureTitle.getLectureTitle(lectureCode,id
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    lecture_title.setText(lecture_title.getText().toString() + response);

                }, throwable -> {});

    }

    public class GetLectureTitle {
        public Observable<String> getLectureTitle(String lectureCode, String id) {
            return Observable.fromCallable(() -> {
                        try {
                            String url = "http://10.0.2.2:8081/getLectureTitle";
                            URL serverUrl = new URL(url);

                            String postData = "lectureCode=" + URLEncoder.encode(lectureCode, "UTF-8")
                                    + "&id=" + URLEncoder.encode(id, "UTF-8");
                            Log.d("lectureCode", lectureCode);
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
