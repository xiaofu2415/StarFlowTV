package com.github.tvbox.osc.config;

import org.junit.Test;
import static org.junit.Assert.*;

public class RefreshRequestLockTest {
    @Test public void busyBDoesNotReleaseAAndCRemainsBusyUntilAFinishes() {
        RefreshRequestLock lock = new RefreshRequestLock();
        RefreshRequestLock.Lease a = lock.tryAcquire();
        assertNotNull(a);
        assertNull(lock.tryAcquire()); // B reports BUSY, owns no release handle.
        assertNull(lock.tryAcquire()); // C must still be BUSY.
        a.close();
        RefreshRequestLock.Lease c = lock.tryAcquire();
        assertNotNull(c);
        a.close(); // Duplicate completion of A cannot release C either.
        assertNull(lock.tryAcquire());
        c.close();
        assertNotNull(lock.tryAcquire());
    }
}
