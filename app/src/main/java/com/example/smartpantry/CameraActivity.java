package com.example.smartpantry;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    private static PreviewView previewView;
    private static ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private static ImageCapture imageCapture;
    private static ImageButton captureBtn;
    private static ProcessCameraProvider camera;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        prepareActivity();
    }

    private void prepareActivity() {
        previewView = findViewById(R.id.previewView);
        captureBtn = findViewById(R.id.captureBtn);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindImageAnalysis(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));

        captureBtn.setOnClickListener(v -> {
            imageCapture.takePicture(executor,
                    new ImageCapture.OnImageCapturedCallback() {
                        @Override
                        public void onCaptureSuccess(@NonNull ImageProxy image) {
                            Log.println(Log.ASSERT,"capture","ok");
                            scanBarcode(image);
                            image.close();
                        }

                        @Override
                        public void onError(ImageCaptureException error) {
                            Log.println(Log.ERROR,"capture","error");
                        }
                    }
            );
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        camera.unbindAll();
    }

    private BarcodeScannerOptions getScanOption() {
         BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_CODABAR,
                                Barcode.FORMAT_CODE_39,
                                Barcode.FORMAT_CODE_93,
                                Barcode.FORMAT_CODE_128,
                                Barcode.FORMAT_EAN_8,
                                Barcode.FORMAT_EAN_13,
                                Barcode.FORMAT_ITF,
                                Barcode.FORMAT_UPC_A,
                                Barcode.FORMAT_UPC_E)
                        .build();
         return options;
    }

    private void scanBarcode(ImageProxy imageProxy) {
        @SuppressLint("UnsafeExperimentalUsageError") Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            BarcodeScanner scanner = BarcodeScanning.getClient(getScanOption());
            Task<List<Barcode>> result = scanner.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> barcodes) {
                        if(!barcodes.isEmpty()) {
                            for (Barcode barcode: barcodes) {
                                String rawValue = barcode.getRawValue();
                                Log.println(Log.DEBUG, "SCAN", "RAW VALUE FOUND " + rawValue);
                                showResult(rawValue);
                            }
                        } else {
                            showToast();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    showToast();
                });
        }
    }

    private void showToast() {
        Toast retryToast = Toast.makeText(
                getApplicationContext(),
                getResources().getString(R.string.retryToastText),
                Toast.LENGTH_SHORT
        );
        retryToast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP,
                0,
                300
        );
        ViewGroup group = (ViewGroup) retryToast.getView();
        TextView messageTextView = (TextView) group.getChildAt(0);
        messageTextView.setTextSize(25);
        retryToast.show();
    }

    private void showResult(String rawValue) {
        LayoutInflater inflater = LayoutInflater.from(this);
        ConstraintLayout popUp = (ConstraintLayout) inflater.inflate(R.layout.scan_result, null, false);
        ConstraintLayout parent = (ConstraintLayout)findViewById(R.id.activity_camera);
        parent.addView(popUp);

        EditText barcode = findViewById(R.id.scanResultCode);
        TextView title = findViewById(R.id.scanResultTitle);
        TextView descr = findViewById(R.id.scanResultDescr);
        Button retryBtn = findViewById(R.id.retryBtn);
        Button confirmBtn = findViewById(R.id.confirmBtn);

        barcode.setText(rawValue);
        title.setText(getResources().getString(R.string.scanResultTitleText));
        descr.setText(getResources().getString(R.string.scanResultDescrText));

        retryBtn.setText(getResources().getString(R.string.retryBtnText));
        confirmBtn.setText(getResources().getString(R.string.confirmBtnText));
        captureBtn.setEnabled(false);

        retryBtn.setOnClickListener(v -> {
            parent.removeView(popUp);
            captureBtn.setEnabled(true);
        });
        confirmBtn.setOnClickListener(v -> {
            String correctBarcode = barcode.getText().toString();
            Intent returnIntent = new Intent();
            returnIntent.putExtra("barcode", correctBarcode);
            setResult(Activity.RESULT_OK,returnIntent);
            finish();
        });

    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> image.close());
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.createSurfaceProvider());
        imageCapture = new ImageCapture.Builder()
                .setTargetRotation(previewView.getDisplay().getRotation())
                .build();
        cameraProvider.bindToLifecycle(
                this, cameraSelector, imageCapture, imageAnalysis, preview);
        camera = cameraProvider;
    }

}
