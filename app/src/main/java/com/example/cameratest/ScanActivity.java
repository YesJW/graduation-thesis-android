package com.example.cameratest;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
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
import java.util.concurrent.ExecutionException;


public class ScanActivity extends AppCompatActivity {

    String id;

    CheckLecture check_lecture;
    List<String> lecture_lsit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        Intent intent = getIntent();
        id = intent.getStringExtra("userId");
        Log.d("get UserID", id);
        check_lecture = new CheckLecture();
        startScan();

    }

    private void startScan(){
        try {
            lecture_lsit = check_lecture.execute().get();

        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        barcodeLauncher.launch(new ScanOptions());
    }
private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
        result -> {
            if(result.getContents() == null) {
                Toast.makeText(ScanActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                //인식 후 처리
                Toast.makeText(ScanActivity.this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                String getContent = result.getContents();
                Log.d("scanQR",getContent);
                String [] lectureSplit = getContent.split(";");

                boolean check_lecture = lecture_lsit.contains(lectureSplit[0]);
                // ###############검증 시간 처리 30000 -> 30초
                if(check_lecture && (System.currentTimeMillis() - Long.parseLong(lectureSplit[1])) <= 15000) {
                    Intent intent = new Intent(ScanActivity.this, MainActivity.class);
                    intent.putExtra("userId", id);
                    startActivity(intent);
                    finish();
                }
                else {
                    Toast.makeText(ScanActivity.this, "qr코드가 맞지 않습니다.", Toast.LENGTH_LONG).show();
                }

            }
        });
    class CheckLecture extends AsyncTask<Void, Void, List<String>> {
        @Override
        protected List<String> doInBackground(Void... voids) {
            List<String> stringList = new ArrayList<>();
            try {
                String url = "http://192.168.35.221:8081/qrCheck";
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
                if(connection.getResponseCode() == connection.HTTP_OK) {
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                }
                else {
                    Log.d("통신 실패", connection.getResponseCode()+"에러");
                }
                JSONArray jsonArray = new JSONArray(response.toString());
                for (int i = 0; i < jsonArray.length(); i++) {
                    stringList.add(jsonArray.getString(i));
                }
                return stringList;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

    }


}