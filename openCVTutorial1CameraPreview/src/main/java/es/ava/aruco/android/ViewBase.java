package es.ava.aruco.android;

import java.util.List;
import java.util.Vector;

import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import es.ava.aruco.Board;
import es.ava.aruco.Marker;
import es.ava.aruco.debug.FpsMeter;

public abstract class ViewBase extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "Sample::SurfaceView";

    private int mFrameWidth, mFrameHeight;
    private SurfaceHolder       mHolder;
    private VideoCapture        mCamera;

    protected Vector<Marker>	 	mDetectedMarkers;
    protected Board					mBoardDetected;
    
    private FpsMeter            mFps;
	public boolean mShowFps;
    
    public ViewBase(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
        mFps = new FpsMeter();
        mDetectedMarkers = new Vector<Marker>();
        mBoardDetected = new Board();
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    public boolean openCamera() {
        Log.i(TAG, "openCamera");
        synchronized (this) {
	        releaseCamera();
	        mCamera = new VideoCapture(Highgui.CV_CAP_ANDROID);
	        if (!mCamera.isOpened()) {
	            mCamera.release();
	            mCamera = null;
	            Log.e(TAG, "Failed to open native camera");
	            return false;
	        }
	    }
        return true;
    }
    
    public void releaseCamera() {
        Log.i(TAG, "releaseCamera");
        synchronized (this) {
	        if (mCamera != null) {
	                mCamera.release();
	                mCamera = null;
            }
        }
    }
    
    public void setupCamera(int width, int height) {
        Log.i(TAG, "setupCamera("+width+", "+height+")");
        synchronized (this) {
            if (mCamera != null && mCamera.isOpened()) {
                List<Size> sizes = mCamera.getSupportedPreviewSizes();
                int mFrameWidth = width;
                int mFrameHeight = height;

                // selecting optimal camera preview size
                {
                    double minDiff = Double.MAX_VALUE;
                    for (Size size : sizes) {
                        if (Math.abs(size.height - height) < minDiff) {
                            mFrameWidth = (int) size.width;
                            mFrameHeight = (int) size.height;
                            minDiff = Math.abs(size.height - height);
                        }
                    }
                }

                mCamera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, mFrameWidth);
                mCamera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, mFrameHeight);
            }
        }

    }
    
    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged");
        setupCamera(width, height);
    }
    
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
        (new Thread(this)).start();
    }
    
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        releaseCamera();
    }
    
//    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
//        Log.i(TAG, "surfaceCreated");
//        synchronized (this) {
//            if (mCamera != null && mCamera.isOpened()) {
//                List<Size> sizes = mCamera.getSupportedPreviewSizes();
//                mFrameWidth = width;
//                mFrameHeight = height;
//
//                // selecting optimal camera preview size
//                {
//                    double minDiff = Double.MAX_VALUE;
//                    for (Size size : sizes) {
//                        if (Math.abs(size.height - height) < minDiff) {
//                            mFrameWidth = (int) size.width;
//                            mFrameHeight = (int) size.height;
//                            minDiff = Math.abs(size.height - height);
//                        }
//                    }
//                }
//
//                mCamera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, mFrameWidth);
//                mCamera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, mFrameHeight);
//
////              mCamera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, 768);
////              mCamera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, 432);
//                
//            }
//        }
//    }
//
//    public void surfaceCreated(SurfaceHolder holder) {
//        Log.i(TAG, "surfaceCreated");
//        mCamera = new VideoCapture(Highgui.CV_CAP_ANDROID);
//        if (mCamera.isOpened()) {
//            (new Thread(this)).start();
//        } else {
//            mCamera.release();
//            mCamera = null;
//            Log.e(TAG, "Failed to open native camera");
//        }
//    }
//
//    public void surfaceDestroyed(SurfaceHolder holder) {
//        Log.i(TAG, "surfaceDestroyed");
//        if (mCamera != null) {
//            synchronized (this) {
//                mCamera.release();
//                mCamera = null;
//            }
//        }
//    }

    protected abstract Bitmap processFrame(VideoCapture capture, SurfaceHolder holder, int width, int height);

    public void run() {
        Log.i(TAG, "Starting processing thread");
        mFps.init();
        
        while (true) {
            Bitmap bmp = null;

            synchronized (this) {
                if (mCamera == null)
                    break;

                if (!mCamera.grab()) {
                    Log.e(TAG, "mCamera.grab() failed");
                    break;
                }

                bmp = processFrame(mCamera, mHolder, mFrameWidth, mFrameHeight);
                mFps.measure();
            }

            if (bmp != null) {
                Canvas canvas = mHolder.lockCanvas();
                if (canvas != null) {
                	// drawBitmap
                	canvas.drawBitmap(bmp, (canvas.getWidth() - bmp.getWidth()) / 2, (canvas.getHeight() - bmp.getHeight()) / 2, null);
                	if(mShowFps)
                		mFps.draw(canvas, (canvas.getWidth() - bmp.getWidth()) / 2, 0);
                    mHolder.unlockCanvasAndPost(canvas);
                }
                bmp.recycle();
            }
        }

        Log.i(TAG, "Finishing processing thread");
    }
}