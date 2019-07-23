package com.varutra.webscarab.util;

import EDU.oswego.cs.dl.util.concurrent.ReentrantWriterPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;

import java.util.Iterator;

public class ReentrantReaderPreferenceReadWriteLock extends ReentrantWriterPreferenceReadWriteLock {
    
    private Sync _writeLock;
    
    public ReentrantReaderPreferenceReadWriteLock() {
        super();
        _writeLock = new LoggingLock(super.writeLock());
    }
    
    
    protected boolean allowReader() {
        return activeWriter_ == null || activeWriter_ == Thread.currentThread();
    }
    
    public void debug() {
        Iterator<?> it = readers_.keySet().iterator();
        System.err.println("Readers:");
        while(it.hasNext()) {
            Object key = it.next();
            Object value = readers_.get(key);
            System.err.println(key + " : " + value);
        }
        System.err.println("Done");
        System.err.println("Writer thread:");
        System.err.println(activeWriter_ == null ? null : activeWriter_.getName());
        System.err.println("Stack Trace:");
        Thread.dumpStack();
    }
    
    public EDU.oswego.cs.dl.util.concurrent.Sync writeLock() {
        return _writeLock;
    }
    
    private class LoggingLock implements Sync {
        
        private Sync _sync;
        
        public LoggingLock(Sync sync) {
            _sync = sync;
        }
        
        public void acquire() throws InterruptedException {
            while (!_sync.attempt(5000)) {
                debug();
            }
        }
        
        public boolean attempt(long msecs) throws InterruptedException {
            try {
                boolean result = _sync.attempt(msecs);
                if (result) {
                } else {
                    System.err.println(Thread.currentThread().getName() + "sync attempt unsuccessful");
                }
                return result;
            } catch (InterruptedException ie) {
                System.err.println(Thread.currentThread().getName() + " interrupted");
                throw ie;
            }
        }
        
        public void release() {
            _sync.release();
        }
        
    }
}