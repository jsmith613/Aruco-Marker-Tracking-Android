package es.ava.aruco;

import android.util.Log;

import java.util.List;
import java.util.Vector;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import es.ava.aruco.exceptions.CPException;
import es.ava.aruco.exceptions.ExtParamException;

/**
 * Misc utilities for aruco library.
 * @author Rafael Ortega
 *
 */
public abstract class Utils {
	
	/**
	 * Performs the product in 2 matrix with column major-order and stores the result
	 * in a third one. Used to carry out the rotation in a modelView matrix in OpenGL.
	 * @param a matrix A
	 * @param b matrix B
	 * @param dst resulting matrix
	 */
	protected static void matrixProduct(double[] a, double[] b, double dst[]){
        for(int i=0;i<4;i++)
        {
            for(int j=0;j<4;j++)
            {
                dst[i+4*j] = 0;
                for(int k=0;k<4;k++)
                {
                	dst[i+4*j] += a[i+4*k]*b[k+j*4];
                }
            }
        }
	}

	//Fixing Axis Swapping that is described here:
	// http://stackoverflow.com/questions/37953086/aruco-axis-swap-while-drawing-3daxis
	protected static void alignToId(Mat rotation, int codeRotation) {
		//get the matrix corresponding to the rotation vector
		Mat R = new Mat(3, 3, CvType.CV_64FC1);
		Calib3d.Rodrigues(rotation, R);

		codeRotation += 1;
		rotateZAxis(rotation, codeRotation * 90);
	}

	/**
	 * Rotates around Z axis by given amount
	 * @param rotation Rvec to be changed
	 * @param rotateDegrees degrees to be rotated
     */
	protected static void rotateZAxis(Mat rotation, double rotateDegrees) {
		// get the matrix corresponding to the rotation vector
		Mat R = new Mat(3,3,CvType.CV_64FC1);
		Calib3d.Rodrigues(rotation, R);

		// create the matrix to rotate around the Z axis
		// cos -sin  0
		// sin  cos  0
		// 0    0    1
		double[] rot = {
				Math.cos(Math.toRadians(rotateDegrees)), -Math.sin(Math.toRadians(rotateDegrees)), 0,
				Math.sin(Math.toRadians(rotateDegrees)), Math.cos(Math.toRadians(rotateDegrees)), 0,
				0,0,1
		};
		// multiply both matrix
		Mat res = new Mat(3,3, CvType.CV_64FC1);
		double[] prod = new double[9];
		double[] a = new double[9];
		R.get(0, 0, a);
		for(int i=0;i<3;i++)
			for(int j=0;j<3;j++){
				prod[3*i+j] = 0;
				for(int k=0;k<3;k++){
					prod[3*i+j] += a[3*i+k]*rot[3*k+j];
				}
			}
		// convert the matrix to a vector with rodrigues back
		res.put(0, 0, prod);
		Calib3d.Rodrigues(res, rotation);
	}

	protected static void rotateXAxis(Mat rotation){
		// get the matrix corresponding to the rotation vector
		Mat R = new Mat(3,3,CvType.CV_64FC1);
		Calib3d.Rodrigues(rotation, R);
	    
		// create the matrix to rotate 90° around the X axis
	    // 1, 0, 0
	    // 0 cos -sin
	    // 0 sin cos
		double[] rot = {
				1, 0,  0,
				0, 0, -1,
				0, 1, 0
		};
		// multiply both matrix
		Mat res = new Mat(3,3, CvType.CV_64FC1);
		double[] prod = new double[9];
		double[] a = new double[9];
		R.get(0, 0, a);
        for(int i=0;i<3;i++)
            for(int j=0;j<3;j++){
            	prod[3*i+j] = 0;
                for(int k=0;k<3;k++){
                	prod[3*i+j] += a[3*i+k]*rot[3*k+j];
                }
            }
        // convert the matrix to a vector with rodrigues back
        res.put(0, 0, prod);
		Calib3d.Rodrigues(res, rotation);
	}
	
