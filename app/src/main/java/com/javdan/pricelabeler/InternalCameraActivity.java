package com.javdan.pricelabeler;

import android.Manifest;
import android.app.Activity;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.util.Size;
import android.view.*;
import android.widget.*;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.*;

public class InternalCameraActivity extends Activity {

    public static final String EXTRA_CAPTURED_PATH = "javdan_captured_path";
    private static final int REQ_CAMERA_PERMISSION = 701;

    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewBuilder;
    private ImageReader imageReader;
    private HandlerThread cameraThread;
    private Handler cameraHandler;

    private String cameraId;
    private int sensorOrientation = 90;
    private boolean frontFacing = false;
    private boolean captureInProgress = false;

    private Button captureButton;
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();

        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERMISSION);
        }
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        textureView = new TextureView(this);
        root.addView(textureView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(20, 16, 20, 28);
        bottom.setBackgroundColor(0x88000000);

        statusView = new TextView(this);
        statusView.setText("دوربین داخلی Javdan Price Labeler");
        statusView.setTextColor(Color.WHITE);
        statusView.setTextSize(15);
        statusView.setGravity(Gravity.CENTER);
        bottom.addView(statusView);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);

        Button cancel = new Button(this);
        cancel.setText("انصراف");
        cancel.setAllCaps(false);
        cancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        captureButton = new Button(this);
        captureButton.setText("● ثبت عکس");
        captureButton.setAllCaps(false);
        captureButton.setEnabled(false);
        captureButton.setOnClickListener(v -> capturePhoto());

        buttons.addView(cancel, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        buttons.addView(captureButton, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f));

        bottom.addView(buttons);

        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        bp.gravity = Gravity.BOTTOM;
        root.addView(bottom, bp);

        setContentView(root);

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                openCamera();
            }
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) { return true; }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraThread();
        if (textureView != null && textureView.isAvailable()) openCamera();
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopCameraThread();
        super.onPause();
    }

    private void startCameraThread() {
        if (cameraThread != null) return;
        cameraThread = new HandlerThread("JavdanInternalCamera");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
    }

    private void stopCameraThread() {
        if (cameraThread == null) return;
        try {
            cameraThread.quitSafely();
            cameraThread.join();
        } catch (Exception ignored) {}
        cameraThread = null;
        cameraHandler = null;
    }

    private void chooseCamera(CameraManager manager) throws Exception {
        String fallback = null;
        for (String id : manager.getCameraIdList()) {
            CameraCharacteristics c = manager.getCameraCharacteristics(id);
            Integer facing = c.get(CameraCharacteristics.LENS_FACING);
            if (fallback == null) fallback = id;
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                cameraId = id;
                frontFacing = false;
                Integer so = c.get(CameraCharacteristics.SENSOR_ORIENTATION);
                if (so != null) sensorOrientation = so;
                return;
            }
        }
        cameraId = fallback;
        if (cameraId != null) {
            CameraCharacteristics c = manager.getCameraCharacteristics(cameraId);
            Integer facing = c.get(CameraCharacteristics.LENS_FACING);
            frontFacing = facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT;
            Integer so = c.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if (so != null) sensorOrientation = so;
        }
    }

    private Size chooseJpegSize(CameraCharacteristics c) {
        android.hardware.camera2.params.StreamConfigurationMap map =
                c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) return new Size(1920, 1080);
        Size[] sizes = map.getOutputSizes(android.graphics.ImageFormat.JPEG);
        if (sizes == null || sizes.length == 0) return new Size(1920, 1080);

        List<Size> list = new ArrayList<>(Arrays.asList(sizes));
        Collections.sort(list, (a,b) -> Long.compare(
                (long)b.getWidth()*b.getHeight(),
                (long)a.getWidth()*a.getHeight()));

        for (Size s : list) {
            long px = (long)s.getWidth() * s.getHeight();
            if (px <= 12_000_000L && s.getWidth() >= 1600 && s.getHeight() >= 1200) {
                return s;
            }
        }
        return list.get(0);
    }

    private void openCamera() {
        if (cameraDevice != null || cameraHandler == null) return;

        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
            chooseCamera(manager);
            if (cameraId == null) throw new IllegalStateException("دوربین پیدا نشد");

            CameraCharacteristics c = manager.getCameraCharacteristics(cameraId);
            Size jpegSize = chooseJpegSize(c);

            if (imageReader != null) imageReader.close();
            imageReader = ImageReader.newInstance(
                    jpegSize.getWidth(), jpegSize.getHeight(),
                    android.graphics.ImageFormat.JPEG, 2);

            imageReader.setOnImageAvailableListener(reader -> {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image == null) return;

                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);

                    File dir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "camera_capture");
                    if (!dir.exists() && !dir.mkdirs()) {
                        throw new IllegalStateException("پوشه عکس ساخته نشد");
                    }

                    File out = new File(dir, "Javdan_" + System.currentTimeMillis() + ".jpg");
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        fos.write(bytes);
                        fos.flush();
                    }

                    if (!out.exists() || out.length() <= 0) {
                        throw new IllegalStateException("فایل عکس صفر بایت شد");
                    }

                    Intent result = new Intent();
                    result.putExtra(EXTRA_CAPTURED_PATH, out.getAbsolutePath());
                    setResult(RESULT_OK, result);
                    runOnUiThread(this::finish);

                } catch (Exception e) {
                    final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                    runOnUiThread(() -> {
                        captureInProgress = false;
                        captureButton.setEnabled(true);
                        statusView.setText("خطا: " + msg);
                        Toast.makeText(this, "ذخیره عکس ناموفق بود: " + msg, Toast.LENGTH_LONG).show();
                    });
                } finally {
                    if (image != null) image.close();
                }
            }, cameraHandler);

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;
                    createPreviewSession();
                }
                @Override public void onDisconnected(CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }
                @Override public void onError(CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                    runOnUiThread(() -> Toast.makeText(
                            InternalCameraActivity.this,
                            "خطای دوربین داخلی: " + error,
                            Toast.LENGTH_LONG).show());
                }
            }, cameraHandler);

        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.toString() : e.getMessage();
            statusView.setText("خطا در باز کردن دوربین: " + msg);
            Toast.makeText(this, statusView.getText(), Toast.LENGTH_LONG).show();
        }
    }

    private void createPreviewSession() {
        if (cameraDevice == null || !textureView.isAvailable() || imageReader == null) return;
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) return;
            texture.setDefaultBufferSize(1920, 1080);
            Surface previewSurface = new Surface(texture);

            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(previewSurface);
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            previewBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON);

            cameraDevice.createCaptureSession(
                    Arrays.asList(previewSurface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override public void onConfigured(CameraCaptureSession session) {
                            if (cameraDevice == null) return;
                            captureSession = session;
                            try {
                                captureSession.setRepeatingRequest(
                                        previewBuilder.build(), null, cameraHandler);
                                runOnUiThread(() -> {
                                    captureButton.setEnabled(true);
                                    statusView.setText("آماده ثبت عکس");
                                });
                            } catch (Exception e) {
                                runOnUiThread(() -> statusView.setText("Preview دوربین آماده نشد"));
                            }
                        }

                        @Override public void onConfigureFailed(CameraCaptureSession session) {
                            runOnUiThread(() -> statusView.setText("راه‌اندازی دوربین ناموفق بود"));
                        }
                    },
                    cameraHandler);

        } catch (Exception e) {
            statusView.setText("خطای Preview: " + e.getMessage());
        }
    }

    private int jpegOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int deviceDegrees;
        if (rotation == Surface.ROTATION_90) deviceDegrees = 90;
        else if (rotation == Surface.ROTATION_180) deviceDegrees = 180;
        else if (rotation == Surface.ROTATION_270) deviceDegrees = 270;
        else deviceDegrees = 0;

        if (frontFacing) return (sensorOrientation + deviceDegrees) % 360;
        return (sensorOrientation - deviceDegrees + 360) % 360;
    }

    private void capturePhoto() {
        if (captureInProgress || cameraDevice == null || captureSession == null || imageReader == null) return;

        captureInProgress = true;
        captureButton.setEnabled(false);
        statusView.setText("در حال ثبت عکس...");

        try {
            CaptureRequest.Builder still =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            still.addTarget(imageReader.getSurface());
            still.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            still.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON);
            still.set(CaptureRequest.JPEG_QUALITY, (byte)95);
            still.set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation());

            captureSession.capture(still.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureFailed(CameraCaptureSession session,
                                            CaptureRequest request,
                                            CaptureFailure failure) {
                    runOnUiThread(() -> {
                        captureInProgress = false;
                        captureButton.setEnabled(true);
                        statusView.setText("ثبت عکس ناموفق بود");
                    });
                }
            }, cameraHandler);

        } catch (Exception e) {
            captureInProgress = false;
            captureButton.setEnabled(true);
            statusView.setText("خطا در ثبت عکس: " + e.getMessage());
        }
    }

    private void closeCamera() {
        try { if (captureSession != null) captureSession.close(); } catch (Exception ignored) {}
        captureSession = null;
        try { if (cameraDevice != null) cameraDevice.close(); } catch (Exception ignored) {}
        cameraDevice = null;
        try { if (imageReader != null) imageReader.close(); } catch (Exception ignored) {}
        imageReader = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "برای گرفتن عکس باید دسترسی دوربین را اجازه بدهید", Toast.LENGTH_LONG).show();
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }
}
