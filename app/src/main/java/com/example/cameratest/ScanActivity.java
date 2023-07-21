package com.example.cameratest;

import android.content.Intent;
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


public class ScanActivity extends AppCompatActivity {

    String id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        Intent intent = getIntent();
        id = intent.getStringExtra("userId");
        Log.d("get UserID", id);
        startScan();
    }

    private void startScan(){
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

                if(getContent.equals("DB")) {
                    Intent intent = new Intent(ScanActivity.this, MainActivity.class);
                    intent.putExtra("userId", id);
                    startActivity(intent);
                    finish();
                }
                else {
                    Toast.makeText(ScanActivity.this, "qr코드가 맞지 않습니다.", Toast.LENGTH_LONG).show();
                    startScan();
                }

            }
        });


}