#include <jni.h>
#include <string>

#include <android/log.h>

#define TAG "[TEST_TAG]"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <errno.h>

#include "wrapsock.h"

//extern "C" JNIEXPORT jstring JNICALL
//Java_com_example_tunnerv6_MainActivity_backend_entry(
//        JNIEnv* env,
//        jobject /* this */) {
//    std::string hello = "Hello from C++";
//    return env->NewStringUTF(hello.c_str());
//}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_tunnerv6_MainActivity_backend_1entry(JNIEnv *env, jobject thiz) {
    // TODO: implement backend_entry()
    LOGI("into backend entry");
    int sockfd;
    struct sockaddr_in6 server_addr;
    sockfd = socket(AF_INET6, SOCK_STREAM, 0);
    if (sockfd == -1) {
        LOGE("socket not setup");
        //exit(0);
        return;
    }

    memset(&server_addr, 0, sizeof(server_addr));
    server_addr.sin6_family = AF_INET6;
    server_addr.sin6_port = htons(38324);

    int n = inet_pton(AF_INET6, "2402:f000:4:72:808::4016", &server_addr.sin6_addr);
    if (n == 0) {
        LOGE("addr not format");
        //exit(0);
        return;
    } else if (n == -1){
        LOGE("inet_pton error");
        //exit(0);
        return;
    }

    if (connect(sockfd, (struct sockaddr*)&server_addr, sizeof(struct sockaddr_in6)) == -1) {
        LOGE("connect error");
        //exit(0);
        return;
    }

    LOGI("connect success");
    char test1[] = "test1 hello world\n";
    char test2[] = "test2\n";
    char test3[] = "test3\n";

    char buff[4096] = "";
    //echo test

    sscanf(test1, "%s", buff);
    ssize_t len = strlen(buff);
    char *ptr = (char*) &len;
    size_t nleft = sizeof(ssize_t);
    ssize_t nwritten;
    while (nleft > 0) {
        if ((nwritten = write(sockfd, ptr, nleft)) <= 0) {
            if (nwritten < 0 && errno == EINTR) nwritten = 0;
            else {
                LOGE("write error");
                return;
            }
        }

        nleft -= nwritten;
        ptr += nwritten;
    }

    nleft = len;
    ptr = buff;
    while (nleft > 0) {
        if ((nwritten = write(sockfd, ptr, nleft)) <= 0) {
            if (nwritten < 0 && errno == EINTR) nwritten = 0;
            else {
                LOGE("write error");
                return;
            }
        }

        nleft -= nwritten;
        ptr += nwritten;
    }

    LOGI("{test1 hello world}  sended");

    //read
    ptr = (char*) &len;
    nleft = sizeof(ssize_t);
    ssize_t nread;
    while (nleft > 0) {
        if ((nread = read(sockfd, ptr, nleft)) < 0) {
            if (errno == EINTR) nread =0;
            else {
                LOGE("read error");
                return;
            }
        } else if (nread == 0) {
            break;
        }

        nleft -= nread;
        ptr += nread;
    }

    LOGI("receive len = %d", len);
    ptr = buff;
    nleft = len;
    while (nleft > 0) {
        if ((nread = read(sockfd, ptr, nleft)) < 0) {
            if (errno == EINTR) nread =0;
            else {
                LOGE("read error");
                return;
            }
        } else if (nread == 0) {
            break;
        }

        nleft -= nread;
        ptr += nread;
    }
    LOGI("receive buff = %s\n", buff);
}