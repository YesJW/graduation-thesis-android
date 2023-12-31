package com.example.graduationthesis;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.example.graduationthesis.R;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;


public class ScanActivity extends AppCompatActivity {

    String id;

    CheckLecture check_lecture;
    List<String> lecture_list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        lecture_list = new ArrayList<>();
        Intent intent = getIntent();
        id = intent.getStringExtra("userId");
        Log.d("get UserID", id);
        startScan();

    }

    @SuppressLint("CheckResult")
    private void startScan(){
        ScanOptions scanOptions = new ScanOptions();
        scanOptions.setOrientationLocked(false);
        scanOptions.setPrompt("화면에 QR코드를 스캔해주세요");
        check_lecture = new CheckLecture();
        check_lecture.checkLecture(id)
                        .subscribe(strings -> {
                            lecture_list = strings;
                            barcodeLauncher.launch(scanOptions);
                            Log.d("lectureList", lecture_list.toString());

                        }, throwable -> {
                            Toast.makeText(ScanActivity.this, "서버에 접속을 실패했습니다.", Toast.LENGTH_LONG).show();
                        });

    }
private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
        result -> {
            if(result.getContents() == null) {
                Toast.makeText(ScanActivity.this, "취소되었습니다.", Toast.LENGTH_LONG).show();
            } else {
                //인식 후 처리
                String getContent = result.getContents();
                Log.d("scanQR",getContent);
                String [] lectureSplit = getContent.split(";");

                boolean check_lecture = lecture_list.contains(lectureSplit[0]);
                // ###############검증 시간 처리 30000 -> 30초
                if(check_lecture && (System.currentTimeMillis() - Long.parseLong(lectureSplit[1])) <= 15000) {
                    Intent intent = new Intent(ScanActivity.this, MainActivity.class);
                    intent.putExtra("userId", id);
                    intent.putExtra("lectureCode",lectureSplit[0]);
                    startActivity(intent);
                    finish();
                }
                else {
                    Toast.makeText(ScanActivity.this, "qr코드가 맞지 않습니다.", Toast.LENGTH_LONG).show();
                    startScan();
                }

            }
        });
    public class CheckLecture {
        public Observable<List<String>> checkLecture(String id) {
            return Observable.fromCallable(() -> {
                        List<String> stringList = new ArrayList<>();
                        try {
                            String url = "http://10.0.2.2:8081/qrCheck";
                            URL serverUrl = new URL(url);

                            String postData = "id=" + URLEncoder.encode(id, "UTF-8");

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
                            JSONArray jsonArray = new JSONArray(response.toString());
                            for (int i = 0; i < jsonArray.length(); i++) {
                                stringList.add(jsonArray.getString(i));
                            }
                            return stringList;
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw e;
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
        }

    }


}