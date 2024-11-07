package top.dreamlike.panama.uring.nativelib.libs;

import top.dreamlike.panama.generator.annotation.NativeFunction;
import top.dreamlike.panama.generator.annotation.Pointer;

import java.lang.foreign.MemorySegment;

public interface Libc {
    int open(MemorySegment pathname, int flags);

    int read(int fd, MemorySegment buf, int count);

    int write(int fd, MemorySegment buf, int count);

    int close(int fd);

    int socket(int domain, int type, int protocol);

    int eventfd(int initVal, int flags);

    int eventfd_read(int fd,/* uint64_t* value  */@Pointer MemorySegment value);

    int eventfd_write(int fd, long value);

    @NativeFunction(value = "__errno_location")
    MemorySegment errorNo();

    int dup(int fd);

    MemorySegment strerror(int errno);

    int fcntl(int fd, int cmd, int arg);

    int bind(int fd, @Pointer MemorySegment addr, int addrlen);

    int listen(int socketFd, int backlog);

    int connect(int socketFd,@Pointer MemorySegment addr, int addrLen);

    int getsockname(int socketFd, @Pointer MemorySegment addr, @Pointer MemorySegment addrlen);

    int getpeername(int socketFd, @Pointer MemorySegment addr, @Pointer MemorySegment addrlen);

    int pipe(@Pointer MemorySegment pipeFd);

    int setsockopt(int sockfd, int level, int optname, @Pointer MemorySegment optval, int optlen);

    int getpagesize();

    int inotify_init();

    int inotify_init1(int flags);

    int inotify_add_watch(int fd, @Pointer MemorySegment name, int mask);

    int inotify_rm_watch (int fd, int wd);

    interface Inotify_H {
        int IN_NONBLOCK = 2048;
        int IN_CLOSEXEC = 524288;

        /* Supported events suitable for MASK parameter of INOTIFY_ADD_WATCH.  */
        int IN_ACCESS = 0x00000001;/* File was accessed.  */
        int IN_MODIFY = 0x00000002;/* File was modified.  */
        int IN_ATTRIB = 0x00000004;/* Metadata changed.  */
        int IN_CLOSE_WRITE = 0x00000008;/* Writtable file was closed.  */
        int IN_CLOSE_NOWRITE = 0x00000010;/* Unwrittable file closed.  */
        int IN_CLOSE = (IN_CLOSE_WRITE | IN_CLOSE_NOWRITE);/* Close.  */
        int IN_OPEN = 0x00000020;/* File was opened.  */
        int IN_MOVED_FROM = 0x00000040;/* File was moved from X.  */
        int IN_MOVED_TO = 0x00000080;/* File was moved to Y.  */
        int IN_MOVE = (IN_MOVED_FROM | IN_MOVED_TO);/* Moves.  */
        int IN_CREATE = 0x00000100;/* Subfile was created.  */
        int IN_DELETE = 0x00000200;/* Subfile was deleted.  */
        int IN_DELETE_SELF = 0x00000400;/* Self was deleted.  */
        int IN_MOVE_SELF = 0x00000800;/* Self was moved.  */

        /* Events sent by the kernel.  */
        int IN_UNMOUNT = 0x00002000;/* Backing fs was unmounted.  */
        int IN_Q_OVERFLOW = 0x00004000;/* Event queued overflowed.  */
        int IN_IGNORED = 0x00008000;/* File was ignored.  */


        /* Special flags.  */
        int IN_ONLYDIR = 0x01000000;/* Only watch the path if it is a
					   directory.  */
        int IN_DONT_FOLLOW = 0x02000000;/* Do not follow a sym link.  */
        int IN_EXCL_UNLINK = 0x04000000;/* Exclude events on unlinked
					   objects.  */
        int IN_MASK_CREATE = 0x10000000;/* Only create watches.  */
        int IN_MASK_ADD = 0x20000000;/* Add to the mask of an already
					   existing watch.  */
        int IN_ISDIR = 0x40000000;/* Event occurred against dir.  */
        int IN_ONESHOT = 0x80000000;/* Only send event once.  */
    }


    interface Fcntl_H {
        static final int O_RDONLY = 0;
        static final int O_WRONLY = 1;
        static final int O_RDWR = 2;
        static final int O_CREAT = 64;
        static final int O_EXCL = 128;
        static final int O_TURNC = 512;
        static final int O_APPEND = 1024;
        static final int O_NONBLOCK = 2048;
        static final int O_SYNC = 1052672;
        static final int O_ASYNC = 8192;
        static final int O_DIRECT = 16384;
        static final int O_DIRECTORY = 65536;
        static final int O_NOFOLLOW = 131072;
        static final int O_CLOEXEC = 524288;
        static final int O_PATH = 2097152;

