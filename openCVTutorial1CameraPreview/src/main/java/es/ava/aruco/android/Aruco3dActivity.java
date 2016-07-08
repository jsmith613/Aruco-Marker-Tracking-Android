package es.ava.aruco.android;

import java.util.Vector;

import min3d.core.RendererActivity;
import min3d.vos.Light;

import org.opencv.core.Mat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import es.ava.aruco.Board;
import es.ava.aruco.BoardConfiguration;
import es.ava.aruco.Marker;

public abstract class Aruco3dActivity extends RendererActivity {
	
	public boolean mLookForBoard;
	public float mMarkerSize;
	public BoardConfiguration mBC;
	public boolean mShowFps;
	
	
	public ArucoView mView;
	
	protected void onCreate(Bundle savedInstanceState) 
	{
		initDetectionParam();
		super.onCreate(savedInstanceState);
	}
	
    @Override
	protected void onPause() {
		super.onPause();
		mView.releaseCamera();
	}
    
	@Override
	protected void onResume() {
		super.onResume();
		if( !mView.openCamera() ) {
			AlertDialog ad = new AlertDialog.Builder(this).create();  
			ad.setCancelable(false); // This blocks the 'BACK' button  
			ad.setMessage("couldn't open camera!");  
			ad.setButton("OK", new DialogInterface.OnClickListener() {  
			    public void onClick(DialogInterface dialog, int which) {  
			        dialog.dismiss();                      
					finish();
			    }  
			});  
			ad.show();
		}
	}
	
    @Override 
	protected void glSurfaceViewConfig()
    {
		// !important
        _glSurfaceView.setEGLConfigChooser(8,8,8,8, 16, 0);
        _glSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
    }
    
    @Override
	public void onInitScene() {
		// !important
		scene.backgroundColor().setAll(0x00000000);
		scene.lights().add(new Light());
	}
	
	public void onCreateSetContentView(){
        requestWindowFeature(Window.FEATURE_NO_TITLE);
		addContentView(_glSurfaceView, new LayoutParams(LayoutParams.FILL_PARENT, 
				LayoutParams.FILL_PARENT));
		mView = new ArucoView(this, this, mMarkerSize, mShowFps);
		addContentView(mView, new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
	}
	
	public abstract void initDetectionParam();
	public abstract void onDetection(Mat frame, Vector<Marker> detectedMarkers, int idSelected);
	public abstract void onBoardDetection(Mat mFrame, Board mBoardDetected, float probability);
}