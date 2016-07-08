package es.ava.aruco.exceptions;

/**
 * This exception will be thrown when a non valid CameraParameters
 * is used. The validity of these objects can be tested with the method
 * isValid() in class CameraParameters
 * @author Rafa Ortega
 *
 */
public class CPException extends Exception{

	private static final long serialVersionUID = 1L;
	private String message;

	public CPException(String string) {
		message = string;
	}
	
	public String getMessage(){
		return message;
	}
}
