package client.future;

import base.command.HttpResponse;
import client.listener.HttpFailureListener;
import client.listener.HttpSuccessListener;

import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by huoyijie on 18/10/18.
 */
public class HttpFuture implements Future<HttpResponse> {
    private final HttpSuccessListener successListener;
    private final HttpFailureListener failureListener;

    public HttpSuccessListener getSuccessListener() {
        return successListener;
    }
    public HttpFailureListener getFailureListener() {
        return failureListener;
    }

    public HttpFuture(HttpSuccessListener successListener, HttpFailureListener failureListener) {
        this.successListener = successListener;
        this.failureListener = failureListener;
    }

    private Lock lock = new ReentrantLock();
    private Condition doneCond = lock.newCondition();

    private volatile HttpResponse resp;
    public void setResp(HttpResponse resp) {
        assert resp != null;
        lock.lock();
        try {
            this.resp = resp;
            doneCond.signal();
        } finally {
            lock.unlock();
        }
    }

    private volatile Throwable cause;
    public void setCause(Throwable cause) {
        assert cause != null;
        lock.lock();
        try {
            this.cause = cause;
            doneCond.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new RuntimeException("Operation not supported!");
    }

    @Override
    public boolean isCancelled() {
        throw new RuntimeException("Operation not supported!");
    }

    @Override
    public boolean isDone() {
        lock.lock();
        try {
            return resp != null || cause != null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public HttpResponse get() throws InterruptedException, ExecutionException {
        try {
            __get_await__(0, null);
        } catch (TimeoutException e) {
        }
        return resp;
    }

    @Override
    public HttpResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        __get_await__(timeout, unit);
        return resp;
    }

    private void __get_await__ (long timeout, TimeUnit unit) throws InterruptedException, TimeoutException, ExecutionException {
        lock.lockInterruptibly();
        try {
            if (!isDone()) {
                if (timeout > 0 && unit != null) {
                    if(!doneCond.await(timeout, unit)) throw new TimeoutException();
                } else {
                    doneCond.await();
                }
            }
        } finally {
            lock.unlock();
        }
        if (cause != null) {
            throw new ExecutionException(cause);
        }
    }
}