	protected static void glGetModelViewMatrix(double[] modelview_matrix, Mat Rvec, Mat Tvec)throws ExtParamException{
	    //check if parameters are valid
	    boolean invalid=false;
	    double[] tvec = new double[3];
	    double[] rvec = new double[3];
	    
	    Rvec.get(0, 0, rvec);
	    Tvec.get(0, 0, tvec);

	    for (int i=0;i<3 && !invalid ;i++){
	        if (tvec[i] != -999999) invalid|=false;
	        if (rvec[i] != -999999) invalid|=false;
	    }
	    
	    if (invalid)
	    	throw new ExtParamException("extrinsic parameters are not set Marker.getModelViewMatrix");
	    Mat Rot = new Mat(3,3,CvType.CV_32FC1);
	    Mat Jacob = new Mat();
	    Calib3d.Rodrigues(Rvec, Rot, Jacob);// TODO jacob no se vuelve a usar

	    double[][] para = new double[3][4];
	    double[] rotvec = new double[9];
	    Rot.get(0,0,rotvec);
	    for (int i=0;i<3;i++)
	        for (int j=0;j<3;j++)
	        	para[i][j]=rotvec[3*i+j];
	    //now, add the translation
	    para[0][3]=tvec[0];
	    para[1][3]=tvec[1];
	    para[2][3]=tvec[2];
	    double scale=1;

	    // R1C2
	    modelview_matrix[0 + 0*4] = para[0][0];
	    modelview_matrix[0 + 1*4] = para[0][1];
	    modelview_matrix[0 + 2*4] = para[0][2];
	    modelview_matrix[0 + 3*4] = para[0][3];
	    // R2
	    modelview_matrix[1 + 0*4] = para[1][0];
	    modelview_matrix[1 + 1*4] = para[1][1];
	    modelview_matrix[1 + 2*4] = para[1][2];
	    modelview_matrix[1 + 3*4] = para[1][3];
	    // R3
	    modelview_matrix[2 + 0*4] = -para[2][0];
	    modelview_matrix[2 + 1*4] = -para[2][1];
	    modelview_matrix[2 + 2*4] = -para[2][2];
	    modelview_matrix[2 + 3*4] = -para[2][3];
	    
	    modelview_matrix[3 + 0*4] = 0.0f;
	    modelview_matrix[3 + 1*4] = 0.0f;
	    modelview_matrix[3 + 2*4] = 0.0f;
	    modelview_matrix[3 + 3*4] = 1.0f;
	    if (scale != 0.0)
	    {
	        modelview_matrix[12] *= scale;
	        modelview_matrix[13] *= scale;
	        modelview_matrix[14] *= scale;
	    }

	    // rotate 90º around the x axis
	    // rotating around x axis in OpenGL is equivalent to
	    // multiply the model matrix by the matrix:
	    // 1, 0, 0, 0, 0, cos(a), sin(a), 0, 0, -sin(a), cos(a), 0, 0, 0, 0, 1
//	    double[] auxRotMat = new double[]{
//	    	1, 0,  0, 0,
//	    	0, 0, 1, 0,
//	    	0, -1,  0, 0,
//	    	0, 0,  0, 1
//	    };
//	    Utils.matrixProduct(modelview_matrix1, auxRotMat, modelview_matrix);
	}
	
	public static void myProjectionMatrix(CameraParameters cp, Size size,
			double proj_matrix[], double gnear, double gfar) throws CPException, ExtParamException{
		if(cp.isValid() == false)
			throw new CPException("Invalid camera parameters");
		double w = size.width;
		double h = size.height;
        // get the cameraMatrix
        float[] camMat = new float[9];
        cp.getCameraMatrix().get(0,0,camMat);
		proj_matrix[0] = 2*camMat[0] / w;
		proj_matrix[1] = 0;
		proj_matrix[2] = 0;
		proj_matrix[3] = 0;
		proj_matrix[4] = 0;
		proj_matrix[5] = -2*camMat[4] / h;
		proj_matrix[6] = 0;
		proj_matrix[7] = 0;
		proj_matrix[8] = 1-2*camMat[2] / w;
		proj_matrix[9] = 2*camMat[5]/h -1;
		proj_matrix[10] = (-gfar-gnear) / (gfar-gnear);
		proj_matrix[11] = -1;
		proj_matrix[12] = 0;
		proj_matrix[13] = 0;
		proj_matrix[14] = -2*gfar*gnear / (gfar-gnear);
		proj_matrix[15] = 0;
	}
	
	// for debugging
	public static void glIdentityMatrix(double[] m){
		m[0] = 1;
		m[1] = 0;
		m[2] = 0;
		m[3] = 0;
		m[4] = 0;
		m[5] = 1;
		m[6] = 0;
		m[7] = 0;
		m[8] = 0;
		m[9] = 0;
		m[10] = 1;
		m[11] = 0;
		m[12] = 0;
		m[13] = 0;
		m[14] = -5;
		m[15] = 1;
	}
	
