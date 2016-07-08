package es.ava.aruco;

/**
 * Defines a board configuration by its width, height and the ids of the markers
 * it contains. You can specify as well the distance between markers and their size.
 * @author Rafael Ortega
 *
 */
public class BoardConfiguration{
	protected int width, height;
	protected int[][] markersId;
	protected int markerSizePix, markerDistancePix;
	
	public BoardConfiguration(int width, int height, int[][] markersId,
			int markerSizePix, int markerDistancePix) {
		super();
		this.width = width;
		this.height = height;
		this.markersId = markersId;
		this.markerSizePix = markerSizePix;
		this.markerDistancePix = markerDistancePix;
	}
}
