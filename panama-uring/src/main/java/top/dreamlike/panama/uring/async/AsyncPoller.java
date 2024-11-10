package top.dreamlike.panama.uring.async;

import top.dreamlike.panama.uring.async.cancel.CancelableFuture;
import top.dreamlike.panama.uring.async.trait.IoUringOperator;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.libs.LibUring;
import top.dreamlike.panama.uring.sync.trait.PollableFd;

public class AsyncPoller implements IoUringOperator {

    private final static LibUring LIB_URING = Instance.LIB_URING;

    private final IoUringEventLoop driver;

    public AsyncPoller(IoUringEventLoop driver) {
        this.driver = driver;
    }

    public CancelableFuture<Integer> register(PollableFd fd, int pollMask) {
        return driver.poll(fd, pollMask);
    }

    @Override
    public IoUringEventLoop owner() {
        return driver;
    }
}
