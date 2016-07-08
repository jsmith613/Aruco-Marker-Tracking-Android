package es.ava.aruco;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Class to detect markers. It will be done by a threshold technique and
 * analysing the contours detected in the frame given looking for valid
 * marker's code inside them.
 * @author Rafael Ortega
 *
 */
// TODO eliminate innecessary native calls, for example store the frame info 
// such as type in member fields and call it only once
public class MarkerDetector {
	private enum thresSuppMethod {FIXED_THRES,ADPT_THRES,CANNY};
	
	private double thresParam1, thresParam2;
	private thresSuppMethod thresMethod;
	private Mat grey, thres, thres2, hierarchy2;
	private Vector<MatOfPoint> contours2;
		
	private final static double MIN_DISTANCE = 10;
	
	public MarkerDetector(){
		thresParam1 = thresParam2 = 7;
		thresMethod = thresSuppMethod.ADPT_THRES;
		// TODO
		grey = new Mat();
		thres = new Mat();
		thres2 = new Mat();
		hierarchy2 = new Mat();
		contours2 = new Vector<MatOfPoint>();
	}
    
	/**
	 * Method to find markers in a Mat given.
	 * @param in input color Mat to find the markers in.
	 * @param detectedMarkers output vector with the markers that have been detected.
	 * @param markerSizeMeters --
	 */

	// @param camMatrix --
    // @param distCoeff --

