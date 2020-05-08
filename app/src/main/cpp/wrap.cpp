#include <asm/errno.h>
#include "util.h"
#include "wrap.h"

ssize_t readn(int fd, void* vptr, size_t n) {
    ssize_t nread;
    size_t nleft = n;
    char *ptr = (char *)vptr;
    while (nleft > 0) {
        if ((nread = read(fd, ptr, nleft)) < 0) {
            if (errno == EINTR) {
                LOGE("readn interupt");
                nread = 0;
            } else {
                LOGE("readn erro! nread: %d, errno = %s", nread, strerror(errno));
                return -1;
            }
        }

        nleft -= nread;
        ptr += nread;
    }

    return n - nleft;
}

ssize_t writen(int fd, const void *vptr, size_t n) {
    size_t nleft = n;
    ssize_t nwritten;
    const char *ptr = (const char*) vptr;
    while (nleft > 0) {
        if ((nwritten = write(fd, ptr, nleft)) <= 0) {
            if (nwritten < 0 && errno == EINTR) {
                LOGW("writen interupt!!");
                nwritten = 0;
            } else {
                LOGE("writen error nwritten = %d, errno = %s", nwritten, strerror(errno));
                return -1;
            }
        }

        nleft -= nwritten;
        ptr += nwritten;
    }

    return n;
}


int Socket(int family, int type, int protocol) {
    int n;
    if ((n = socket(family, type, protocol)) < 0) {
        LOGE("socket setup error: %s", strerror(errno));
        exit(0);
    }

    return n;
}

void Connect(int fd, const struct sockaddr *sa, socklen_t salen) {
    if (connect(fd, sa, salen) < 0) {
        LOGE("connect error: %s", strerror(errno));
        exit(0);
    }
}

void Bind(int fd, const struct sockaddr *sa, socklen_t salen) {
    if (bind(fd, sa, salen) < 0) {
        LOGE("bind error: %s", strerror(errno));
        exit(0);
    }
}

int Open(const char* path, int flags) {
    int fd = open(path, flags);
    if (fd < 0) {
        LOGE("open %s error: %s", path, strerror(errno));
        exit(0);
    }
    return fd;
}

void Close(int fd) {
    if (close(fd) == -1) {
        LOGE("close error: %s", strerror(errno));
        exit(0);
    }
}


void Remove(const char* path) {
    if (remove(path) < 0) {
        LOGE("remove %s error: %s", path, strerror(errno));
        exit(0);
    }
}

void Inet_pton(int family, const char* strptr, void *addrptr) {
    int n = inet_pton(family, strptr, addrptr);
    if (n == 0) {
        LOGE("inet pton addr not format");
        exit(0);
    } else if (n < 0) {
        LOGE("inet_pton error for %s", strptr);
        exit(0);
    }
}

void Mkfifo(const char* path, mode_t mode) {
    if (mkfifo(path, mode) < 0) {
        if (errno == EEXIST) {
            LOGW("pipe %s has created before", path);
        } else {
            LOGE("mkfifo %s error: %s", path, strerror(errno));
            exit(0);
        }
    }
}

