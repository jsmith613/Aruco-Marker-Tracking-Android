package es.ava.aruco;

import android.util.Log;

/**
 * The code stored as matrix of int represents the inside content of a marker.
 * It will have 7x7 dimensions.
 * 0->black
 * 1->white
 *
 */
public class Code {// TODO check if the parameters are in range
	protected int[][] code;
	
	protected Code(){
		code = new int[7][7];
	}
	
	protected void set(int x, int y, int value){
		code[x][y] = value;
	}

	protected int get(int x, int y){
		return code[x][y];
	}
	
	static protected Code rotate(Code in){
		Code out = new Code();
		for(int i=0;i<7;i++)
			for(int j=0;j<7;j++){
				out.code[i][j] = in.code[6-j][i];
			}
		return out;
	}

	public String toString() {
        String returnStr = "";
        for (int i = 0; i < 7; i++) {
            String aLine = "";
            for (int j = 0; j < 7; j++) {
                aLine = aLine + Integer.toString(code[j][i]);
            }
            returnStr = returnStr + aLine + "\n";
        }
        return returnStr;
//        Log.d("Code", lines[1]);
//        Log.d("Code", lines[2]);
//        Log.d("Code", lines[3]);
//        Log.d("Code", lines[4]);
//        Log.d("Code", lines[5]);
//        Log.d("Code", lines[6]);
//        Log.d("Code", lines[7]);
	}
}
