#include "util.h"
#include "wrap.h"

//extern "C" JNIEXPORT jstring JNICALL
//Java_com_example_tunnelv6_MainActivity_backend_entry(
//        JNIEnv* env,
//        jobject /* this */) {
//    std::string hello = "Hello from C++";
//    return env->NewStringUTF(hello.c_str());
//}


std::atomic<int> total_read_time;
std::atomic<int> total_read_len;
std::atomic<int> total_write_time;
std::atomic<int> total_write_len;
std::atomic<unsigned long long> keepalive_time;

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
    //LOGI("read len succ: %d", msg.len);
    readn(fd, &msg.type, sizeof(msg.type));
    //LOGI("read type succ: %d", msg.type);
    size_t left = msg.len - offsetof(msg_t, data);
    readn(fd, &msg.data, left);

    /*
    LOGI("msg read succ");
    LOGI("\tlen: %d", msg.len);
    LOGI("\ttype: %d", msg.type);
    LOGI("\tdata: %s", msg.data);
    */
}

void write_msg(int fd, const msg_t& msg) {
    writen(fd, &msg, msg.len);
}

void* timer_worker(void* arg) {
    thread_arg_t* a = (thread_arg_t*)arg;
    int backend_fd = a->backend_fd;
    int sockfd = a->server_fd;
    struct timeval tv;
    unsigned long long last_send = 0;
    char buf[4096] = "";
    int buf_len = 0;
    while (a->running) {
        sleep(1);
        //send info to frontend
        memset(buf, 0, sizeof(buf));
        //upload_time upload_len download_time download_len
        int upload_time = total_read_time;
        int upload_len = total_read_len;
        int download_time = total_write_time;
        int download_len = total_write_len;
        sprintf(buf, "%d %d %d %d", upload_time, upload_len, download_time, download_len);
        buf_len = strlen(buf);
        writen(backend_fd, buf, buf_len);

        unsigned long long last_keepalive = keepalive_time;
        gettimeofday(&tv, NULL);
        unsigned long long current = static_cast<unsigned long long int>(tv.tv_sec);
        if (current - last_keepalive >= 60) {
            LOGW("connect timeout");
            a->running = false;
        } else {
            if (current - last_send >= 20) {
                //send keepalive
                msg_t msg;
                msg.type = 104;
                msg.len = offsetof(msg_t, data);
                write_msg(sockfd, msg);
                last_send = current;
            }
        }
    }
}

void* request_worker(void* arg) {
    thread_arg_t* a = (thread_arg_t*)arg;
    int vpn_fd = a->vpn_fd;
    int sockfd = a->server_fd;


    msg_t msg;
    msg.type = 102;
    while (a->running) {
        int nread = 0;
        if ((nread = read(vpn_fd, msg.data, PAGE_SIZE)) <= 0) {
            if (errno == EAGAIN) continue;
            LOGE("nread < 0??: %s, nread = %d", strerror(errno), nread);
            a->running = false;
            continue;
        }
        total_read_time++;
        total_read_len += nread;
        msg.len = offsetof(msg_t, data) + nread;
        write_msg(sockfd, msg);
        LOGI("102 net request write from vpn read len: %d", nread);
    }
}

void respond_worker(thread_arg_t* arg) {
    int vpn_fd = arg->vpn_fd;
    int sockfd = arg->server_fd;
    msg_t msg;


    while (arg->running) {
        read_msg(sockfd, msg);
        switch (msg.type) {
            case 101: {
                LOGE("101 repeated type: %d", msg.type);
                break;
            };
            case 103: {
                LOGI("103 net respond, len: %d", msg.len - offsetof(msg_t, data));
                size_t len = msg.len - offsetof(msg_t, data);
                total_write_time++;
                total_write_len += len;

                char buf[4096] = "";
                int p = 0;
                for (int i = 0;i < len; ++i) {
                    int n = sprintf(&buf[p], "%02x", msg.data[i]);
                    //LOGW("i = %d, buf: %s", i, buf);
                    p += n;
                }
                LOGW("!!!buf: %s", buf);

                writen(vpn_fd, msg.data, len);
                break;
            }
            case 104: {
                LOGI("104 keepalive");
                struct timeval tv;
                gettimeofday(&tv, NULL);
                unsigned long long t = static_cast<unsigned long long int>(tv.tv_sec);
                keepalive_time = t;
                break;
            }
            default: {
                LOGE("unknown type: %d", msg.type);
                arg->running = false;
                break;
            }
        }
    }
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
    LOGI("connect succ");


    //send IP request
    msg_t msg;
    msg.type = 100;
    msg.len = offsetof(msg_t, data);
    write_msg(sockfd, msg);
    LOGI("write succ, msg.len: %d", msg.len);

    //receive IP reply
    read_msg(sockfd, msg);
    LOGI("IP reply received");


    //send it to frontend
    char buf[PAGE_SIZE] = "";
    size_t buf_len = 0;
    strcpy(buf, msg.data);
    buf_len = strlen(msg.data);

    sprintf(buf + buf_len, " %d\0", sockfd);
    buf_len = strlen(buf);

    LOGI("buf: %s", buf);
    writen(backend_fd, buf, buf_len);

    LOGI("write pip done");

    //receive vpn_fd from frontend
    //sleep(2);
    int vpn_fd = -1;
    LOGI("prepare to read vpn_fd");
    //readn(frontend_fd, &vpn_fd, sizeof(int));
    int nread = 0;
    nread = read(frontend_fd, &vpn_fd, sizeof(int));

    //sleep(5);
    LOGI("receive vpn_fd: %d, nread: %d", vpn_fd, nread);
    //return;


    //global init
    total_write_len = 0;
    total_write_time = 0;
    total_read_len = 0;
    total_read_time = 0;
    struct timeval tv;
    gettimeofday(&tv, NULL);
    keepalive_time = tv.tv_sec;


    thread_arg_t arg;
    arg.running = true;
    arg.server_fd = sockfd;
    arg.vpn_fd = vpn_fd;
    arg.frontend_fd = frontend_fd;
    arg.backend_fd = backend_fd;
    pthread_t request_thread, timer_thread;

    pthread_create(&request_thread, NULL, request_worker, &arg);
    pthread_create(&timer_thread, NULL, timer_worker, &arg);
    respond_worker(&arg);

    pthread_join(request_thread, NULL);
    pthread_join(timer_thread, NULL);

    //Close(sockfd);
    //Close(vpn_fd);
    //Close(backend_fd);
    //Close(frontend_fd);
}