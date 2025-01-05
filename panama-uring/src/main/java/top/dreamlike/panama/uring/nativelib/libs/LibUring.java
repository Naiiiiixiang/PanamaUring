package top.dreamlike.panama.uring.nativelib.libs;

import top.dreamlike.panama.generator.annotation.CLib;
import top.dreamlike.panama.generator.annotation.NativeFunction;
import top.dreamlike.panama.generator.annotation.Pointer;
import top.dreamlike.panama.generator.exception.StructException;
import top.dreamlike.panama.generator.proxy.NativeArrayPointer;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.helper.KernelVersionLimit;
import top.dreamlike.panama.uring.nativelib.helper.NativeHelper;
import top.dreamlike.panama.uring.nativelib.struct.epoll.NativeEpollEvent;
import top.dreamlike.panama.uring.nativelib.struct.futex.FutexWaitV;
import top.dreamlike.panama.uring.nativelib.struct.iovec.Iovec;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUring;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringBufReg;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringConstant;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringParams;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringProbe;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringSqe;
import top.dreamlike.panama.uring.nativelib.struct.liburing.NativeIoUringBufRing;
import top.dreamlike.panama.uring.nativelib.struct.sigset.SigsetType;
import top.dreamlike.panama.uring.nativelib.struct.socket.MsgHdr;
import top.dreamlike.panama.uring.nativelib.struct.time.KernelTime64Type;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

import static java.lang.foreign.ValueLayout.JAVA_SHORT;

@CLib(value = "liburing-ffi.so", suffix = "_rs")
@KernelVersionLimit(major = 5, minor = 10)
public interface LibUring {

    @NativeFunction(fast = true)
    int io_uring_struct_size();

    @NativeFunction(fast = true)
    int io_uring_cq_struct_size();

    @NativeFunction(fast = true)
    int io_uring_cqe_struct_size();

    @NativeFunction(fast = true)
    int io_uring_sq_struct_size();

    @NativeFunction(fast = true)
    int io_uring_sqe_struct_size();

    @NativeFunction(returnIsPointer = true)
    IoUringProbe io_uring_get_probe();

    void io_uring_free_probe(@Pointer IoUringProbe probe);

    //跟队列本身相关的操作=
    int io_uring_queue_init(int entries, @Pointer IoUring ring, int flags);

    int io_uring_queue_init_params(int entries, @Pointer IoUring ring, @Pointer IoUringParams p);

    void io_uring_queue_exit(@Pointer IoUring ring);

    int io_uring_setup(int entries, @Pointer IoUringParams ioUringParams);

    //cqe相关的操作
    @NativeFunction(fast = true)
    default int io_uring_peek_batch_cqe(@Pointer IoUring ring, @Pointer MemorySegment cqes, int count) {
        int ready;
        boolean overflow_checked = false;
        int shift = 0;
        if ((ring.getFlags() & IoUringConstant.IORING_SETUP_CQE32) != 0) {
            shift = 1;
        }
        MemorySegment ringStruct = StructProxyGenerator.findMemorySegment(ring);
        do {
            ready = io_uring_cq_ready(ring);
            if (ready != 0) {
                int head = (int) IoUringConstant.AccessShortcuts.IO_URING_CQ_KHEAD_DEFERENCE_VARHANDLE.get(ringStruct, 0L);
                int mask = (int) IoUringConstant.AccessShortcuts.IO_URING_CQ_RING_MASK_VARHANDLE.get(ringStruct, 0L);
                int last;
                int i = 0;
                count = Math.min(count, ready);
                last = head + count;
                MemorySegment cqesBase = (MemorySegment) IoUringConstant.AccessShortcuts.IO_URING_CQ_CQES_VARHANDLE.get(ringStruct, 0L);
                long step = IoUringConstant.AccessShortcuts.IoUringCqeLayout.byteSize();
                for (; head != last; head++, i++) {
                    int index = (head & mask) << shift;
                    cqes.set(ValueLayout.ADDRESS, i * ValueLayout.ADDRESS.byteSize(), MemorySegment.ofAddress(cqesBase.address() + index * step));
                }
                return count;
            }
            if (overflow_checked) {
                return 0;
            }
            if (cq_ring_needs_flush(ringStruct)) {
                io_uring_get_events(ring);
                overflow_checked = true;
            }
        } while (overflow_checked);
        return 0;
    }

    @NativeFunction(fast = true)
    default long[] io_uring_peek_batch_cqe(@Pointer IoUring ring, int maxCount) {
        int ready;
        boolean overflow_checked = false;
        int shift = 0;
        if ((ring.getFlags() & IoUringConstant.IORING_SETUP_CQE32) != 0) {
            shift = 1;
        }
        MemorySegment ringStruct = StructProxyGenerator.findMemorySegment(ring);
        do {
            ready = io_uring_cq_ready(ring);
            if (ready != 0) {
                int head = (int) IoUringConstant.AccessShortcuts.IO_URING_CQ_KHEAD_DEFERENCE_VARHANDLE.get(ringStruct, 0L);
                int mask = (int) IoUringConstant.AccessShortcuts.IO_URING_CQ_RING_MASK_VARHANDLE.get(ringStruct, 0L);
                int last;
                int i = 0;
                maxCount = Math.min(maxCount, ready);
                long[] cqes = new long[maxCount];
                last = head + maxCount;
                MemorySegment cqesBase = (MemorySegment) IoUringConstant.AccessShortcuts.IO_URING_CQ_CQES_VARHANDLE.get(ringStruct, 0L);
                long step = IoUringConstant.AccessShortcuts.IoUringCqeLayout.byteSize();
                for (; head != last; head++, i++) {
                    int index = (head & mask) << shift;
                    long currentCqe = cqesBase.address() + index * step;
                    cqes[i] = currentCqe;
                }
                return cqes;
            }
            if (overflow_checked) {
                return NativeHelper.EMPTY_ARRAY;
            }
            if (cq_ring_needs_flush(ringStruct)) {
                io_uring_get_events(ring);
                overflow_checked = true;
            }
        } while (overflow_checked);
        return NativeHelper.EMPTY_ARRAY;
    }

