///
///
///	Prima Facie 7
///

package org.opencv.samples.facedetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

public class FdActivity extends Activity implements CvCameraViewListener2 {

    private static final String    TAG                 = "PFV5::Activity";
    private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    private static final Scalar	   MOUTH_RECT_COLOR    = new Scalar(255,0,0,255);
    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;
   
    public static final double	   TEXTSIZE			   =1.75;
    
    private static int			   TIER				   = 0;
    
    public static boolean          DETECT_MOUTH		   = false; 			// logic for when detection is falling through switch.
   
    private Mat                    mRgba;
    private Mat                    mGray;
    private Mat					   ROI;
    private File                   mFile;
    
    private DetectionBasedTracker  mLBPFrontalFace;
    private DetectionBasedTracker  mHaarMouth;
    
    private HOGBasedSvm			   mHogHappy;
    private HOGBasedSvm			   mHogSuprise;

    private MenuItem			   selectCamera;
    private int 				   mCameraIndex = 1;
    float 						   result = 0;
    
    private int					   runThisFrame = 0;

    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize   = 0;

    private CameraBridgeViewBase   mOpenCvCameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("detection_based_tracker");
                   

                    try {
                        // LOAD TRAINED MODELS
                    	
                    	//load lbo frontal face
                    	InputStream is;
                    	FileOutputStream os;
                    	File cascadeDir;
                    	
                        is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        
                        cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        os = new FileOutputStream(mFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ( (bytesRead = is.read(buffer) ) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();
                        
                        mLBPFrontalFace = new DetectionBasedTracker(mFile.getAbsolutePath(), 0);
                      
                        
                        //load haar mouth cascade
                        is = getResources().openRawResource(R.raw.haarcascade_mcs_mouth);
                        
                        cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mFile = new File(cascadeDir, "haarcascade_mcs_mouth.xml");
                        os = new FileOutputStream(mFile);

                        buffer = new byte[4096];
                        
                       
                        while ( (bytesRead = is.read(buffer) ) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();
                        
                        mHaarMouth = new DetectionBasedTracker(mFile.getAbsolutePath(), 0);
                        
                        //load trained hog svm for happy
                        is = getResources().openRawResource(R.raw.hogsvm);
                        
                        cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mFile = new File(cascadeDir, "hogsvm");
                        os = new FileOutputStream(mFile);

                        buffer = new byte[4096];
                        
                       
                        while ( (bytesRead = is.read(buffer) ) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();
                        
                        mHogHappy = new HOGBasedSvm(mFile.getAbsolutePath());
                        
                      //load trained hog svm for suprised
                        is = getResources().openRawResource(R.raw.suprise_hog_mouth);
                        
                        cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mFile = new File(cascadeDir, "suprise_hog_mouth");
                        os = new FileOutputStream(mFile);

                        buffer = new byte[4096];
                        
                       
                        while ( (bytesRead = is.read(buffer) ) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();
                        
                        mHogSuprise = new HOGBasedSvm(mFile.getAbsolutePath());
                       
                        
                        // done loading files.. delete cascade directory
                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }
                    //mOpenCvCameraView.setCameraIndex(mCameraIndex);
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

    }


	@Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

    	if (mGray.empty()){
    		mGray = new Mat();
    	}
    	if (mRgba.empty()){
    		mRgba = new Mat();
    	}

        // this only happens on the first run... probably not a good way of doing this.
        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            mLBPFrontalFace.setMinFaceSize(mAbsoluteFaceSize);
        }
        

        
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        // initiate new Mats to hold new faces/mouths
        
        MatOfRect faces = new MatOfRect();
        MatOfRect mouths = new MatOfRect();       	
        	
        mLBPFrontalFace.detect(mGray, faces);
        	
       
        Rect mouthRect;
        
	        
	      
        //runs for each face found
	       
        Rect[] facesArray = faces.toArray();
	    
        for (int i = 0; i < facesArray.length; i++){
	    
        	//draw a rectangle around that face	        
        	Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
	               		        
        	Point infoSqr = new Point(	        
        			(int)(facesArray[i].br().x + 30),	        		
        			(int)(facesArray[i].br().y-(facesArray[i].height/2))	        		
        			);
	        	
        	
	        
        	Core.putText(mRgba, "Face has been detected.", infoSqr, 	        
        			Core.FONT_HERSHEY_COMPLEX_SMALL, TEXTSIZE, new Scalar(0,200,0), 4);
	        	       		              		        		        
        	// The mouth would only be in the lower half of the face, so create a rectangle	        
        	//around that area		    
        			    
        	switch (TIER){		            	
        		case 0:				// HAPPY	            			        
        			//look for mouth
		            
        			mouthRect = new Rect(  							            
        					new Point ((int)(facesArray[i].tl().x), (int)(facesArray[i].tl().y+facesArray[i].height/2)),		            		
        					facesArray[i].br()			            		
        					);
					        
        			// that rectangle is the region of interest.					
        			ROI = new Mat(mGray, facesArray[i]);					
       					
        			//find mouths in that area					
        			mHaarMouth.detect(ROI, mouths);					       								        	
					
        			//runs for each mouth found in that face...should only be once
					
        			Rect[] mouthArray = mouths.toArray();
        			
        			if (mouths.empty()){
        				Core.putText(mRgba, "Mouth not detected.", new Point(infoSqr.x, infoSqr.y+60), 				    	
        						Core.FONT_HERSHEY_COMPLEX_SMALL, TEXTSIZE, new Scalar(0,200,0), 4);	
        				
        			} else {
        				Core.putText(mRgba, "Mouth Detected.", new Point(infoSqr.x, infoSqr.y+60), 				    	
        						Core.FONT_HERSHEY_COMPLEX_SMALL, TEXTSIZE, new Scalar(0,200,0), 4);
        			}
					
        			for (int j = 0; j < mouthArray.length; j++){					
        				//draw rectangle					    
        				//correct for offeset					    
        				Rect mouthOffset = new Rect (					    
        						new Point((int)(mouthArray[j].tl().x+mouthRect.tl().x),(int)(mouthArray[j].tl().y+mouthRect.tl().y)) , 					            
        						new Point((int)(mouthArray[j].br().x+mouthRect.tl().x),(int)(mouthArray[j].br().y+mouthRect.tl().y))					            
        						);
					            
        				Core.rectangle(mRgba, 					    
        						//correct for offset in ROI					            
        						new Point((int)(mouthArray[j].tl().x+mouthRect.tl().x),(int)(mouthArray[j].tl().y+mouthRect.tl().y)), 					            
        						new Point((int)(mouthArray[j].br().x+mouthRect.tl().x),(int)(mouthArray[j].br().y+mouthRect.tl().y)),				            
        						MOUTH_RECT_COLOR, 				            
        						3);
					            	
					            
        				//this is the region of interest					    
        				ROI = new Mat(mGray, facesArray[i]);
					          									           									    
        								            					            			
					            
        				//predict happy
					    
        				mHogHappy.predict(ROI);
        				
        				if (mHogHappy.result != 0){		//happy detected	            
            				//write happy on the screen		            	
            				Core.putText(mRgba, "Emotion found:     HAPPY", new Point(infoSqr.x, infoSqr.y+40), 				    	        		    
            						Core.FONT_HERSHEY_COMPLEX_SMALL, TEXTSIZE, new Scalar(0,200,0), 4);	            	
            				TIER = 0;		            	
            				break;		            

            			} else {	            
            				//step down		
            				DETECT_MOUTH = false;
            				TIER = 1;		            	        						            	
            			}
					    
        			}					            		            
        			
		            				            
        		case 1:	//find if suprised.	    
        			if (DETECT_MOUTH){				//no need to redetect mouth if falling through switch... this is already done for this frame
        				
        				mouthRect = new Rect(  							            
            					new Point ((int)(facesArray[i].tl().x), (int)(facesArray[i].tl().y+facesArray[i].height/2)),		            		
            					facesArray[i].br()			            		
            					);
        				
        				//find mouths in that area					
            			mHaarMouth.detect(ROI, mouths);					       								        	
    					
            			//runs for each mouth found in that face...should only be once
    					
            			mouthArray = mouths.toArray();
            			
            			if (mouths.empty()){
            				Core.putText(mRgba, "Mouth not detected.", new Point(infoSqr.x, infoSqr.y+60), 				    	
            						Core.FONT_HERSHEY_COMPLEX_SMALL, TEXTSIZE, new Scalar(0,200,0), 4);	
            				
            			} else {
            				Core.putText(mRgba, "Mouth Detected.", new Point(infoSqr.x, infoSqr.y+60), 				    	
            						Core.FONT_HERSHEY_COMPLEX_SMALL, TEXTSIZE, new Scalar(0,200,0), 4);
            			}
            			
        				for (int j = 0; j < mouthArray.length; j++){					
            				//draw rectangle					    
            				//correct for offeset					    
            				Rect mouthOffset = new Rect (					    
            						new Point((int)(mouthArray[j].tl().x+mouthRect.tl().x),(int)(mouthArray[j].tl().y+mouthRect.tl().y)) , 					            
            						new Point((int)(mouthArray[j].br().x+mouthRect.tl().x),(int)(mouthArray[j].br().y+mouthRect.tl().y))					            
            						);
    					            
            				Core.rectangle(mRgba, 					    
            						//correct for offset in ROI					            
            						new Point((int)(mouthArray[j].tl().x+mouthRect.tl().x),(int)(mouthArray[j].tl().y+mouthRect.tl().y)), 					            
            						new Point((int)(mouthArray[j].br().x+mouthRect.tl().x),(int)(mouthArray[j].br().y+mouthRect.tl().y)),				            
            						MOUTH_RECT_COLOR, 				            
            						3);
    					            	
    					            
            				//this is the region of interest					    
            				ROI = new Mat(mGray, mouthOffset);
        				}
        			}
        			
        			mHogSuprise.predict(ROI);
        			if (mHogSuprise.result != 0){
        				//suprise is found
        				//write happy on the screen		            	
        				Core.putText(mRgba, "Emotion found:     SUPRISED", new Point(infoSqr.x, infoSqr.y+40), 				    	        		    
        						Core.FONT_HERSHEY_COMPLEX_SMALL, TEXTSIZE, new Scalar(0,200,0), 4);
        				
        				TIER = 1;			//just to be sure
        				DETECT_MOUTH = true;
        				break;
        				
        			}else {
        				//step down
        				TIER = 2;
        			}
        		default:
        			Core.putText(mRgba, "Emotion found:     ", new Point(infoSqr.x, infoSqr.y+40), 				    	        		    
    						Core.FONT_HERSHEY_COMPLEX_SMALL, TEXTSIZE, new Scalar(0,200,0), 4);
        			TIER = 0;
        			break;
		            			
					
        	}	            			        			            			            	
		    
        }		        	    	        	

        return mRgba;
    }
    
/*    //Create Menu Option and Handle Input for switching between Cameras.
   
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
    	Log.i(TAG, "called onCreateOptionsMenu");
    	selectCamera = menu.add("Switch Camera");
    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
    	//could not find way to query the current camera index from OpenCvCameraView
    	//using bitwise not to turn 0 to 1
    	
    	if (item == selectCamera){
	    	mCameraIndex = mCameraIndex^1;	//turns 0 to 1 and 1 to 0;
	    	
	    	mOpenCvCameraView.disableView();
	    	mOpenCvCameraView.setCameraIndex(mCameraIndex);
	    	mOpenCvCameraView.enableView();
    	}
    	
    	return true;
    }*/


}
