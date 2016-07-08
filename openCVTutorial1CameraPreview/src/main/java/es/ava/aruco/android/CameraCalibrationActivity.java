package es.ava.aruco.android;

import java.io.File;
import java.util.List;
import java.util.Vector;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.samples.tutorial1.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;
//import es.ava.aruco.R;

public class CameraCalibrationActivity extends Activity{
	
	private static final int TAKE_PHOTO = 1;
	private static int PHOTOS = 0;
	
	private File dir;
	private Mat im;
	private Mat corners;
	
	private List<Mat> imageCorners;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			// make the directory 'calibration'
			dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/calibration");
			dir.mkdirs();
		
			im = new Mat();
			corners = new Mat();
			imageCorners = new Vector<Mat>();
			
			setContentView(R.layout.calibrate);
		}
		else{
			Toast t = Toast.makeText(getApplicationContext(), "Couldn't access media storage", Toast.LENGTH_SHORT);
			t.show();
			finish();
		}
	}
	

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == TAKE_PHOTO) {
			if (resultCode == RESULT_OK) {
				// new photo taken
				// read the photo
				im = Highgui.imread(dir.getAbsolutePath()+"/calibrate"+PHOTOS+".jpg");
				// find corners in an asynctask because it is a heavy operation
				new FindCornersTask(this).execute(im);
//				boolean ret = Calib3d.findChessboardCorners(im, new Size(9,6), corners);
//				Calib3d.drawChessboardCorners(im, new Size(9,6), corners, ret);
				// save it
//				Highgui.imwrite(dir.getAbsolutePath()+"/calibrate"+PHOTOS+"-drawn.jpg", im);
				// insert it into the layout
//				ImageView photo = (ImageView) findViewById(R.id.photo);
//				Bitmap bmp = Bitmap.createBitmap(im.cols(), im.rows(), Bitmap.Config.ARGB_8888);
//				org.opencv.android.Utils.matToBitmap(im,bmp);
//				photo.setImageBitmap(bmp);
				PHOTOS++;
			}
		}
	}
	
	public void calibrate(View view){
		if(PHOTOS < 4){
			Toast toast = Toast.makeText(getApplicationContext(), "at least 4 photos are required", Toast.LENGTH_SHORT);
			toast.show();
			return;
		}
		
//		Calib3d.calibrateCamera(objectPoints, imagePoints, imageSize, cameraMatrix, distCoeffs, rvecs, tvecs)
		// read the images
		Mat im1 = new Mat();
		Mat im2 = new Mat();
		
		File root = Environment.getExternalStorageDirectory();
	    File file = new File(root, "/calibration/IMAG0232.jpg");
	    im1 = Highgui.imread(file.getAbsolutePath());
	    if(file.exists()){
	    	Toast toast = Toast.makeText(getApplicationContext(), "abierto", Toast.LENGTH_LONG);
	    	toast.show();
	    }
	}
	
	public void takePhoto(View view){
		Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		File photo = new File(Environment
				.getExternalStorageDirectory(), "/calibration/calibrate"+PHOTOS+".jpg");
		Uri fileUri = Uri.fromFile(photo);
		i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
		startActivityForResult(i, TAKE_PHOTO);
	}
	
	private class FindCornersTask extends AsyncTask<Mat, Void, Void> {
		private ProgressDialog dialog;
		
		private FindCornersTask(Context context) {
			super();
			dialog = new ProgressDialog(context);
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog.setMessage("Looking for corners...");
			dialog.show();
		}

		@Override
		protected Void doInBackground(Mat... m) {
			Mat corners = new Mat();
//			boolean ret = Calib3d.findChessboardCorners(m[0], new Size(9,6), corners);TODO
//			if(ret)
				// only agregate if all the corners are found
				imageCorners.add(corners);
			return null;
		}

		@Override
		protected void onPostExecute(Void v) {
			super.onPostExecute(v);
			if (dialog.isShowing())
				dialog.dismiss();
//
//			if (dialog.isShowing())
//				dialog.dismiss();
//
//			Toast t;
//			//
//			if (id == -3) {
//				t = Toast.makeText(getApplicationContext(), "Unknow host",
//						Toast.LENGTH_SHORT);
//				t.show();
//			} else if (id == -4) {
//				t = Toast.makeText(getApplicationContext(),
//						"Servidor no disponible", Toast.LENGTH_SHORT);
//				t.show();
//			} else {
//				mUser.setIdUsuario(id);
//
//				ContactManagerDataSource db = new ContactManagerDataSource(
//						getApplicationContext());
//				if (db.insertUser(mUser) == false)
//					t = Toast.makeText(getApplicationContext(), "error",
//							Toast.LENGTH_SHORT);
//				else {
//					t = Toast.makeText(getApplicationContext(), "registrado",
//							Toast.LENGTH_SHORT);
//					Intent i = new Intent(getApplicationContext(),
//							MainMenuActivity.class);
//					startActivity(i);
//					finish();
//				}
//				t.show();
//			}
		}
	}
}
