LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#OPENCV_CAMERA_MODULES:=off
#OPENCV_INSTALL_MODULES:=off
#OPENCV_LIB_TYPE:=SHARED
include ../../soft/OpenCV-2.4.9-android-sdk/sdk/native/jni/OpenCV.mk
LOCAL_SRC_FILES  := HOGBasedSvm_jni.cpp DetectionBasedTracker_jni.cpp 

LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_LDLIBS     += -llog -ldl

LOCAL_MODULE     := detection_based_tracker

include $(BUILD_SHARED_LIBRARY)
