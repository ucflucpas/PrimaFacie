package org.opencv.samples.facedetect;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.util.Log;


public class HOGBasedSvm {
	
	private double mNativeObj = 0;
	public float result = 0;
	
	
	public HOGBasedSvm(String cascadeName){
		mNativeObj = nativeCreateHog(cascadeName);
	}
	
	public void predict(Mat imgGray){
		Mat img = imgGray.clone();
		result =nativePredict(mNativeObj, img.getNativeObjAddr());
		
		
	}
	
    private static native double nativeCreateHog(String jFileName);

    private static native long nativePredict(double thiz, long imgGrayAddr);
}