	// default invert = false
    public static void glGetProjectionMatrix(CameraParameters cp, Size orgImgSize, Size size, double proj_matrix[],
    		double gnear, double gfar) throws CPException, ExtParamException{	
    	glGetProjectionMatrix(cp, orgImgSize, size, proj_matrix, gnear, gfar, false);
    }
    
    public static void glGetProjectionMatrix(CameraParameters cp, Size orgImgSize, Size size, double proj_matrix[],
    		double gnear, double gfar, boolean invert) throws CPException, ExtParamException{
        if (cp.isValid()==false)
        	throw new CPException("invalid camera parameters MarkerDetector::glGetProjectionMatrix");
        //Deterime the resized info
        double Ax=(double)(size.width)/(double)(orgImgSize.width);
        double Ay=(double)(size.height)/(double)(orgImgSize.height);
        
        // get the cameraMatrix
        float[] camMat = new float[9];
        cp.getCameraMatrix().get(0,0,camMat);
        double _fx=camMat[0]*Ax;
        double _cx=camMat[2]*Ax;
        double _fy=camMat[4]*Ay;
        double _cy=camMat[5]*Ay;
        double[][] cparam = 
        {
            {_fx,   0,  _cx, 0},
            {  0, _fy,  _cy, 0},
            {  0,   0,    1, 0}
        };

        argConvGLcpara2( cparam, size.width, size.height, gnear, gfar, proj_matrix, invert );
    }
    
	public static void draw3dAxis(Mat frame, CameraParameters cp, Scalar color, double height, Mat Rvec, Mat Tvec){
//		Mat objectPoints = new Mat(4,3,CvType.CV_32FC1);;
		MatOfPoint3f objectPoints = new MatOfPoint3f();
		Vector<Point3> points = new Vector<Point3>();
		points.add(new Point3(0,     0,     0));
		points.add(new Point3(height,0,     0));
		points.add(new Point3(0,     height,0));
		points.add(new Point3(0,     0,     height)); 
		objectPoints.fromList(points);

		MatOfPoint2f imagePoints = new MatOfPoint2f();
		Calib3d.projectPoints(objectPoints, Rvec, Tvec,
				cp.getCameraMatrix(), cp.getDistCoeff(), imagePoints);
		List<Point> pts = new Vector<Point>();
		Converters.Mat_to_vector_Point(imagePoints, pts);

		Core.line(frame ,pts.get(0),pts.get(1), color, 2);
		Core.line(frame ,pts.get(0),pts.get(2), color, 2);
		Core.line(frame ,pts.get(0),pts.get(3), color, 2);

		Core.putText(frame, "X", pts.get(1), Core.FONT_HERSHEY_SIMPLEX, 2.0,  color,2);
		Core.putText(frame, "Y", pts.get(2), Core.FONT_HERSHEY_SIMPLEX, 2.0,  color,2);
		Core.putText(frame, "Z", pts.get(3), Core.FONT_HERSHEY_SIMPLEX, 2.0,  color,2);
	}
    
