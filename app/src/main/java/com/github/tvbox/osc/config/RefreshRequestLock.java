package com.github.tvbox.osc.config;

/** A BUSY caller receives no lease and therefore cannot release another request. */
final class RefreshRequestLock {
    private Lease owner;

    synchronized Lease tryAcquire() {
        if (owner != null) return null;
        owner = new Lease();
        return owner;
    }

    final class Lease {
        boolean close() {
            synchronized (RefreshRequestLock.this) {
                if (owner != this) return false;
                owner = null;
                return true;
            }
        }
    }
}
