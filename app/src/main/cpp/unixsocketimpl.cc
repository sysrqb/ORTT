#include <sys/socket.h>
#include <sys/un.h>
#include <string.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <jni.h>

extern "C"
JNIEXPORT jint JNICALL
Java_sysrqb_ortt_ORTTUnixSocket_connectImpl(JNIEnv* env, jobject,
                                                 jstring path) {
    struct sockaddr_un sun;

    printf("This is a test from JNI\n");
    const char *c_path = env->GetStringUTFChars(path, 0);
    if (c_path == NULL)
        return -1;

    memcpy(&sun.sun_path, c_path, strlen(c_path));
    env->ReleaseStringUTFChars(path,c_path);
    int fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (fd == -1)
        return -2;

    if (connect(fd, (struct sockaddr *) &sun, sizeof(sun)) == 0) {
        return fd;
    }
    return -3;
}

const int currentfd(JNIEnv* env, jobject obj) {
    jfieldID fdfield = env->GetFieldID(env->GetObjectClass(obj), "fd", "private int");
    return env->GetIntField(obj, fdfield);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_sysrqb_ortt_ORTTUnixSocket_writeImpl(JNIEnv* env, jobject obj,
                                               jbyte buf[],
                                               jint size) {
    const int fd = currentfd(env, obj);
    size_t c_size;
    if (size < 0) {
        size = 0;
    }
    c_size = (size_t) size;
    return write(fd, buf, c_size);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_sysrqb_ortt_ORTTUnixSocket_readImpl(JNIEnv* env, jobject obj,
                                               jbyte buf[],
                                               jint size) {
    const int fd = currentfd(env, obj);
    size_t c_size;
    if (size < 0) {
        size = 0;
    }
    c_size = (size_t) size;
    return read(fd, buf, c_size);
}