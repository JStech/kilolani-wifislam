#include <jni.h>
#include <string>

extern "C"
jstring
Java_net_stechschulte_kilolani_Navigate_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