        static final int SPLICE_F_MOVE = 1;
        static final int SPLICE_F_NONBLOCK = 2;
        static final int SPLICE_F_MORE = 4;
        static final int SPLICE_F_GIFT = 8;

        static final int F_DUPFD = 0;
        ;/* Duplicate file descriptor.  */
        static final int F_GETFD = 1;
        ;/* Get file descriptor flags.  */
        static final int F_SETFD = 2;
        ;/* Set file descriptor flags.  */
        static final int F_GETFL = 3;
        ;/* Get file status flags.  */
        static final int F_SETFL = 4;
        ;/* Set file status flags.  */
    }

    interface Socket_H {
        interface Domain {
            static final int AF_UNIX = 1;
            static final int AF_LOCAL = 1;
            static final int AF_INET = 2;
            static final int AF_AX25 = 3;
            static final int AF_IPX = 4;
            static final int AF_APPLETALK = 5;
            static final int AF_X25 = 9;
            static final int AF_INET6 = 10;
            static final int AF_DECnet = 12;
            static final int AF_KEY = 15;
            static final int AF_NETLINK = 16;
            static final int AF_PACKET = 17;
            static final int AF_RDS = 21;
            static final int AF_PPPOX = 24;
            static final int AF_LLC = 26;
            static final int AF_IB = 27;
            static final int AF_MPLS = 28;
            static final int AF_CAN = 29;
            static final int AF_TIPC = 30;
            static final int AF_BLUETOOTH = 31;
            static final int AF_IUCV = 32;
            static final int AF_RXRPC = 33;
            static final int AF_ISDN = 34;
            static final int AF_PHONET = 35;
            static final int AF_IEEE802154 = 36;
            static final int AF_CAIF = 37;
            static final int AF_ALG = 38;
            static final int AF_NFC = 39;
            static final int AF_VSOCK = 40;
            static final int AF_KCM = 41;
            static final int PF_QIPCRTR = 42;
            static final int AF_SMC = 43;
            static final int AF_XDP = 44;
            static final int AF_MCTP = 45;
            static final int AF_MAX = 46;

        }

        interface Type {
            static final int SOCK_STREAM = 1;
            static final int SOCK_DGRAM = 2;
            static final int SOCK_RAW = 3;
            static final int SOCK_RDM = 4;
            static final int SOCK_SEQPACKET = 5;
            static final int SOCK_DCCP = 6;
            static final int SOCK_PACKET = 10;
            static final int SOCK_CLOEXEC = 02000000;
            static final int SOCK_NONBLOCK = 04000;
        }

        interface SetSockOpt {
            static final int SOL_SOCKET = 1;
        }

        interface OptName {
            //       ;/* For setsockopt(2) */
            int SOL_SOCKET = 1;
            int SO_DEBUG = 1;
            int SO_REUSEADDR = 2;
            int SO_TYPE = 3;
            int SO_ERROR = 4;
            int SO_DONTROUTE = 5;
            int SO_BROADCAST = 6;
            int SO_SNDBUF = 7;
            int SO_RCVBUF = 8;
            int SO_SNDBUFFORCE = 32;
            int SO_RCVBUFFORCE = 33;
            int SO_KEEPALIVE = 9;
            int SO_OOBINLINE = 10;
            int SO_NO_CHECK = 11;
            int SO_PRIORITY = 12;
            int SO_LINGER = 13;
            int SO_BSDCOMPAT = 14;
            int SO_REUSEPORT = 15;
            int SO_PASSCRED = 16;
            int SO_PEERCRED = 17;
            int SO_RCVLOWAT = 18;
            int SO_SNDLOWAT = 19;
            int SO_RCVTIMEO_OLD = 20;
            int SO_SNDTIMEO_OLD = 21;
        }

        interface Flag {
            int MSG_DONTWAIT = 0x40;
        }
    }

    interface EventFd_H {
        static final int EFD_SEMAPHORE = 1;
        static final int EFD_CLOEXEC = 524288;
        static final int EFD_NONBLOCK = 2048;
    }

