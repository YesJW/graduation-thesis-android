package com.example.cameratest;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;

import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.cameratest.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding viewBinding;
    private ExecutorService cameraExecutor;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private CameraSelector cameraSelector;
    private ImageCapture imageCapture;
    private boolean check;


    /*@Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        switch (newConfig.orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                //ㅁㄴㅇ
                break;

            case Configuration.ORIENTATION_LANDSCAPE:
                //ㅁㄴㅇ
                break;
        }
    }*/
    String id;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        id = intent.getStringExtra("userId");
        viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        cameraExecutor = Executors.newSingleThreadExecutor();
        check = false;
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewBinding.viewFinder.getSurfaceProvider());

                // ImageAnalysis
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        //.setTargetResolution(new Size(480, 360)) // 조정할 해상도로 변경
                        .build();

                imageCapture = new ImageCapture.Builder().build();

                imageAnalysis.setAnalyzer(cameraExecutor, new YourAnalyzer());

                // Select back camera as a default
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Unbind all use cases before binding
                cameraProvider.unbindAll();

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};
    private static final int REQUEST_CODE_PERMISSIONS = 10;

    private class YourAnalyzer implements ImageAnalysis.Analyzer {

        private FaceDetectorOptions faceDetectorOptions;
        public YourAnalyzer() {
            // Configure face detection options
            faceDetectorOptions = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .setMinFaceSize(1.0f)
                    .build();
        }

        @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                FaceDetector detector = FaceDetection.getClient(faceDetectorOptions);
                detector.process(inputImage)
                        .addOnSuccessListener(faces -> {
                            if (!faces.isEmpty()) {
                                // 얼굴이 검출되었을 때의 처리를 수행합니다.
                                for (Face face : faces) {
                                    FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
                                    FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
                                    if (leftEye != null && rightEye != null) {
                                        Log.d("facedetection", "눈이 검출되었습니다.  " + leftEye.getPosition());
                                        if(face.getRightEyeOpenProbability() > 0.1 || face.getLeftEyeOpenProbability() > 0.1){
                                            Log.d("facedetection", "눈 뜸");
                                        }
                                        else {
                                            Log.d("facedetection", "눈 감음 왼쪽 : " + face.getLeftEyeOpenProbability() + "오른쪽 : "+ face.getRightEyeOpenProbability());
                                            if (check == false) {
                                                check = true;
                                                captureAndUpload();
                                            }


                                        }
                                    }
                                    else {
                                        Log.d("facedetection", "얼굴이 검출되지 않았습니다..");

                                    }
                                }
                            } else {
                                // 얼굴이 검출되지 않았을 때의 처리를 수행합니다.
                                Log.d("facedetection", "얼굴이 검출되지 않았습니다.");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // 얼굴 검출 실패 시 예외 처리를 수행합니다.
                                e.printStackTrace();
                            }
                        })
                        .addOnCompleteListener(new OnCompleteListener<List<Face>>() {
                            @Override
                            public void onComplete(@NonNull Task<List<Face>> task) {
                                imageProxy.close();
                            }
                        });
            } else {
                imageProxy.close();
            }
        }

        // 이전 코드 생략
        private int getRotationDegrees(ImageProxy imageProxy) {
            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();

            return rotationDegrees;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    /*public void capture() {
        ImageCapture imageCapture = this.imageCapture;
        if (imageCapture == null) return;

        // Create time stamped name and MediaStore entry.
        String name = new SimpleDateFormat("yy-MM-dd", Locale.US)
                .format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image");
        }

        // Create output options object which contains file + metadata
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
                .build();

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        String msg = "Photo capture succeeded: " + output.getSavedUri();
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                    }
                }
        );
    }*/
    public void captureAndUpload() {
        ImageCapture imageCapture = this.imageCapture;
        if (imageCapture == null) return;

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        // Convert ImageProxy to Bitmap
                        Bitmap bitmap = imageToBitmap(image);

                        // Encode Bitmap to byte array
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
                        byte[] imageBytes = byteArrayOutputStream.toByteArray();

                        // Close the ImageProxy
                        image.close();

                        String fileName = generateUniqueFileName();


                        // Send the image to the server
                        uploadImage(imageBytes, fileName);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        // Handle capture error
                    }
                }
        );
    }

//    private void uploadImage(byte[] imageBytes, String fileName) {
//        AsyncTask<Void, Void, Integer> uploadTask = new AsyncTask<Void, Void, Integer>() {
//            @Override
//            protected Integer doInBackground(Void... voids) {
//                try {
//                    String url1 = "http://192.168.35.58:8081/upload";
//                    URL url = new URL(url1);
//                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                    connection.setDoOutput(true);
//                    connection.setRequestMethod("POST");
//                    connection.setRequestProperty("Content-Type", "image/jpeg");
//                    connection.setRequestProperty("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
//
//
//                    // Write the image data to the request body
//                    OutputStream outputStream = connection.getOutputStream();
//                    outputStream.write(imageBytes);
//                    outputStream.flush();
//                    outputStream.close();
//
//                    int responseCode = connection.getResponseCode();
//                    if (responseCode == HttpURLConnection.HTTP_OK) {
//                        // Image uploaded successfully
//                        // Handle the server response if needed
//                        Log.d("imageUpload","성공");
//                    } else {
//                        Log.d("imageUpload","실패");
//                        // Image upload failed
//                        // Handle the error
//                    }
//
//                    connection.disconnect();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    // Handle the exception
//                }
//                return null;
//            }
//        };
//
//        uploadTask.execute();
//
//    }

    private void uploadImage(byte[] imageBytes, String fileName) {
        AsyncTask<Void, Void, Integer> uploadTask = new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                try {
                    String url1 = "http://192.168.35.58:8081/upload";
                    URL url = new URL(url1);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=*****");
                    connection.setRequestProperty("Connection", "Keep-Alive");
                    connection.setRequestProperty("Cache-Control", "no-cache");

                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(("--*****\r\n" +
                            "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" +
                            "Content-Type: image/jpeg\r\n" +
                            "\r\n").getBytes());
                    outputStream.write(imageBytes);  outputStream.write(("\r\n--*****\r\n" +
                            "Content-Disposition: form-data; name=\"fileName\"\r\n" +
                            "\r\n" + fileName + "\r\n" +
                            "--*****--\r\n").getBytes());
                    outputStream.flush();
                    outputStream.close();

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Image uploaded successfully
                        // Handle the server response if needed
                        Log.d("imageUpload", "성공");
                    } else {
                        Log.d("imageUpload", "실패");
                        // Image upload failed
                        // Handle the error
                    }

                    connection.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                    // Handle the exception
                }
                return null;
            }
        };

        uploadTask.execute();
    }
    private Bitmap imageToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private String generateUniqueFileName() {
        // Generate a unique file name using a timestamp or UUID

        return id + ".jpg";
    }

}
