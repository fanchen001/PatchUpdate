LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := patch
LOCAL_CXXFLAGS :=
LOCAL_C_INCLUDES := $(LOCAL_PATH)
LOCAL_SRC_FILES := com_fanchen_update_jni_PatchUpdate.c
LOCAL_LDLIBS := -lz -llog
include $(BUILD_SHARED_LIBRARY)