    interface Error_H {
        public static final int EPERM = 1;
        ;/* Operation not permitted */
        public static final int ENOENT = 2;
        ;/* No such file or directory */
        public static final int ESRCH = 3;
        ;/* No such process */
        public static final int EINTR = 4;
        ;/* Interrupted system call */
        public static final int EIO = 5;
        ;/* I/O error */
        public static final int ENXIO = 6;
        ;/* No such device or address */
        public static final int E2BIG = 7;
        ;/* Argument list too long */
        public static final int ENOEXEC = 8;
        ;/* Exec format error */
        public static final int EBADF = 9;
        ;/* Bad file number */
        public static final int ECHILD = 10;
        ;/* No child processes */
        public static final int EAGAIN = 11;
        ;/* Try again */
        public static final int ENOMEM = 12;
        ;/* Out of memory */
        public static final int EACCES = 13;
        ;/* Permission denied */
        public static final int EFAULT = 14;
        ;/* Bad address */
        public static final int ENOTBLK = 15;
        ;/* Block device required */
        public static final int EBUSY = 16;
        ;/* Device or resource busy */
        public static final int EEXIST = 17;
        ;/* File exists */
        public static final int EXDEV = 18;
        ;/* Cross-device link */
        public static final int ENODEV = 19;
        ;/* No such device */
        public static final int ENOTDIR = 20;
        ;/* Not a directory */
        public static final int EISDIR = 21;
        ;/* Is a directory */
        public static final int EINVAL = 22;
        ;/* Invalid argument */
        public static final int ENFILE = 23;
        ;/* File table overflow */
        public static final int EMFILE = 24;
        ;/* Too many open files */
        public static final int ENOTTY = 25;
        ;/* Not a typewriter */
        public static final int ETXTBSY = 26;
        ;/* Text file busy */
        public static final int EFBIG = 27;
        ;/* File too large */
        public static final int ENOSPC = 28;
        ;/* No space left on device */
        public static final int ESPIPE = 29;
        ;/* Illegal seek */
        public static final int EROFS = 30;
        ;/* Read-only file system */
        public static final int EMLINK = 31;
        ;/* Too many links */
        public static final int EPIPE = 32;
        ;/* Broken pipe */
        public static final int EDOM = 33;
        ;/* Math argument out of domain of func */
        public static final int ERANGE = 34;
        ;/* Math value not representable */

        public static final int EDEADLK = 35;
        ;/* Resource deadlock would occur */
        public static final int ENAMETOOLONG = 36;
        ;/* File name too long */
        public static final int ENOLCK = 37;
        ;/* No record locks available */