	public void detect(Mat in, Vector<Marker> detectedMarkers, CameraParameters cp, float markerSizeMeters){
		Vector<Marker> candidateMarkers = new Vector<Marker>();
		// the detection in the incoming frame will be done in a different vector
		// because this will allow the ontouchlistener in View
		// to have a valid detectedMarkers vector longer
		Vector<Marker> newMarkers = new Vector<Marker>();
		
		// do the threshold of image and detect contours
		Imgproc.cvtColor(in, grey, Imgproc.COLOR_RGBA2GRAY);
		thresHold(thresMethod, grey, thres);

		// pass a copy because it modifies the src image
		thres.copyTo(thres2);
		Imgproc.findContours(thres2, contours2, hierarchy2, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);
		
		// uncomment the following line if you want the contours drawn
//		Imgproc.drawContours(frameDebug, contours2, -1, new Scalar(255,0,0),2);
		// to each contour analyze if it is a paralelepiped likely to be a marker
		MatOfPoint2f approxCurve = new MatOfPoint2f();
//		List<Point> approxPoints = new ArrayList<Point>();
		for(int i=0;i<contours2.size();i++){
			MatOfPoint2f contour = new MatOfPoint2f();
			contours2.get(i).convertTo(contour, CvType.CV_32FC2);
			// first check if it has enough points
			int contourSize = (int)contour.total();
			if(contourSize > in.cols()/5){
				Imgproc.approxPolyDP(contour, approxCurve, contourSize*0.05, true);
//				Converters.Mat_to_vector_Point(approxCurve, approxPoints);
				// check the polygon has 4 points
				if(approxCurve.total()== 4){
					// and if it is convex
					MatOfPoint mat = new MatOfPoint();
					approxCurve.convertTo(mat, CvType.CV_32SC2);
					if(Imgproc.isContourConvex(mat)){
						// ensure the distance between consecutive points is large enough
						double minDistFound = Double.MAX_VALUE;
						float[] points = new float[8];// [x1 y1 x2 y2 x3 y3 x4 y4]
						approxCurve.get(0,0,points);
						// look for the min distance
						for(int j=0;j<=4;j+=2){
							double d = Math.sqrt( (points[j]-points[(j+2)%4])*(points[j]-points[(j+2)%4]) +
												(points[j+1]-points[(j+3)%4])*(points[j+1]-points[(j+3)%4]));
							if(d<minDistFound)
								minDistFound = d;
						}
						if(minDistFound > MIN_DISTANCE){
							// create a candidate marker
							Vector<Point> p = new Vector<Point>();
							p.add(new Point(points[0],points[1]));
							p.add(new Point(points[2],points[3]));
							p.add(new Point(points[4],points[5]));
							p.add(new Point(points[6],points[7]));
							candidateMarkers.add(new Marker(markerSizeMeters, p));
//							candidateMarkers.add(new Marker(markerSizeMeters));
//							candidateMarkers.lastElement().add(new Point(points[0],points[1]));
//							candidateMarkers.lastElement().add(new Point(points[2],points[3]));
//							candidateMarkers.lastElement().add(new Point(points[4],points[5]));
//							candidateMarkers.lastElement().add(new Point(points[6],points[7]));
						}
					}
				}
			}
		}// all contours processed, now we have the candidateMarkers
		int nCandidates = candidateMarkers.size();
		// sort the points in anti-clockwise order
		for(int i=0;i<nCandidates;i++){
			Marker marker = candidateMarkers.get(i);
			List<Point> p = new Vector<Point>();
			p = marker.toList();
	        // trace a line between the first and second point.
	        // if the third point is at the right side, then the points are anti-clockwise
			double dx1 = p.get(1).x - p.get(0).x;
			double dy1 = p.get(1).y - p.get(0).y;
			double dx2 = p.get(2).x - p.get(0).x;
			double dy2 = p.get(2).y - p.get(0).y;
			double o = dx1*dy2 - dy1*dx2;
			if(o < 0.0){ // the third point is in the left side, we have to swap
				Collections.swap(p, 1, 3);
				marker.setPoints(p);
			}
		}// points sorted in anti-clockwise order

		// remove the elements whose corners are to close to each other // TODO necessary?
		Vector<Integer> tooNearCandidates = new Vector<Integer>(); // stores the indexes in the candidateMarkers
										   // i.e [2,3,4,5] the marker 2 is too close to 3 and 4 to 5
		for(int i=0;i<nCandidates;i++){
			Marker toMarker = candidateMarkers.get(i);
			List<Point> toPoints = new Vector<Point>();
			toPoints = toMarker.toList();
			// calculate the average distance of each corner to the nearest corner in the other marker
			for(int j=i+1;j<nCandidates;j++){
				float dist=0;
				Marker fromMarker = candidateMarkers.get(j);
				List<Point> fromPoints = new Vector<Point>();
				fromPoints = fromMarker.toList();
				// unrolling loop
				dist+=Math.sqrt((fromPoints.get(0).x-toPoints.get(0).x)*(fromPoints.get(0).x-toPoints.get(0).x)+
						(fromPoints.get(0).y-toPoints.get(0).y)*(fromPoints.get(0).y-toPoints.get(0).y));

				dist+=Math.sqrt((fromPoints.get(1).x-toPoints.get(1).x)*(fromPoints.get(1).x-toPoints.get(1).x)+
						(fromPoints.get(1).y-toPoints.get(1).y)*(fromPoints.get(1).y-toPoints.get(1).y));
				
				dist+=Math.sqrt((fromPoints.get(2).x-toPoints.get(2).x)*(fromPoints.get(2).x-toPoints.get(2).x)+
						(fromPoints.get(2).y-toPoints.get(2).y)*(fromPoints.get(2).y-toPoints.get(2).y));
				
				dist+=Math.sqrt((fromPoints.get(3).x-toPoints.get(3).x)*(fromPoints.get(3).x-toPoints.get(3).x)+
						(fromPoints.get(3).y-toPoints.get(3).y)*(fromPoints.get(3).y-toPoints.get(3).y));
				dist = dist/4;
				if(dist < MIN_DISTANCE){
					tooNearCandidates.add(i);
					tooNearCandidates.add(j);
				}
			}
		}
		Vector<Integer> toRemove = new Vector<Integer>();// 1 means to remove
		for(int i=0;i<nCandidates;i++)
			toRemove.add(0);
		// set to remove the marker with the smaller perimeter
		for(int i=0;i<tooNearCandidates.size();i+=2){
			Marker first = candidateMarkers.get(tooNearCandidates.get(i));
			Marker second = candidateMarkers.get(tooNearCandidates.get(i+1));
			if(first.perimeter()<second.perimeter())
				toRemove.set(tooNearCandidates.get(i), 1);
			else
				toRemove.set(tooNearCandidates.get(i+1), 1);
		}

		// identify the markers
		for(int i=0;i<nCandidates;i++){
			if(toRemove.get(i) == 0){
				Marker marker = candidateMarkers.get(i);
				Mat canonicalMarker = new Mat();
				warp(in, canonicalMarker, new Size(50,50), marker.toList());
				marker.setMat(canonicalMarker);
				marker.extractCode();
				if(marker.checkBorder()){
					int id = marker.calculateMarkerId();
					if(id != -1){
						// rotate the points of the marker so they are always in the same order no matter the camera orientation
						Collections.rotate(marker.toList(), 4-marker.getRotations());

						newMarkers.add(marker);

					}
				}
			}
		}
		// TODO refine using pixel accuracy
		
		// now sort by id and check that each marker is only detected once
		Collections.sort(newMarkers);
		toRemove.clear();
		for(int i=0;i<newMarkers.size();i++)
			toRemove.add(0);
		
		for(int i=0;i<newMarkers.size()-1;i++){
			if(newMarkers.get(i).id == newMarkers.get(i+1).id)
				if(newMarkers.get(i).perimeter()<newMarkers.get(i+1).perimeter())
					toRemove.set(i, 1);
				else
					toRemove.set(i+1, 1);
		}
		
		for(int i=toRemove.size()-1;i>=0;i--)// done in inverse order in case we need to remove more than one element
			if(toRemove.get(i) == 1)
				newMarkers.remove(i);
		
		// detect the position of markers if desired
		for(int i=0;i<newMarkers.size();i++){
			if(cp.isValid())
				newMarkers.get(i).calculateExtrinsics(cp.getCameraMatrix(), cp.getDistCoeff(), markerSizeMeters);
		}
		detectedMarkers.setSize(newMarkers.size());
		Collections.copy(detectedMarkers, newMarkers);
	}
	
