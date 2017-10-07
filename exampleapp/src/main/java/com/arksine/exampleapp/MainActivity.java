package com.arksine.exampleapp;

import android.hardware.usb.UsbDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import com.arksine.libusbtv.UsbTv;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;


public class MainActivity extends AppCompatActivity {
    private final Object CAM_LOCK = new Object();

    private FrameLayout mRootLayout;
    private SurfaceView mCameraView;
    private Surface mPreviewSurface;
    private SurfaceHolder mSurfaceHolder;
    private boolean mIsFullScreen = true;
    AtomicBoolean mIsStreaming = new AtomicBoolean(false);


    private UsbTv mTestDriver;
    private final UsbTv.DriverCallbacks mCallbacks = new UsbTv.DriverCallbacks() {
        @Override
        public void onOpen(boolean status) {
            Timber.i("UsbTv Open Status: %b", status);
            synchronized (CAM_LOCK) {
                if (mPreviewSurface != null) {
                    mIsStreaming.set(true);
                    mTestDriver.setDrawingSurface(mPreviewSurface);
                    mTestDriver.startStreaming();
                }
            }
        }

        @Override
        public void onClose() {
            Timber.i("UsbTv Device Closed");
            if (mPreviewSurface != null) {
                mPreviewSurface.release();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTestDriver = new UsbTv(getApplicationContext(), mCallbacks,
                false);

        mRootLayout = (FrameLayout) findViewById(R.id.activity_main);
        mCameraView = (SurfaceView) findViewById(R.id.camera_view);

        mSurfaceHolder = mCameraView.getHolder();
        mSurfaceHolder.addCallback(mCameraViewCallback);

        ArrayList<UsbDevice> devList = UsbTv.enumerateUsbtvDevices(this);
        UsbDevice device = null;
        if (!devList.isEmpty()) {
            device = devList.get(0);
        } else {
            Timber.i("Dev List Empty");
        }

        if (device != null) {
            Timber.i("Open Device");
            mTestDriver.open(device);
        } else {
            Timber.i("Can't open");
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTestDriver.close();
    }

    private final SurfaceHolder.Callback mCameraViewCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(final SurfaceHolder holder) {
            Timber.v("Camera surfaceCreated:");
        }

        @Override
        public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
            if ((width == 0) || (height == 0)) return;
            Timber.v("Camera surfaceChanged:");
            mPreviewSurface = holder.getSurface();
            synchronized (CAM_LOCK) {
                if (mTestDriver!= null & mIsStreaming.compareAndSet(false, true)) {
                    mTestDriver.setDrawingSurface(mPreviewSurface);
                    mTestDriver.startStreaming();
                }
            }
        }

        @Override
        public void surfaceDestroyed(final SurfaceHolder holder) {
            Timber.v("Camera surfaceDestroyed:");
            synchronized (CAM_LOCK) {
                if (mTestDriver != null && mIsStreaming.get()) {
                    mTestDriver.stopStreaming();
                }
                mIsStreaming.set(false);
                mPreviewSurface = null;
            }
        }
    };

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            setViewLayout();
        }
    }

    private void setViewLayout() {
        if (mCameraView == null) {
            return;
        }

        FrameLayout.LayoutParams params;

        if (mIsFullScreen)  {
            params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER);
            mCameraView.setLayoutParams(params);
        } else {
            int currentWidth = mRootLayout.getWidth();
            int currentHeight = mRootLayout.getHeight();

            if (currentWidth >= (4 * currentHeight) / 3) {
                int destWidth = (4 * currentHeight) / 3 + 1;
                params = new FrameLayout.LayoutParams(destWidth,
                        FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER);
            } else {
                int destHeight = (3 * currentWidth) / 4 + 1;
                params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        destHeight, Gravity.CENTER);
            }

            mCameraView.setLayoutParams(params);

        }

        Timber.v("Current view size %d x %d: ", mCameraView.getWidth(), mCameraView.getHeight());
    }
}