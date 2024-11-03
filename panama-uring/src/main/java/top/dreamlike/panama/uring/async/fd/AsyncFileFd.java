package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.uring.async.cancel.CancelableFuture;
import top.dreamlike.panama.uring.async.trait.IoUringBufferRing;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.helper.UnsafeHelper;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;

public class AsyncFileFd implements IoUringAsyncFd, IoUringSelectedReadableFd {

    private static final VarHandle BUFFER_RING_VH;

    static {
        try {
            BUFFER_RING_VH = MethodHandles.lookup().findVarHandle(AsyncFileFd.class, "bufferRing", IoUringBufferRing.class);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private final IoUringEventLoop ioUringEventLoop;
    private final int fd;
    private IoUringBufferRing bufferRing;

    public AsyncFileFd(IoUringEventLoop ioUringEventLoop, int fd) {
        this.ioUringEventLoop = ioUringEventLoop;
        this.fd = fd;
    }

    public AsyncFileFd(IoUringEventLoop ioUringEventLoop, File file) throws IOException {
        if (!file.exists()) {
            throw new IllegalArgumentException("file not exists");
        }
        FileChannel open = FileChannel.open(file.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE);
        this(ioUringEventLoop, open);
    }

    public AsyncFileFd(IoUringEventLoop ioUringEventLoop, FileChannel fc) {
        this.ioUringEventLoop = ioUringEventLoop;
        this.fd = UnsafeHelper.getFd(fc);
    }

    public static CancelableFuture<AsyncFileFd> asyncOpen(IoUringEventLoop ioUringEventLoop, OwnershipMemory path, int flags) {
        return (CancelableFuture<AsyncFileFd>) ioUringEventLoop.asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_openat(sqe, -1, path.resource(), flags, 0))
                .whenComplete((_, _) -> path.drop())
                .thenCompose(cqe -> cqe.getRes() <= 0 ? CompletableFuture.failedFuture(new SyscallException(cqe.getRes())) : CompletableFuture.completedFuture(cqe.getRes()))
                .thenApply(fd -> new AsyncFileFd(ioUringEventLoop, fd));
    }

    @Override
    public IoUringEventLoop owner() {
        return ioUringEventLoop;
    }

    public CancelableFuture<Integer> asyncFsync(int fsync_flags) {
        return (CancelableFuture<Integer>) ioUringEventLoop.asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_fsync(sqe, fd, fsync_flags))
                .thenApply(IoUringCqe::getRes);
    }

    public CancelableFuture<Integer> asyncFsync(int fsync_flags, int offset, int len) {
        return (CancelableFuture<Integer>) ioUringEventLoop.asyncOperation(sqe -> {
                    Instance.LIB_URING.io_uring_prep_fsync(sqe, fd, fsync_flags);
                    sqe.setLen(len);
                    sqe.setOffset(offset);
                })
                .thenApply(IoUringCqe::getRes);
    }

    @Override
    public int fd() {
        return fd;
    }

    @Override
    public IoUringBufferRing bufferRing() {
        if (bufferRing == null) {
            throw new IllegalArgumentException("must set bufferRing before use it!");
        }
        return bufferRing;
    }

    public boolean bindBufferRing(IoUringBufferRing bufferRing) {
        return BUFFER_RING_VH.compareAndSet(this, null, bufferRing);
    }
}