    /**
     * Set the parameters of the threshold method
     * We are currently using the Adptive threshold ee opencv doc of adaptiveThreshold for more info
     * @param p1: blockSize of the pixel neighborhood that is used to calculate a threshold value for the pixel
     * @param p2: The constant subtracted from the mean or weighted mean
     */
	public void setThresholdParams(double p1, double p2){
		thresParam1=p1;
		thresParam2=p2;
	}
	
    /**
     * Get the parameters of the threshold method
     * they will be returned as a 2 items double array.
     */
	public double[] getThresholdParams(){
		double[] ret = {thresParam1,thresParam2};
		return ret;
	}
	
	/**
	 * sets the method to be used in the threshold necessary to the marker detection.
	 * @param method must be a supported method.
	 */
	public void setThresholdMethod(thresSuppMethod method){
		thresMethod = method;
	}
	
	/**
	 * returns the method being used to threshold the image.
	 * @return the method used.
	 */
	public thresSuppMethod getThresholdMethod(){
		return thresMethod;
	}
	
	// TODO test different options
	private void thresHold(thresSuppMethod method, Mat src, Mat dst){
		switch(method){
		case FIXED_THRES:
			Imgproc.threshold(src, dst, thresParam1,255, Imgproc.THRESH_BINARY_INV);
			break;
		case ADPT_THRES:
			Imgproc.adaptiveThreshold(src,dst,255.0,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
					Imgproc.THRESH_BINARY_INV,(int)thresParam1,thresParam2);
			break;
		case CANNY:
			Imgproc.Canny(src, dst, 10, 220);// TODO this parameters??
			break;
		}
	}
	
	/**
	 * This fits a mat containing 4 vertices captured through the camera
	 * into a canonical mat.
	 * @param in the frame captured
	 * @param out the canonical mat
	 * @param size the size of the canonical mat we want to create
	 * @param points the coordinates of the points in the "in" mat 
	 */
	private void warp(Mat in, Mat out, Size size, List<Point> points){
		Mat pointsIn = new Mat(4,1,CvType.CV_32FC2);
		Mat pointsRes = new Mat(4,1,CvType.CV_32FC2);
		pointsIn.put(0,0, points.get(0).x,points.get(0).y,
						  points.get(1).x,points.get(1).y,
						  points.get(2).x,points.get(2).y,
						  points.get(3).x,points.get(3).y);
		pointsRes.put(0,0, 0,0,
						   size.width-1,0,
						   size.width-1,size.height-1,
						   0,size.height-1);
		Mat m = new Mat();
		m = Imgproc.getPerspectiveTransform(pointsIn, pointsRes);
		Imgproc.warpPerspective(in, out, m, size);
	}
}
