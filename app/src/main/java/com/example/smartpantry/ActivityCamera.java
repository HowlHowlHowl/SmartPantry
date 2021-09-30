package com.example.smartpantry;

import android.annotation.SuppressLint;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
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

public class ActivityCamera extends AppCompatActivity {
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider>                                                                                                                                                                                                                                                                                                                                                                                                 cameraProviderFuture;
    private ImageCapture imageCapture;
    private ImageButton captureBtn;
    private ProcessCameraProvider camera;
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

    private void showResultFragment(String value) {
        Bundle bundle = new Bundle();
        String barcodeValue = value;
        bundle.putString("barcode", barcodeValue);
        FragmentBarcodeDialog fragInfo = new FragmentBarcodeDialog();
        fragInfo.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.activity_camera, fragInfo)
                .addToBackStack(null)
                .commit();
    }
    public void toggleCaptureBtn() {
        captureBtn.setEnabled(!captureBtn.isEnabled());
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
                                showResultFragment(rawValue);
                            }
                        } else {
                            showSnackbar();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    showSnackbar();
                });
        }
    }

    private void showSnackbar() {
        Snackbar.make(findViewById(R.id.captureBtn),
                getResources().getString(R.string.retryToastText),
                Snackbar.LENGTH_SHORT)
                .setAnchorView(R.id.barcodeBig)
                .show();
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
        imageCapture = new ImageCapture.Builder().build();
        cameraProvider.bindToLifecycle(
                this, cameraSelector, imageCapture, imageAnalysis, preview);
        camera = cameraProvider;
    }

}
