#include "util.h"
#include "wrap.h"

//extern "C" JNIEXPORT jstring JNICALL
//Java_com_example_tunnelv6_MainActivity_backend_entry(
//        JNIEnv* env,
//        jobject /* this */) {
//    std::string hello = "Hello from C++";
//    return env->NewStringUTF(hello.c_str());
//}

void dir_init(const char* dir) {
    snprintf(IP_INFO_BACKEND, sizeof(IP_INFO_BACKEND), "%s/%s", dir, IP_INFO_BACKEND_NAME);
    snprintf(IP_INFO_FRONTEND, sizeof(IP_INFO_FRONTEND), "%s/%s", dir, IP_INFO_FRONTEND_NAME);
}

void pipe_init() {
    Mkfifo(IP_INFO_BACKEND, 0666);
    Mkfifo(IP_INFO_FRONTEND, 0666);
}

void read_msg(int fd, msg_t& msg) {
    memset(&msg, 0, sizeof(msg_t));

    readn(fd, &msg.len, sizeof(msg.len));
    readn(fd, &msg.type, sizeof(msg.type));
    size_t left = msg.len - sizeof(msg.len) - sizeof(msg.type);
    readn(fd, &msg.data, left);

    LOGI("msg read succ");
    LOGI("\tlen: %d", msg.len);
    LOGI("\ttype: %d", msg.type);
    LOGI("\tdata: %s", msg.data);
}

void write_msg(int fd, const msg_t& msg) {
    writen(fd, &msg, msg.len);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_tunnelv6_MainActivity_backend_1entry(JNIEnv *env, jobject thiz, jstring dir) {
    LOGI("into backend entry");

    dir_init(env->GetStringUTFChars(dir, NULL));
    pipe_init();

    int backend_fd = Open(IP_INFO_BACKEND, O_RDWR | O_CREAT | O_TRUNC);
    int frontend_fd = Open(IP_INFO_FRONTEND, O_RDWR | O_CREAT | O_TRUNC);

    int sockfd;
    struct sockaddr_in6 server_addr;
    sockfd = Socket(AF_INET6, SOCK_STREAM, 0);
    server_addr.sin6_family = AF_INET6;
    server_addr.sin6_port = htons(SERVER_PORT);
    Inet_pton(AF_INET6, SERVER_IPV6, &server_addr.sin6_addr);
    Connect(sockfd, (struct sockaddr*)&server_addr, sizeof(struct sockaddr_in6));


    //send IP request
    msg_t msg;
    msg.type = 100;
    msg.len = sizeof(msg);
    write_msg(sockfd, msg);

    //receive IP reply
    read_msg(sockfd, msg);


    //send it to frontend
    char buf[PAGE_SIZE] = "";
    size_t buf_len = 0;
    strcpy(buf, msg.data);
    buf_len = strlen(msg.data);

    sprintf(buf + buf_len - 1, " %d\0", sockfd);
    buf_len = strlen(buf);

    LOGI("buf: %s", buf);
    writen(backend_fd, buf, buf_len);

    LOGI("write pip done");
    Close(sockfd);
}