        public static final int ENOSYS = 38;
        ;/* Invalid system call number */
        public static final int ENOTEMPTY = 39;
        ;/* Directory not empty */
        public static final int ELOOP = 40;
        ;/* Too many symbolic links encountered */
        public static final int EWOULDBLOCK = EAGAIN;
        ;/* Operation would block */
        public static final int ENOMSG = 42;
        ;/* No message of desired type */
        public static final int EIDRM = 43;
        ;/* Identifier removed */
        public static final int ECHRNG = 44;
        ;/* Channel number out of range */
        public static final int EL2NSYNC = 45;
        ;/* Level 2 not synchronized */
        public static final int EL3HLT = 46;
        ;/* Level 3 halted */
        public static final int EL3RST = 47;
        ;/* Level 3 reset */
        public static final int ELNRNG = 48;
        ;/* Link number out of range */
        public static final int EUNATCH = 49;
        ;/* Protocol driver not attached */
        public static final int ENOCSI = 50;
        ;/* No CSI structure available */
        public static final int EL2HLT = 51;
        ;/* Level 2 halted */
        public static final int EBADE = 52;
        ;/* Invalid exchange */
        public static final int EBADR = 53;
        ;/* Invalid request descriptor */
        public static final int EXFULL = 54;
        ;/* Exchange full */
        public static final int ENOANO = 55;
        ;/* No anode */
        public static final int EBADRQC = 56;
        ;/* Invalid request code */
        public static final int EBADSLT = 57;
        ;/* Invalid slot */
        public static final int EDEADLOCK = EDEADLK;
        public static final int EBFONT = 59;
        ;/* Bad font file format */
        public static final int ENOSTR = 60;
        ;/* Device not a stream */
        public static final int ENODATA = 61;
        ;/* No data available */
        public static final int ETIME = 62;
        ;/* Timer expired */
        public static final int ENOSR = 63;
        ;/* Out of streams resources */
        public static final int ENONET = 64;
        ;/* Machine is not on the network */
        public static final int ENOPKG = 65;
        ;/* Package not installed */
        public static final int EREMOTE = 66;
        ;/* Object is remote */
        public static final int ENOLINK = 67;
        ;/* Link has been severed */
        public static final int EADV = 68;
        ;/* Advertise error */
        public static final int ESRMNT = 69;
        ;/* Srmount error */
        public static final int ECOMM = 70;
        ;/* Communication error on send */
        public static final int EPROTO = 71;
        ;/* Protocol error */
        public static final int EMULTIHOP = 72;
        ;/* Multihop attempted */
        public static final int EDOTDOT = 73;
        ;/* RFS specific error */
        public static final int EBADMSG = 74;
        ;/* Not a data message */
        public static final int EOVERFLOW = 75;
        ;/* Value too large for defined data type */
        public static final int ENOTUNIQ = 76;
        ;/* Name not unique on network */
        public static final int EBADFD = 77;
        ;/* File descriptor in bad state */
        public static final int EREMCHG = 78;
        ;/* Remote address changed */
        public static final int ELIBACC = 79;
        ;/* Can not access a needed shared library */
        public static final int ELIBBAD = 80;
        ;/* Accessing a corrupted shared library */
        public static final int ELIBSCN = 81;
        ;/* .lib section in a.out corrupted */
        public static final int ELIBMAX = 82;
        ;/* Attempting to link in too many shared libraries */
        public static final int ELIBEXEC = 83;
        ;/* Cannot exec a shared library directly */
        public static final int EILSEQ = 84;
        ;/* Illegal byte sequence */
        public static final int ERESTART = 85;
        ;/* Interrupted system call should be restarted */
        public static final int ESTRPIPE = 86;
        ;/* Streams pipe error */
        public static final int EUSERS = 87;
        ;/* Too many users */
        public static final int ENOTSOCK = 88;
        ;/* Socket operation on non-socket */
        public static final int EDESTADDRREQ = 89;
        ;/* Destination address required */
        public static final int EMSGSIZE = 90;
        ;/* Message too long */
        public static final int EPROTOTYPE = 91;
        ;/* Protocol wrong type for socket */
        public static final int ENOPROTOOPT = 92;
        ;/* Protocol not available */
        public static final int EPROTONOSUPPORT = 93;
        ;/* Protocol not supported */
        public static final int ESOCKTNOSUPPORT = 94;
        ;/* Socket type not supported */
        public static final int EOPNOTSUPP = 95;
        ;/* Operation not supported on transport endpoint */
        public static final int EPFNOSUPPORT = 96;
        ;/* Protocol family not supported */
        public static final int EAFNOSUPPORT = 97;
        ;/* Address family not supported by protocol */
        public static final int EADDRINUSE = 98;
        ;/* Address already in use */
        public static final int EADDRNOTAVAIL = 99;
        ;/* Cannot assign requested address */
        public static final int ENETDOWN = 100;
        ;/* Network is down */
        public static final int ENETUNREACH = 101;
        ;/* Network is unreachable */
        public static final int ENETRESET = 102;
        ;/* Network dropped connection because of reset */
        public static final int ECONNABORTED = 103;
        ;/* Software caused connection abort */
        public static final int ECONNRESET = 104;
        ;/* Connection reset by peer */
        public static final int ENOBUFS = 105;
        ;/* No buffer space available */
        public static final int EISCONN = 106;
        ;/* Transport endpoint is already connected */
        public static final int ENOTCONN = 107;
        ;/* Transport endpoint is not connected */
        public static final int ESHUTDOWN = 108;
        ;/* Cannot send after transport endpoint shutdown */
        public static final int ETOOMANYREFS = 109;
        ;/* Too many references: cannot splice */
        public static final int ETIMEDOUT = 110;
        ;/* Connection timed out */
        public static final int ECONNREFUSED = 111;
        ;/* Connection refused */
        public static final int EHOSTDOWN = 112;
        ;/* Host is down */
        public static final int EHOSTUNREACH = 113;
        ;/* No route to host */
        public static final int EALREADY = 114;
        ;/* Operation already in progress */
        public static final int EINPROGRESS = 115;
        ;/* Operation now in progress */
        public static final int ESTALE = 116;
        ;/* Stale file handle */
        public static final int EUCLEAN = 117;
        ;/* Structure needs cleaning */
        public static final int ENOTNAM = 118;
        ;/* Not a XENIX named type file */
        public static final int ENAVAIL = 119;
        ;/* No XENIX semaphores available */
        public static final int EISNAM = 120;
        ;/* Is a named type file */
        public static final int EREMOTEIO = 121;
        ;/* Remote I/O error */
        public static final int EDQUOT = 122;
        ;/* Quota exceeded */
        public static final int ENOMEDIUM = 123;
        ;/* No medium found */
        public static final int EMEDIUMTYPE = 124;
        ;/* Wrong medium type */
        public static final int ECANCELED = 125;
        ;/* Operation Canceled */
        public static final int ENOKEY = 126;
        ;/* Required key not available */
        public static final int EKEYEXPIRED = 127;
        ;/* Key has expired */
        public static final int EKEYREVOKED = 128;
        ;/* Key has been revoked */
        public static final int EKEYREJECTED = 129;
        ;/* Key was rejected by service */
        public static final int EOWNERDEAD = 130;
        ;/* Owner died */
        public static final int ERFKILL = 132;
        ;/* Operation not possible due to RF-kill */
        public static final int EHWPOISON = 133;
        ;/* Memory page has hardware error */
        public static final int ENOTRECOVERABLE = 131;
        ;/* State not recoverable */
    }
}