    default int get_koverflow(@Pointer IoUring uring) {
        MemorySegment ringStruct = StructProxyGenerator.findMemorySegment(uring);
        return (int) IoUringConstant.AccessShortcuts.IO_URING_CQ_K_OVERFLOW_VARHANDLE.get(ringStruct, 0L);
    }

    //https://lwn.net/Articles/804108/
    private boolean cq_ring_needs_flush(MemorySegment ioUring) {
        int sqFlags = (int) IoUringConstant.AccessShortcuts.IO_URING_SQ_KFLAGS_DEFERENCE_VARHANDLE.get(ioUring, 0L);
        return (sqFlags & (IoUringConstant.IORING_SQ_CQ_OVERFLOW | IoUringConstant.IORING_SQ_TASKRUN)) != 0;
    }

    int io_uring_peek_cqe(@Pointer IoUring ring, @Pointer MemorySegment cqe_ptr);

    int io_uring_wait_cqes(@Pointer IoUring ring, @Pointer MemorySegment cqe_ptr, int wait_nr, @Pointer KernelTime64Type ts, @Pointer SigsetType sigmask);

    @KernelVersionLimit(major = 5, minor = 11)
    int io_uring_wait_cqe_timeout(@Pointer IoUring ring, @Pointer MemorySegment cqe_ptr, @Pointer KernelTime64Type ts);

    int io_uring_submit(@Pointer IoUring ring);

    @KernelVersionLimit(major = 5, minor = 11)
    int io_uring_submit_and_wait(@Pointer IoUring ring, int wait_nr);

    @KernelVersionLimit(major = 5, minor = 11)
    int io_uring_submit_and_wait_timeout(@Pointer IoUring ring, @Pointer MemorySegment cqe_ptr, int wait_nr, @Pointer KernelTime64Type ts, @Pointer SigsetType sigmask);

    @NativeFunction(fast = true)
    default void io_uring_cq_advance(@Pointer IoUring ring, int nr) {
        if (nr == 0) {
            return;
        }
        MemorySegment realMemory = StructProxyGenerator.findMemorySegment(ring);
        VarHandle kheadVH = IoUringConstant.AccessShortcuts.IO_URING_CQ_KHEAD_DEFERENCE_VARHANDLE;
        int head = (int) kheadVH.get(realMemory, 0L);
        kheadVH.setRelease(realMemory, 0L, head + nr);
    }

    int io_uring_register_buffers(@Pointer IoUring ring, @Pointer NativeArrayPointer<Iovec> iovecs, int nr_iovecs);

    int io_uring_unregister_buffers(@Pointer IoUring ring);

    int io_uring_register_files(@Pointer IoUring ring,/*const int * */ @Pointer MemorySegment files, int nr_files);

    int io_uring_unregister_files(@Pointer IoUring ring);

    int io_uring_register_eventfd(@Pointer IoUring ring, int fd);

    int io_uring_register_eventfd_async(@Pointer IoUring ring, int fd);

    int io_uring_unregister_eventfd(@Pointer IoUring ring);

    int io_uring_register_ring_fd(@Pointer IoUring ring);

    int io_uring_unregister_ring_fd(@Pointer IoUring ring);

    int io_uring_close_ring_fd(@Pointer IoUring ring);

    @KernelVersionLimit(major = 5, minor = 19)
    int io_uring_register_buf_ring(@Pointer IoUring ring, @Pointer IoUringBufReg br, int br_flags);

    @KernelVersionLimit(major = 5, minor = 19)
    int io_uring_unregister_buf_ring(@Pointer IoUring ring, int bgid);

    @NativeFunction(fast = true)
    @KernelVersionLimit(major = 5, minor = 19)
    default void io_uring_buf_ring_add(@Pointer NativeIoUringBufRing br, @Pointer MemorySegment addr, int len, short bid, int mask, int buf_offset) {
        MemorySegment realMemory = StructProxyGenerator.findMemorySegment(br);
        short tail = (short) IoUringConstant.AccessShortcuts.IO_URING_BUF_RING_TAIL_VARHANDLE.get(realMemory, 0L);
        int index = (tail + buf_offset) & mask;
        MemorySegment bufPointer = MemorySegment.ofAddress(realMemory.address() + index * IoUringConstant.AccessShortcuts.IoUringBufLayout.byteSize())
                .reinterpret(IoUringConstant.AccessShortcuts.IoUringBufLayout.byteSize());
        IoUringConstant.AccessShortcuts.IO_URING_BUF_ADDR_VARHANDLE.set(bufPointer, 0L, addr.address());
        IoUringConstant.AccessShortcuts.IO_URING_BUF_LEN_VARHANDLE.set(bufPointer, 0L, len);
        IoUringConstant.AccessShortcuts.IO_URING_BUF_BID_VARHANDLE.set(bufPointer, 0L, bid);
    }

