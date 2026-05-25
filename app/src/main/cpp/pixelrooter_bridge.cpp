#include <jni.h>
#include <dlfcn.h>
#include <string>
#include <android/log.h>
#include <unistd.h>
#include <sys/types.h>

#define TAG "PixelRooter"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Exploit payloads export this symbol. Return 0 on success (running as uid 0).
typedef int (*exploit_fn)(void);

extern "C"
JNIEXPORT jint JNICALL
Java_com_pixelrooter_domain_exploit_ExploitEngine_executeExploitPayload(
        JNIEnv *env,
        jobject /* this */,
        jstring payloadPath) {

    const char *path = env->GetStringUTFChars(payloadPath, nullptr);
    if (!path) {
        LOGE("Failed to get payload path string");
        return -1;
    }

    LOGI("Loading exploit payload: %s", path);

    void *handle = dlopen(path, RTLD_NOW | RTLD_LOCAL);
    if (!handle) {
        LOGE("dlopen failed: %s", dlerror());
        env->ReleaseStringUTFChars(payloadPath, path);
        return -1;
    }

    exploit_fn exploit = reinterpret_cast<exploit_fn>(dlsym(handle, "exploit_main"));
    if (!exploit) {
        LOGE("exploit_main symbol not found: %s", dlerror());
        dlclose(handle);
        env->ReleaseStringUTFChars(payloadPath, path);
        return -2;
    }

    LOGI("Executing exploit payload...");
    int result = exploit();
    LOGI("Exploit returned: %d (uid now: %d)", result, getuid());

    // Do not dlclose — the payload may have set up persistent hooks in the process.
    env->ReleaseStringUTFChars(payloadPath, path);
    return result;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_pixelrooter_domain_exploit_ExploitEngine_getCurrentUid(
        JNIEnv * /* env */,
        jobject /* this */) {
    return static_cast<jint>(getuid());
}
