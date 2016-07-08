package es.ava.aruco.android;

import java.util.Date;

import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.View.OnTouchListener;
import es.ava.aruco.BoardDetector;
import es.ava.aruco.CameraParameters;
import es.ava.aruco.Marker;
import es.ava.aruco.MarkerDetector;
import es.ava.aruco.Utils;
import es.ava.aruco.exceptions.CPException;
import es.ava.aruco.exceptions.ExtParamException;

public class ArucoView extends ViewBase implements OnTouchListener{
	private Mat mFrame;
    private int mIdSelected;
    public CameraParameters mCamParam;
    protected float markerSizeMeters;
    protected Aruco3dActivity	mRenderer;
    protected MarkerDetector mDetector;
    protected BoardDetector mBDetector;
    
    // things needed to interaction with object by touching the screen
    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float mPreviousX;
    private float mPreviousY;
    // current angles of rotation
    private float mAnglex;
    private float mAngley;
    // scaling factor and detector
    private float mScaleFactor = 1f;
    private ScaleGestureDetector mScaleDetector;
    
    public ArucoView(Context context, Aruco3dActivity renderer, float markerSize, boolean showFps) {
        super(context);
        
        setOnTouchListener(this);
        mCamParam = new CameraParameters();
        mCamParam.readFromFile(Environment.getExternalStorageDirectory().toString() + "/camCalib/camCalibData.csv");
        mDetector = new MarkerDetector();
        mBDetector = new BoardDetector();
        mRenderer = renderer;
        markerSizeMeters = markerSize;
        mShowFps = showFps;
        mIdSelected = -1;
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    @Override
    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
        super.surfaceChanged(_holder, format, width, height);

        synchronized (this) {
            // initialize Mats before usage TODO proper type
        	mFrame = new Mat();
        	
    		double[] proj_matrix = new double[16];
    		try {
    			Utils.myProjectionMatrix(mCamParam, new Size(width,height), proj_matrix, 0.05, 10);
//    			Utils.glGetProjectionMatrix(mCamParam, new Size(width,height),
//    					new Size(width, height), proj_matrix, 0.05, 10);
    		} catch (CPException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		} catch (ExtParamException e){
    			e.getMessage();
    		}
    		mRenderer.setProjMatrix(proj_matrix);
        }
    }
    
    @Override
    protected Bitmap processFrame(VideoCapture capture, SurfaceHolder holder, int width, int height) {
        capture.retrieve(mFrame, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);

		mDetector.detect(mFrame, mDetectedMarkers, mCamParam, markerSizeMeters);
		
		mRenderer.onDetection(mFrame, mDetectedMarkers, mIdSelected);
		
		// the onTouch method may have set this to false. Now it is set again to true to continue rendering
		mRenderer.renderContinuously(true);

		if(mRenderer.mLookForBoard == true){
			float prob=0f;
			try {
				Date initial = new Date();
				prob = mBDetector.detect(mDetectedMarkers, mRenderer.mBC, mBoardDetected, mCamParam, markerSizeMeters);
				Log.d(VIEW_LOG_TAG, "boarddetection took: " + ((new Date()).getTime() - initial.getTime()));
			} catch (CvException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mRenderer.onBoardDetection(mFrame, mBoardDetected, prob);
		}
        Bitmap bmp = Bitmap.createBitmap(mFrame.cols(), mFrame.rows(), Bitmap.Config.ARGB_8888);

        try{
        	org.opencv.android.Utils.matToBitmap(mFrame,bmp);
            return bmp;
        }
        catch(IllegalArgumentException e){
            bmp.recycle();
            return null;
        }
    }

    @Override
    public void run() {
        super.run();

        synchronized (this) {
            // Explicitly deallocate Mats
        	if(mFrame != null)
        		mFrame.release();
        	mFrame = null;
        }
    }

	@Override
	public boolean onTouch(android.view.View v, MotionEvent event) {
		float x = event.getX();
		float y = event.getY();
		
		mScaleDetector.onTouchEvent(event);
        switch (event.getAction()) {
        case MotionEvent.ACTION_MOVE:
        	// rotate
//        	if(mIdSelected != -1){// if there is a marker selected
        		// only rotate if no scale is taking place
        		if(!mScaleDetector.isInProgress()){
	        		// calculate the new angles
		            float dx = x - mPreviousX;
		            float dy = y - mPreviousY;
		            mAnglex += dx * TOUCH_SCALE_FACTOR;
		            mAngley += dy * TOUCH_SCALE_FACTOR;
		            invalidate();
        		}
//        	}
            mPreviousX = x;
            mPreviousY = y;
        	break;
        case MotionEvent.ACTION_DOWN:
        	// select
        	
			// avoid rendering again until the new markers are detected
			// this will be set to true in processFrame
			mRenderer.renderContinuously(false);
			boolean found = false;
			for(int i=0;i<mDetectedMarkers.size() && !found;i++){
				Marker m = mDetectedMarkers.get(i);
				if(m.object()!=null && m.object().selected)
					m.object().selected = false;
				if(Imgproc.pointPolygonTest(m, new Point(x,y), false)>=0){
					found = true;
					mIdSelected = m.getMarkerId();
					if(m.object()!=null){
						m.object().selected = true;
						mAnglex = m.object().rotation().x;
						mAngley = m.object().rotation().y;
						mScaleFactor = m.object().scale().x / m.object().initialScale();
					}
				}
			}
            mPreviousX = x;
            mPreviousY = y;
        }
		return true;
	}
	
	public float mAngleX(){
		return mAnglex;
	}
	
	public float mAngleY(){
		return mAngley;
	}
	
	public float mScale(){
		return mScaleFactor;
	}
	
	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		
	    @Override
	    public boolean onScale(ScaleGestureDetector detector) {
	        mScaleFactor *= detector.getScaleFactor();
	        
	        // Don't let the object get too small or too large.
	        mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));

	        invalidate();
	        return true;
	    }
	}
}