	private static void argConvGLcpara2(double[][] cparam, double width, double height, double gnear,
			double gfar, double[] m, boolean invert) throws ExtParamException{
		double[][] icpara = new double[3][4];
		double[][] trans  = new double[3][4];
		double[][] p      = new double[4][4];
		double[][] q      = new double[4][4];
		
		cparam[0][2] *= -1f;
		cparam[1][2] *= -1f;
		cparam[2][2] *= -1f;
		
		if(arParamDecompMat(cparam, icpara, trans)<0)
			throw new ExtParamException("parameter error, argConvGLcpara2");
		for(int i=0;i<3;i++)
			for(int j=0;j<3;j++)
				p[i][j] = icpara[i][j]/icpara[2][2];
	    q[0][0] = (2.0 * p[0][0] / width);
	    q[0][1] = (2.0 * p[0][1] / width);
	    q[0][2] = ((2.0 * p[0][2] / width)  - 1.0);
	    q[0][3] = 0.0;

	    q[1][0] = 0.0;
	    q[1][1] = (2.0 * p[1][1] / height);
	    q[1][2] = ((2.0 * p[1][2] / height) - 1.0);
	    q[1][3] = 0.0;

	    q[2][0] = 0.0;
	    q[2][1] = 0.0;
	    q[2][2] = (gfar + gnear)/(gfar - gnear);
	    q[2][3] = -2.0 * gfar * gnear / (gfar - gnear);

	    q[3][0] = 0.0;
	    q[3][1] = 0.0;
	    q[3][2] = 1.0;
	    q[3][3] = 0.0;
	    
	    for (int i = 0; i < 4; i++ )
	    {
	        for (int j = 0; j < 3; j++ )
	        {
	            m[i+j*4] = q[i][0] * trans[0][j]
	                       + q[i][1] * trans[1][j]
	                       + q[i][2] * trans[2][j];
	        }
	        m[i+3*4] = q[i][0] * trans[0][3]
	                   + q[i][1] * trans[1][3]
	                   + q[i][2] * trans[2][3]
	                   + q[i][3];
	    }

	    if (!invert)
	    {
	        m[13]=-m[13] ;
	        m[1]=-m[1];
	        m[5]=-m[5];
	        m[9]=-m[9];
	    }
	}
	
	
	private static int arParamDecompMat(double[][] source, double[][] cpara, double[][] trans){
	    int        r, c;
	    double[][] Cpara = new double[3][4];
	    double     rem1, rem2, rem3;

        for ( r = 0; r < 3; r++ )
        {
            for ( c = 0; c < 4; c++ )
            {
                Cpara[r][c] = source[r][c];
            }
        }

	    for ( r = 0; r < 3; r++ )
	    {
	        for ( c = 0; c < 4; c++ )
	        {
	            cpara[r][c] = 0.0;
	        }
	    }
	    cpara[2][2] = norm( Cpara[2][0], Cpara[2][1], Cpara[2][2] );
	    trans[2][0] = Cpara[2][0] / cpara[2][2];
	    trans[2][1] = Cpara[2][1] / cpara[2][2];
	    trans[2][2] = Cpara[2][2] / cpara[2][2];
	    trans[2][3] = Cpara[2][3] / cpara[2][2];

	    cpara[1][2] = dot( trans[2][0], trans[2][1], trans[2][2],
	                       Cpara[1][0], Cpara[1][1], Cpara[1][2] );
	    rem1 = Cpara[1][0] - cpara[1][2] * trans[2][0];
	    rem2 = Cpara[1][1] - cpara[1][2] * trans[2][1];
	    rem3 = Cpara[1][2] - cpara[1][2] * trans[2][2];
	    cpara[1][1] = norm( rem1, rem2, rem3 );
	    trans[1][0] = rem1 / cpara[1][1];
	    trans[1][1] = rem2 / cpara[1][1];
	    trans[1][2] = rem3 / cpara[1][1];

	    cpara[0][2] = dot( trans[2][0], trans[2][1], trans[2][2],
	                       Cpara[0][0], Cpara[0][1], Cpara[0][2] );
	    cpara[0][1] = dot( trans[1][0], trans[1][1], trans[1][2],
	                       Cpara[0][0], Cpara[0][1], Cpara[0][2] );
	    rem1 = Cpara[0][0] - cpara[0][1]*trans[1][0] - cpara[0][2]*trans[2][0];
	    rem2 = Cpara[0][1] - cpara[0][1]*trans[1][1] - cpara[0][2]*trans[2][1];
	    rem3 = Cpara[0][2] - cpara[0][1]*trans[1][2] - cpara[0][2]*trans[2][2];
	    cpara[0][0] = norm( rem1, rem2, rem3 );
	    trans[0][0] = rem1 / cpara[0][0];
	    trans[0][1] = rem2 / cpara[0][0];
	    trans[0][2] = rem3 / cpara[0][0];

	    trans[1][3] = (Cpara[1][3] - cpara[1][2]*trans[2][3]) / cpara[1][1];
	    trans[0][3] = (Cpara[0][3] - cpara[0][1]*trans[1][3]
	                   - cpara[0][2]*trans[2][3]) / cpara[0][0];

	    for ( r = 0; r < 3; r++ )
	    {
	        for ( c = 0; c < 3; c++ )
	        {
	            cpara[r][c] /= cpara[2][2];
	        }
	    }

	    return 0;
	}
	
	private static double norm( double a, double b, double c )
	{
	    return( Math.sqrt( a*a + b*b + c*c ) );
	}
	
	private static double dot( double a1, double a2, double a3,
            double b1, double b2, double b3 )
	{
		return( a1 * b1 + a2 * b2 + a3 * b3 );
	}
	
}