    @NativeFunction(fast = true)
    @KernelVersionLimit(major = 5, minor = 19)
    default void io_uring_buf_ring_advance(@Pointer NativeIoUringBufRing br, int count) {
        MemorySegment realMemory = StructProxyGenerator.findMemorySegment(br);
        short newTail = (short) ((short) IoUringConstant.AccessShortcuts.IO_URING_BUF_RING_TAIL_VARHANDLE.get(realMemory, 0L) + count);
        //由于MemoryLayout直接获取的varHandle只支持int 、 long 、 float 、 double 和 MemorySegment 的原子更新访问
        //所以换个方式
        JAVA_SHORT.varHandle().setRelease(realMemory, IoUringConstant.AccessShortcuts.IO_URING_BUF_RING_TAIL_OFFSET, newTail);
    }

    @NativeFunction(fast = true)
    default int io_uring_buf_ring_mask(int ringEntries) {
        return ringEntries - 1;
    }

    @NativeFunction(fast = true)
    default void io_uring_buf_ring_init(@Pointer NativeIoUringBufRing br) {
        MemorySegment realMemory = StructProxyGenerator.findMemorySegment(br);
        IoUringConstant.AccessShortcuts.IO_URING_BUF_RING_TAIL_VARHANDLE.set(realMemory, 0L, 0);
    }

    @NativeFunction(returnIsPointer = true)
    @KernelVersionLimit(major = 5, minor = 19)
    NativeIoUringBufRing io_uring_setup_buf_ring(@Pointer IoUring ioUring, int nentries, int bgid, int flags,/*int *ret*/@Pointer MemorySegment ret);

    int io_uring_buf_ring_head(@Pointer IoUring ring, int buf_group, /* unsigned * head*/ @Pointer MemorySegment head);

    int io_uring_submit_and_get_events(@Pointer IoUring ring);

    int io_uring_get_events(@Pointer IoUring ring);

    //sqe相关的操作
    @NativeFunction(fast = true, returnIsPointer = true)
    default IoUringSqe io_uring_get_sqe(@Pointer IoUring ring) {
        MemorySegment realMemory = StructProxyGenerator.findMemorySegment(ring);
        int head;
        int next = (int) IoUringConstant.AccessShortcuts.IO_URING_SQ_SQE_TAIL_VARHANDLE.get(realMemory, 0L) + 1;
        int shift = 0;
        if ((ring.getFlags() & IoUringConstant.IORING_SETUP_SQE128) != 0) {
            shift = 1;
        }
        if ((ring.getFlags() & IoUringConstant.IORING_SETUP_SQPOLL) != 0) {
            head = (int) IoUringConstant.AccessShortcuts.IO_URING_SQ_KHEAD_DEFERENCE_VARHANDLE.getAcquire(realMemory, 0L);
        } else {
            head = (int) IoUringConstant.AccessShortcuts.IO_URING_SQ_KHEAD_DEFERENCE_VARHANDLE.get(realMemory, 0L);
        }
        int ring_entries = (int) IoUringConstant.AccessShortcuts.IO_URING_SQ_RING_ENTRIES_VARHANDLE.get(realMemory, 0L);
        if (next - head > ring_entries) {
            return null;
        }
        MemorySegment sqesBase = (MemorySegment) IoUringConstant.AccessShortcuts.IO_URING_SQ_SQES_VARHANDLE.get(realMemory, 0L);
        int index = (int) IoUringConstant.AccessShortcuts.IO_URING_SQ_SQE_TAIL_VARHANDLE.get(realMemory, 0L) & (int) IoUringConstant.AccessShortcuts.IO_URING_SQ_RING_MASK_VARHANDLE.get(realMemory, 0L);
        index = index << shift;
        MemoryLayout sqeLayout = IoUringConstant.AccessShortcuts.IoUringSqeLayout;
        MemorySegment currentSqe = MemorySegment.ofAddress(sqesBase.address() + index * sqeLayout.byteSize())
                .reinterpret(sqeLayout.byteSize());
        IoUringConstant.AccessShortcuts.IO_URING_SQ_SQE_TAIL_VARHANDLE.set(realMemory, 0L, next);
        return Instance.STRUCT_PROXY_GENERATOR.enhance(currentSqe);
    }

    @NativeFunction(fast = true)
    default void io_uring_back_sqe(@Pointer IoUring ring) {
        MemorySegment realMemory = StructProxyGenerator.findMemorySegment(ring);
        int tail = (int) IoUringConstant.AccessShortcuts.IO_URING_SQ_SQE_TAIL_VARHANDLE.get(realMemory, 0L);
        IoUringConstant.AccessShortcuts.IO_URING_SQ_SQE_TAIL_VARHANDLE.set(realMemory, 0L, tail - 1);
    }

