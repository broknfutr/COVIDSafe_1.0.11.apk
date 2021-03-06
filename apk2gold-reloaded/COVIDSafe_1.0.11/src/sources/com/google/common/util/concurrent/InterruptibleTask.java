package com.google.common.util.concurrent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

abstract class InterruptibleTask<T> extends AtomicReference<Runnable> implements Runnable {
    private static final Runnable DONE = new DoNothingRunnable();
    private static final Runnable INTERRUPTING = new DoNothingRunnable();
    private static final int MAX_BUSY_WAIT_SPINS = 1000;
    private static final Runnable PARKED = new DoNothingRunnable();

    private static final class DoNothingRunnable implements Runnable {
        public void run() {
        }

        private DoNothingRunnable() {
        }
    }

    /* access modifiers changed from: 0000 */
    public abstract void afterRanInterruptibly(@NullableDecl T t, @NullableDecl Throwable th);

    /* access modifiers changed from: 0000 */
    public abstract boolean isDone();

    /* access modifiers changed from: 0000 */
    public abstract T runInterruptibly() throws Exception;

    /* access modifiers changed from: 0000 */
    public abstract String toPendingString();

    InterruptibleTask() {
    }

    static {
        Class<LockSupport> cls = LockSupport.class;
    }

    public final void run() {
        Object obj;
        Thread currentThread = Thread.currentThread();
        if (compareAndSet(null, currentThread)) {
            boolean z = !isDone();
            if (z) {
                try {
                    obj = runInterruptibly();
                } catch (Throwable th) {
                    if (!compareAndSet(currentThread, DONE)) {
                        Runnable runnable = (Runnable) get();
                        boolean z2 = false;
                        int i = 0;
                        while (true) {
                            if (runnable != INTERRUPTING && runnable != PARKED) {
                                break;
                            }
                            i++;
                            if (i > 1000) {
                                Runnable runnable2 = PARKED;
                                if (runnable == runnable2 || compareAndSet(INTERRUPTING, runnable2)) {
                                    z2 = Thread.interrupted() || z2;
                                    LockSupport.park(this);
                                }
                            } else {
                                Thread.yield();
                            }
                            runnable = (Runnable) get();
                        }
                        if (z2) {
                            currentThread.interrupt();
                        }
                    }
                    if (z) {
                        afterRanInterruptibly(null, th);
                    }
                }
            } else {
                obj = null;
            }
            if (!compareAndSet(currentThread, DONE)) {
                Runnable runnable3 = (Runnable) get();
                boolean z3 = false;
                int i2 = 0;
                while (true) {
                    if (runnable3 != INTERRUPTING && runnable3 != PARKED) {
                        break;
                    }
                    i2++;
                    if (i2 > 1000) {
                        Runnable runnable4 = PARKED;
                        if (runnable3 == runnable4 || compareAndSet(INTERRUPTING, runnable4)) {
                            z3 = Thread.interrupted() || z3;
                            LockSupport.park(this);
                        }
                    } else {
                        Thread.yield();
                    }
                    runnable3 = (Runnable) get();
                }
                if (z3) {
                    currentThread.interrupt();
                }
            }
            if (z) {
                afterRanInterruptibly(obj, null);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public final void interruptTask() {
        Runnable runnable = (Runnable) get();
        if ((runnable instanceof Thread) && compareAndSet(runnable, INTERRUPTING)) {
            try {
                ((Thread) runnable).interrupt();
            } finally {
                if (((Runnable) getAndSet(DONE)) == PARKED) {
                    LockSupport.unpark((Thread) runnable);
                }
            }
        }
    }

    public final String toString() {
        String str;
        Runnable runnable = (Runnable) get();
        if (runnable == DONE) {
            str = "running=[DONE]";
        } else if (runnable == INTERRUPTING) {
            str = "running=[INTERRUPTED]";
        } else if (runnable instanceof Thread) {
            StringBuilder sb = new StringBuilder();
            sb.append("running=[RUNNING ON ");
            sb.append(((Thread) runnable).getName());
            sb.append("]");
            str = sb.toString();
        } else {
            str = "running=[NOT STARTED YET]";
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append(str);
        sb2.append(", ");
        sb2.append(toPendingString());
        return sb2.toString();
    }
}
