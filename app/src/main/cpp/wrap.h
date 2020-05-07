#ifndef tunnelV6_WRAP_H
#define tunnelV6_WRAP_H

#include <sys/socket.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/un.h>
#include <stdio.h>
#include <netinet/in.h>
#include <fcntl.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <errno.h>

int Socket(int family, int type, int protocol);
ssize_t readn(int fd, void* vptr, size_t n);
ssize_t writen(int fd, const void *vptr, size_t n);
void Inet_pton(int family, const char* strptr, void *addrptr);
void Connect(int fd, const struct sockaddr *sa, socklen_t salen);
void Bind(int fd, const struct sockaddr *sa, socklen_t salen);
int Open(const char* path, int flags);
void Remove(const char* path);
void Close(int fd);
void Mkfifo(const char* path, mode_t mode);
#endif //tunnelV6_WRAP_H
