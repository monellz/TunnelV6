#ifndef tunnelV6_UTIL_H
#define tunnelV6_UTIL_H


#include <jni.h>
#include <android/log.h>

#include <stdlib.h>
#include <string.h>
#include <pthread.h>


#define TAG "[TEST_TAG from c++]"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)


#define IP_INFO_BACKEND_NAME "ip_info_backend"
#define IP_INFO_FRONTEND_NAME "ip_info_frontend"
extern char IP_INFO_BACKEND[128];
extern char IP_INFO_FRONTEND[128];

#define OFFICIAL

#ifdef OFFICIAL
#define SERVER_IPV6 "2402:f000:4:72:808::9a47"
#define SERVER_PORT 5678
#else
#define SERVER_IPV6 "2402:f000:4:72:808::4016"
#define SERVER_PORT 38324
#endif


struct msg_t {
    int len;
    char type;
    char data[4096];
};

#endif //tunnelV6_UTIL_H
