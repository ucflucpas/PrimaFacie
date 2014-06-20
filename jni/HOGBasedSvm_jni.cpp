/*
 * HOGBasedSvm_jni.cpp
 *
 *  Created on: Jun 7, 2014
 *      Author: developer
 */


#include "HOGBasedSvm_jni.h"
#include <opencv2/core/core.hpp>
#include <opencv2/contrib/detection_based_tracker.hpp>
#include <opencv2/objdetect/objdetect.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/ml/ml.hpp>
#include <string.h>

#include <string>
#include <vector>

#include <android/log.h>

#define LOG_TAG "FaceDetection/HogBasedSVM"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))

using namespace std;
using namespace cv;




JNIEXPORT jlong JNICALL Java_org_opencv_samples_facedetect_HOGBasedSvm_nativeCreateHog
(JNIEnv * jenv, jclass, jstring jFileName)
{
    LOGD("nativeCreateObject enter");
	jlong obj = 0;
	const char* jnamestr = jenv->GetStringUTFChars(jFileName, NULL);
	obj = (jlong)new CvSVM();

	((CvSVM*)obj)->load(jnamestr);
	 LOGD("nativeCreateObject exit");
	return obj;

}


JNIEXPORT jfloat JNICALL Java_org_opencv_samples_facedetect_HOGBasedSvm_nativePredict
(JNIEnv * jenv, jclass,jlong thiz, jlong imgGrayAddr)
{
	try
	{
	LOGD("---NATIVE PREDICTION 0---");
	// between here
	vector<float> 								mHogDescriptors;
	vector<float>::iterator 					it;

	float response;

	//the input image
	Mat& imgGray = *(Mat*)imgGrayAddr;


	//gets the hogDescriptor for that area.
	HOGDescriptor mHog;


	Mat resizedImg;

	resize(imgGray, resizedImg, Size(64,128));

	mHog.compute(resizedImg, mHogDescriptors);

	Mat testMat(1,mHogDescriptors.size(),CV_32FC1);

	LOGD("---NATIVE PREDICTiON 1---");

	if (testMat.total() == mHogDescriptors.size() ){
		LOGD("testMat and mHogDescriptors are the same size.");
	}

	//convert descriptors to mat


	//memcopy(testMat.data, mHogDescriptors.data(), mHogDescriptors.size()*sizeof(float));

	int j = 0;

	for (it = mHogDescriptors.begin(); it != mHogDescriptors.end(); it++, j++){
			testMat.at<float>(0,j) = (float)(*it);
			//testMat.push_back<float>((float)*it);

			if (j == mHogDescriptors.size()-1){
				LOGD("EXIT LOOP");
			}
	}

	mHogDescriptors.clear();






	LOGD("---NATIVE PREDICTION 2---");
	response = ((CvSVM*)thiz)->predict(testMat);

	jfloat result = (jfloat)response;

	imgGray.release();
	LOGD("---NATIVE PREDICTION 3---");
	return result;


	}
	catch(cv::Exception& e)
	 {
	     LOGD("nativeCreateObject caught cv::Exception: %s", e.what());
	     jclass je = jenv->FindClass("org/opencv/core/CvException");
	     if(!je)
	         je = jenv->FindClass("java/lang/Exception");
	     jenv->ThrowNew(je, e.what());
	 }
	 catch (...)
	 {
	     LOGD("nativeHogPredict caught unknown exception");
	     jclass je = jenv->FindClass("java/lang/Exception");
	     jenv->ThrowNew(je, "Unknown exception in JNI code of DetectionBasedTracker.nativePredict()");
	     return 0;
	 }

}