    default void io_uring_prep_rw(int opcode, @Pointer IoUringSqe sqe, int fd, MemorySegment addr, int len, long offset) {
        if (!StructProxyGenerator.isNativeStruct(sqe)) {
            throw new StructException("sqe is not struct,pleace call StructProxyGenerator::enhance before calling native function");
        }
        sqe.setOpcode((byte) opcode);
        sqe.setFlags((byte) 0);
        sqe.setIoprio((short) 0);
        sqe.setFd(fd);
        sqe.setOffset(offset);
        sqe.setAddr(addr == null ? 0 : addr.address());
        sqe.setLen(len);
        sqe.setFlagsInFlagsUnion(0);
        sqe.setBufIndex((short) 0);
        sqe.setPersonality((short) 0);
        sqe.setFileIndex(0);
        sqe.setAddr3(0);
        sqe.setPad2(0);
    }


    /**
     * io_uring_prep_splice() - Either @fd_in or @fd_out must be a pipe.
     * - If @fd_in refers to a pipe, @off_in is ignored and must be set to -1.
     * - If @fd_in does not refer to a pipe and @off_in is -1, then @nbytes are read
     * from @fd_in starting from the file offset, which is incremented by the
     * number of bytes read.
     * - If @fd_in does not refer to a pipe and @off_in is not -1, then the starting
     * offset of @fd_in will be @off_in.
     * <p>
     * This splice operation can be used to implement sendfile by splicing to an
     * intermediate pipe first, then splice to the final destination.
     * In fact, the implementation of sendfile in kernel uses splice internally.
     * NOTE that even if fd_in or fd_out refers to a pipe, the splice operation
     * can still fail with EINVAL if one of the fd doesn't explicitly support splice
     * operation, e.g. reading from terminal is unsupported from kernel 5.7 to 5.11.
     * Check issue #291 for more information.
     */
    @KernelVersionLimit(major = 5, minor = 7)
    default void io_uring_prep_splice(@Pointer IoUringSqe sqe,
                                      int fd_in, long off_in,
                                      int fd_out, long off_out,
                                      int nbytes,
                                      int splice_flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_SPLICE, sqe, fd_out, MemorySegment.NULL, nbytes, off_out);
        sqe.setSpliceOffIn(off_in);
        sqe.setSpliceFdIn(fd_in);
        sqe.setSpliceFlags(splice_flags);
    }

    @KernelVersionLimit(major = 5, minor = 8)
    default void io_uring_prep_tee(@Pointer IoUringSqe sqe,
                                   int fd_in, int fd_out,
                                   int nbytes,
                                   int splice_flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_TEE, sqe, fd_out, MemorySegment.NULL, nbytes, 0);
        sqe.setSpliceOffIn(0);
        sqe.setSpliceFdIn(fd_in);
        sqe.setSpliceFlags(splice_flags);
    }

    default void io_uring_prep_readv(@Pointer IoUringSqe sqe, int fd, @Pointer NativeArrayPointer<Iovec> iovec, int count, long offset) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_READV, sqe, fd, StructProxyGenerator.findMemorySegment(iovec), count, offset);
    }

    default void io_uring_prep_readv2(@Pointer IoUringSqe sqe, int fd, @Pointer Iovec iovec, int count, long offset, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_READV, sqe, fd, StructProxyGenerator.findMemorySegment(iovec), count, offset);
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_read_fixed(@Pointer IoUringSqe sqe, int fd, @Pointer MemorySegment buf, int nbytes, long offset, int buf_index) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_READ_FIXED, sqe, fd, buf, nbytes, offset);
        sqe.setBufIndex((short) buf_index);
    }


    default void io_uring_prep_writev(@Pointer IoUringSqe sqe, int fd, @Pointer NativeArrayPointer<Iovec> iovec, int count, long offset) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_WRITEV, sqe, fd, StructProxyGenerator.findMemorySegment(iovec), count, offset);
    }

    default void io_uring_prep_writev2(@Pointer IoUringSqe sqe, int fd, @Pointer NativeArrayPointer<Iovec> iovec, int count, long offset, int flags) {
        io_uring_prep_writev(sqe, fd, iovec, count, offset);
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_write_fixed(@Pointer IoUringSqe sqe, int fd, @Pointer MemorySegment buf, int nbytes, long offset, int buf_index) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_WRITE_FIXED, sqe, fd, buf, nbytes, offset);
        sqe.setBufIndex((short) buf_index);
    }

    default void io_uring_prep_recvmsg(@Pointer IoUringSqe sqe, int fd, @Pointer MsgHdr msg, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_RECVMSG, sqe, fd, StructProxyGenerator.findMemorySegment(msg), 1, 0);
        //是个union 直接写rwflags就行了
        sqe.setFlagsInFlagsUnion(flags);
    }

    @KernelVersionLimit(major = 5, minor = 19)
    default void io_uring_prep_recvmsg_multishot(@Pointer IoUringSqe sqe, int fd, @Pointer MsgHdr msg, int flags) {
        io_uring_prep_recvmsg(sqe, fd, msg, flags);
        sqe.setIoprio((short) (sqe.getIoprio() | IoUringConstant.IORING_RECV_MULTISHOT));
    }

    default void io_uring_prep_sendmsg(@Pointer IoUringSqe sqe, int fd, @Pointer MsgHdr msg, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_SENDMSG, sqe, fd, StructProxyGenerator.findMemorySegment(msg), 1, 0);
        //是个union 直接写rwflags就行了
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_poll_add(@Pointer IoUringSqe sqe, int fd, int poll_mask) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_POLL_ADD, sqe, fd, MemorySegment.NULL, 0, 0);
        //是个union 直接写rwflags就行了
        sqe.setFlagsInFlagsUnion(io_uring_prep_poll_mask(poll_mask));
    }

    @KernelVersionLimit(major = 5, minor = 13)
    default void io_uring_prep_poll_multishot(@Pointer IoUringSqe sqe, int fd, int poll_mask) {
        io_uring_prep_poll_add(sqe, fd, poll_mask);
        sqe.setLen(IoUringConstant.IORING_POLL_ADD_MULTI);
    }

    default void io_uring_prep_poll_remove(@Pointer IoUringSqe sqe, int user_data) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_POLL_REMOVE, sqe, -1, MemorySegment.NULL, 0, 0);
        sqe.setAddr(user_data);
    }

    @KernelVersionLimit(major = 5, minor = 13)
    default void io_uring_prep_poll_update(@Pointer IoUringSqe sqe, long old_user_data, long new_user_data, int poll_mask, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_POLL_REMOVE, sqe, -1, MemorySegment.NULL, flags, new_user_data);
        sqe.setAddr(old_user_data);
        sqe.setFlagsInFlagsUnion(io_uring_prep_poll_mask(poll_mask));
    }

    default void io_uring_prep_fsync(@Pointer IoUringSqe sqe, int fd, int fsync_flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_FSYNC, sqe, fd, MemorySegment.NULL, 0, 0);
        sqe.setFlagsInFlagsUnion(fsync_flags);
    }

    default void io_uring_prep_nop(@Pointer IoUringSqe sqe) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_NOP, sqe, -1, MemorySegment.NULL, 0, 0);
    }

    default void io_uring_prep_timeout(@Pointer IoUringSqe sqe, @Pointer KernelTime64Type ts, int count, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_TIMEOUT, sqe, -1, StructProxyGenerator.findMemorySegment(ts), 1, count);
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_timeout_remove(@Pointer IoUringSqe sqe, int user_data, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_TIMEOUT_REMOVE, sqe, -1, MemorySegment.NULL, 0, 0);
        sqe.setAddr(user_data);
        sqe.setFlagsInFlagsUnion(flags);
    }

    @KernelVersionLimit(major = 5, minor = 11)
    default void io_uring_prep_timeout_update(@Pointer IoUringSqe sqe, @Pointer KernelTime64Type ts, long user_data, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_TIMEOUT_REMOVE, sqe, -1, MemorySegment.NULL, 0, StructProxyGenerator.findMemorySegment(ts).address());
        sqe.setAddr(user_data);
        sqe.setFlagsInFlagsUnion(flags | IoUringConstant.IORING_TIMEOUT_UPDATE);
    }

    /**
     * addr给的宽松一点 以支持v4 v6
     */
    default void io_uring_prep_accept(@Pointer IoUringSqe sqe, int fd,/* struct sockaddr * */ @Pointer MemorySegment addr, /* socklen_t*  */@Pointer MemorySegment addrlen, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_ACCEPT, sqe, fd, addr, 0, addrlen.address());
        sqe.setFlagsInFlagsUnion(flags);
    }

    /* accept directly into the fixed file table */
    default void io_uring_prep_accept_direct(@Pointer IoUringSqe sqe, int fd,/* struct sockaddr * */ @Pointer MemorySegment addr, /* socklen_t*  */@Pointer MemorySegment addrlen, int flags, int file_index) {
        io_uring_prep_accept(sqe, fd, addr, addrlen, flags);
        /* offset by 1 for allocation */
        if (file_index == IoUringConstant.IORING_FILE_INDEX_ALLOC) {
            file_index--;
        }
        sqe.setFileIndex(file_index + 1);
    }

    @KernelVersionLimit(major = 5, minor = 19)
    default void io_uring_prep_multishot_accept(@Pointer IoUringSqe sqe, int fd,/* struct sockaddr * */ @Pointer MemorySegment addr, /* socklen_t*  */@Pointer MemorySegment addrlen, int flags) {
        io_uring_prep_accept(sqe, fd, addr, addrlen, flags);
        sqe.setIoprio((short) (sqe.getIoprio() | IoUringConstant.IORING_ACCEPT_MULTISHOT));
    }

    @KernelVersionLimit(major = 5, minor = 19)
    default void io_uring_prep_multishot_accept_direct(@Pointer IoUringSqe sqe, int fd,/* struct sockaddr * */ @Pointer MemorySegment addr, /* socklen_t*  */@Pointer MemorySegment addrlen, int flags) {
        io_uring_prep_multishot_accept(sqe, fd, addr, addrlen, flags);
        sqe.setFileIndex(IoUringConstant.IORING_FILE_INDEX_ALLOC);
    }

    /**
     * Although  the  cancelation request uses async request syntax, the kernel side of the cancelation is always run synchronously. It is guaranteed that a CQE is always generated by the time the cancel re‐
     * quest has been submitted. If the cancelation is successful, the completion for the request targeted for cancelation will have been posted by the time submission returns. For ‐EALREADY it  may  take  a
     * bit of time to do so. For this case, the caller must wait for the canceled request to post its completion event.
     */
    default void io_uring_prep_cancel64(@Pointer IoUringSqe sqe, long user_data, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_ASYNC_CANCEL, sqe, -1, MemorySegment.NULL, 0, 0);
        sqe.setAddr(user_data);
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_cancel_fd(@Pointer IoUringSqe sqe, int fd, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_ASYNC_CANCEL, sqe, fd, MemorySegment.NULL, 0, 0);
        sqe.setFlagsInFlagsUnion(flags | IoUringConstant.IORING_ASYNC_CANCEL_FD);
    }

    default void io_uring_prep_link_timeout(@Pointer IoUringSqe sqe, @Pointer KernelTime64Type ts, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_LINK_TIMEOUT, sqe, -1, StructProxyGenerator.findMemorySegment(ts), 1, 0);
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_connect(@Pointer IoUringSqe sqe, int fd, /* struct sockaddr * */ @Pointer MemorySegment addr, int addrlen) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_CONNECT, sqe, fd, addr, 0, addrlen);
    }

    default void io_uring_prep_files_update(@Pointer IoUringSqe sqe,/* int *fds */ @Pointer MemorySegment fds, int nr_fds, int offset) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_FILES_UPDATE, sqe, -1, fds, nr_fds, offset);
    }

    default void io_uring_prep_fallocate(@Pointer IoUringSqe sqe, int fd, int mode, long offset, long len) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_FALLOCATE, sqe, fd, MemorySegment.NULL, mode, offset);
        sqe.setAddr(len);
    }

    @KernelVersionLimit(major = 5, minor = 15)
    default void io_uring_prep_openat(@Pointer IoUringSqe sqe, int dfd, /*const char * path*/@Pointer MemorySegment path, int flags, int mode) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_OPENAT, sqe, dfd, path, mode, 0);
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_openat_direct(@Pointer IoUringSqe sqe, int dfd, /*const char * path*/@Pointer MemorySegment path, int flags, int mode, int fileIndex) {
        io_uring_prep_openat(sqe, dfd, path, flags, mode);
        if (fileIndex == IoUringConstant.IORING_FILE_INDEX_ALLOC) {
            fileIndex--;
        }
        sqe.setFileIndex(fileIndex + 1);
    }

    @KernelVersionLimit(major = 5, minor = 15)
    default void io_uring_prep_close(@Pointer IoUringSqe sqe, int fd) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_CLOSE, sqe, fd, MemorySegment.NULL, 0, 0);
    }

    @KernelVersionLimit(major = 5, minor = 15)
    default void io_uring_prep_close_direct(@Pointer IoUringSqe sqe, int fileIndex) {
        io_uring_prep_close(sqe, 0);
        sqe.setFileIndex(fileIndex + 1);
    }

    default void io_uring_prep_read(@Pointer IoUringSqe sqe, int fd, @Pointer MemorySegment buf, int nbytes, long offset) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_READ, sqe, fd, buf, nbytes, offset);
    }

    default void io_uring_prep_read_multishot(@Pointer IoUringSqe sqe, int fd, int nbytes, long offset, int buf_group) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_READ_MULTISHOT, sqe, fd, MemorySegment.NULL, nbytes, offset);
        sqe.setFlags((byte) (sqe.getFlags() | IoUringConstant.IOSQE_BUFFER_SELECT));
        sqe.setBufGroup((short) buf_group);
    }

    default void io_uring_prep_write(@Pointer IoUringSqe sqe, int fd, @Pointer MemorySegment buf, int nbytes, long offset) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_WRITE, sqe, fd, buf, nbytes, offset);
    }

    default void io_uring_prep_statx(@Pointer IoUringSqe sqe, int dfd, /*const char * path*/@Pointer MemorySegment path, int flags, int mask,/* struct statx *statxbuf */ @Pointer MemorySegment statxbuf) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_STATX, sqe, dfd, path, mask, statxbuf.address());
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_fadvise(@Pointer IoUringSqe sqe, int fd, long offset, long len, int advice) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_FADVISE, sqe, fd, MemorySegment.NULL, advice, offset);
        sqe.setFlagsInFlagsUnion(advice);
    }

    default void io_uring_prep_madvise(@Pointer IoUringSqe sqe, @Pointer MemorySegment addr, long length, int advice) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_MADVISE, sqe, -1, addr, (int) length, 0);
        sqe.setFlagsInFlagsUnion(advice);
    }

    default void io_uring_prep_send(@Pointer IoUringSqe sqe, int sockfd, @Pointer MemorySegment buf, int nbytes, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_SEND, sqe, sockfd, buf, nbytes, 0);
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_send_set_addr(@Pointer IoUringSqe sqe,/*const struct sockaddr *dest_addr*/ @Pointer MemorySegment addr, short addrlen) {
        sqe.setAddr2(addr.address());
        sqe.setLen(addrlen);
    }

    default void io_uring_prep_sendto(@Pointer IoUringSqe sqe, int socketfd, @Pointer MemorySegment buf, int len, int flags,
            /*const struct sockaddr *addr*/ @Pointer MemorySegment addr, int addrlen) {
        io_uring_prep_send(sqe, socketfd, buf, len, flags);
        io_uring_prep_send_set_addr(sqe, addr, (short) addrlen);
    }

    @KernelVersionLimit(major = 6, minor = 0)
    default void io_uring_prep_send_zc(@Pointer IoUringSqe sqe, int sockfd, @Pointer MemorySegment buf, int len, int flags, int zc_flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_SEND_ZC, sqe, sockfd, buf, len, 0);
        sqe.setFlagsInFlagsUnion(flags);
        sqe.setIoprio((short) zc_flags);
    }

    @KernelVersionLimit(major = 6, minor = 0)
    default void io_uring_prep_send_zc_fixed(@Pointer IoUringSqe sqe, int sockfd,
                                             @Pointer MemorySegment buf, int len, int flags, int zc_flags,
                                             int buf_index) {
        io_uring_prep_send_zc(sqe, sockfd, buf, len, flags, zc_flags);
        sqe.setIoprio((short) (sqe.getIoprio() | IoUringConstant.IORING_RECVSEND_FIXED_BUF));
        sqe.setBufIndex((short) buf_index);
    }

    //todo 暂时未知是什么内核版本加入的
    default void io_uring_prep_sendmsg_zc(@Pointer IoUringSqe sqe, int fd, @Pointer MsgHdr msgHdr, int flags) {
        io_uring_prep_sendmsg(sqe, fd, msgHdr, flags);
        sqe.setOpcode(IoUringConstant.Opcode.IORING_OP_SENDMSG_ZC);
    }

    default void io_uring_prep_recv(@Pointer IoUringSqe sqe, int sockfd, @Pointer MemorySegment buf, int len, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_RECV, sqe, sockfd, buf, len, 0);
        sqe.setFlagsInFlagsUnion(flags);
    }

    @KernelVersionLimit(major = 5, minor = 19)
    default void io_uring_prep_recv_multishot(@Pointer IoUringSqe sqe, int sockfd, @Pointer MemorySegment buf, int len, int flags) {
        io_uring_prep_recv(sqe, sockfd, buf, len, flags);
        sqe.setIoprio((short) (sqe.getIoprio() | IoUringConstant.IORING_RECV_MULTISHOT));
    }

    default void io_uring_prep_recv_multishot(@Pointer IoUringSqe sqe, int sockfd, int flags) {
        io_uring_prep_recv(sqe, sockfd, MemorySegment.NULL, 0, flags);
        sqe.setIoprio((short) (sqe.getIoprio() | IoUringConstant.IORING_RECV_MULTISHOT));
    }

    default void io_uring_prep_epoll_ctl(@Pointer IoUringSqe sqe, int epfd, int fd, int op, @Pointer NativeEpollEvent event) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_EPOLL_CTL, sqe, epfd, StructProxyGenerator.findMemorySegment(event), op, fd);
    }

    default void io_uring_prep_provide_buffers(@Pointer IoUringSqe sqe, @Pointer MemorySegment addr, int len, int nr, int bgid, int bid) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_PROVIDE_BUFFERS, sqe, nr, addr, len, bid);
        sqe.setBufGroup((short) bgid);
    }

    default void io_uring_prep_remove_buffers(@Pointer IoUringSqe sqe, int nr, int bgid) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_REMOVE_BUFFERS, sqe, nr, MemorySegment.NULL, 0, 0);
        sqe.setBufGroup((short) bgid);
    }

    default void io_uring_prep_sync_file_range(@Pointer IoUringSqe sqe, int fd, int len, long offset, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_SYNC_FILE_RANGE, sqe, fd, MemorySegment.NULL, len, offset);
        sqe.setFlagsInFlagsUnion(flags);
    }

    @KernelVersionLimit(major = 5, minor = 18)
    default void io_uring_prep_msg_ring(@Pointer IoUringSqe sqe, int fd, int targetCqeRes, long targetCqeUserData, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_MSG_RING, sqe, fd, MemorySegment.NULL, targetCqeRes, targetCqeUserData);
        sqe.setFlagsInFlagsUnion(flags);
    }

    @KernelVersionLimit(major = 5, minor = 18)
    default void io_uring_prep_msg_ring_cqe_flags(@Pointer IoUringSqe sqe, int fd, int len, long data, int flags, int cqe_flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_MSG_RING, sqe, fd, MemorySegment.NULL, len, data);
        sqe.setFlagsInFlagsUnion(flags | IoUringConstant.IORING_MSG_RING_FLAGS_PASS);
        sqe.setFileIndex(cqe_flags);
    }

    @KernelVersionLimit(major = 5, minor = 18)
    default void io_uring_prep_msg_ring_fd(@Pointer IoUringSqe sqe, int fd, int sourceFd, int targetFd, long data, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_MSG_RING, sqe, fd, MemorySegment.ofAddress(IoUringConstant.IORING_MSG_SEND_FD), 0, data);
        sqe.setAddr3(sourceFd);
        if (targetFd == IoUringConstant.IORING_FILE_INDEX_ALLOC) {
            targetFd--;
        }
        sqe.setFileIndex(targetFd + 1);
        sqe.setFlagsInFlagsUnion(flags);
    }

    @KernelVersionLimit(major = 5, minor = 18)
    default void io_uring_prep_msg_ring_fd_alloc(@Pointer IoUringSqe sqe, int fd, int sourceFd, long data, int flags) {
        io_uring_prep_msg_ring_fd(sqe, fd, sourceFd, IoUringConstant.IORING_FILE_INDEX_ALLOC, data, flags);
    }

    @KernelVersionLimit(major = 5, minor = 19)
    default void io_uring_prep_socket(@Pointer IoUringSqe sqe, int domain, int type, int protocol, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_SOCKET, sqe, domain, MemorySegment.NULL, protocol, type);
        sqe.setFlagsInFlagsUnion(flags);
    }

    @KernelVersionLimit(major = 5, minor = 19)
    default void io_uring_prep_socket_direct(@Pointer IoUringSqe sqe, int domain, int type, int protocol, int fileIndex, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_SOCKET, sqe, domain, MemorySegment.NULL, protocol, type);
        if (fileIndex == IoUringConstant.IORING_FILE_INDEX_ALLOC) {
            fileIndex--;
        }
        sqe.setFileIndex(fileIndex + 1);
        sqe.setFlagsInFlagsUnion(flags);
    }

    @KernelVersionLimit(major = 5, minor = 19)
    default void io_uring_prep_socket_direct_alloc(@Pointer IoUringSqe sqe, int domain, int type, int protocol, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_SOCKET, sqe, domain, MemorySegment.NULL, protocol, type);
        sqe.setFlagsInFlagsUnion(flags);
        sqe.setFileIndex(IoUringConstant.IORING_FILE_INDEX_ALLOC);
    }

    @KernelVersionLimit(major = 6, minor = 5)
    default void io_uring_prep_waitid(@Pointer IoUringSqe sqe, int idtype, int id,/* siginfo_t *infop */ @Pointer MemorySegment infop, int options, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_WAITID, sqe, id, MemorySegment.NULL, idtype, 0);
        sqe.setFlagsInFlagsUnion(flags);
        sqe.setFileIndex(options);
        sqe.setAddr2(infop.address());
    }

    default void io_uring_prep_futex_wake(@Pointer IoUringSqe sqe, @Pointer MemorySegment futex, long val, long mask, int futex_flags, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_FUTEX_WAKE, sqe, futex_flags, futex, 0, val);
        sqe.setFlagsInFlagsUnion(flags);
        sqe.setAddr3(mask);
    }

    default void io_uring_prep_futex_wait(@Pointer IoUringSqe sqe, @Pointer MemorySegment futex, long val, long mask, int futex_flags, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_FUTEX_WAIT, sqe, futex_flags, futex, 0, val);
        sqe.setFlagsInFlagsUnion(flags);
        sqe.setAddr3(mask);
    }

    default void io_uring_prep_futex_waitv(@Pointer IoUringSqe sqe, @Pointer FutexWaitV futex, int nr_futex, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_FUTEX_WAITV, sqe, 0, StructProxyGenerator.findMemorySegment(futex), nr_futex, 0);
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_fixed_fd_install(@Pointer IoUringSqe sqe, int fd, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_FIXED_FD_INSTALL, sqe, fd, MemorySegment.NULL, 0, 0);
        sqe.setFlags((byte) IoUringConstant.IOSQE_FIXED_FILE);
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_ftruncate(@Pointer IoUringSqe sqe, int fd, long size) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_FTRUNCATE, sqe, fd, MemorySegment.NULL, 0, size);
    }

    default int io_uring_sq_ready(@Pointer IoUring ring) {
        MemorySegment realMemory = StructProxyGenerator.findMemorySegment(ring);
        int khead = (int) IoUringConstant.AccessShortcuts
                .IO_URING_SQ_KHEAD_DEFERENCE_VARHANDLE
                .get(realMemory, 0L);
        if ((ring.getFlags() & IoUringConstant.IORING_SETUP_SQPOLL) != 0) {
            khead = (int) IoUringConstant.AccessShortcuts
                    .IO_URING_SQ_KHEAD_DEFERENCE_VARHANDLE
                    .getAcquire(realMemory, 0L);
        }
        return (int) IoUringConstant.AccessShortcuts
                .IO_URING_SQ_SQE_TAIL_VARHANDLE
                .get(realMemory, 0L) - khead;
    }

    default int io_uring_sq_space_left(@Pointer IoUring ring) {
        MemorySegment realMemory = StructProxyGenerator.findMemorySegment(ring);
        return (int) IoUringConstant.AccessShortcuts.IO_URING_SQ_RING_ENTRIES_VARHANDLE.get(realMemory, 0L) - io_uring_sq_ready(ring);
    }

    default int io_uring_cq_ready(@Pointer IoUring ring) {
        MemorySegment realMemory = StructProxyGenerator.findMemorySegment(ring);
        return (int) IoUringConstant.AccessShortcuts.IO_URING_CQ_KTAIL_DEFERENCE_VARHANDLE.get(realMemory, 0L)
                - (int) IoUringConstant.AccessShortcuts.IO_URING_CQ_KHEAD_DEFERENCE_VARHANDLE.get(realMemory, 0L);
    }

    default boolean io_uring_cq_has_overflow(@Pointer IoUring ring) {
        MemorySegment realMemory = StructProxyGenerator.findMemorySegment(ring);
        int cqKFlags = (int) IoUringConstant.AccessShortcuts.IO_URING_SQ_KFLAGS_DEFERENCE_VARHANDLE.getAcquire(realMemory, 0L);
        return (cqKFlags & IoUringConstant.IORING_SQ_CQ_OVERFLOW) != 0;
    }


    private int io_uring_prep_poll_mask(int poll_mask) {
        //判断大小端
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
//            用于交换32位整数的高位字节和低位字节。
            poll_mask = Integer.reverseBytes(poll_mask);
        }
        return poll_mask;
    }


}
