LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#opencv
OPENCVROOT:= C:\Android\OpenCV-android-sdk
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
OPENCV_LIB_TYPE:=SHARED
include ${OPENCVROOT}/sdk/native/jni/OpenCV.mk

LOCAL_SRC_FILES := DetectionBasedTracker_jni.cpp

LOCAL_LDLIBS += -llog
LOCAL_MODULE := detection_based_tracker


include $(BUILD_SHARED_LIBRARY)