
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_STATIC_JAVA_LIBRARIES := helmet_protobuf
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v4

LOCAL_STATIC_JAVA_LIBRARIES += vendor.mediatek.hardware.nvram-V1.0-java-static

LOCAL_JAVA_LIBRARIES := \
    telephony-common

LOCAL_JNI_SHARED_LIBRARIES := libmp3lame
LOCAL_PACKAGE_NAME := SmartHelmet
LOCAL_CERTIFICATE := platform
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_PROGUARD_ENABLED := disabled
LOCAL_PRIVILEGED_MODULE := true
LOCAL_OVERRIDES_PACKAGES := Home Launcher2 Launcher3 MtkLauncher3
include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := helmet_protobuf:libs/protobuf-java-2.4.1.jar
include $(BUILD_MULTI_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := libmp3lame
LOCAL_MODULE_TAGS := optional
ifeq ($(TARGET_ARCH), arm64)
LOCAL_SRC_FILES := libs/arm64-v8a/libmp3lame.so
LOCAL_MULTILIB = 64
else
LOCAL_SRC_FILES := libs/armeabi-v7a/libmp3lame.so
LOCAL_MULTILIB = 32
endif
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_SUFFIX = .so
include $(BUILD_PREBUILT)
