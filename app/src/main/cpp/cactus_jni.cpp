#include <jni.h>
#include <string>
#include <android/log.h>

#define TAG "CactusJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_agent_airgap_cpp_CactusBridge_processAudio(
    JNIEnv* env,
    jobject /* this */,
    jbyteArray pcmData) {

    jsize length = env->GetArrayLength(pcmData);
    jbyte* buffer = env->GetByteArrayElements(pcmData, nullptr);

    LOGI("Ingested %d bytes of PCM audio for Cactus inference", length);

    // Call into Cactus / Needle 3 runtime pipeline here.
    // Replace this placeholder string with the engine output string.
    std::string resultJson = R"({"name": "confirm_connection", "arguments": {"phone": "6162098213", "name": "Jenna"}})";

    env->ReleaseByteArrayElements(pcmData, buffer, JNI_ABORT);

    return env->NewStringUTF(resultJson.c_str());
}