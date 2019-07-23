package com.varutra.webscarab.util;

import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.PrintStream;

public class LogInputStream extends FilterInputStream {
    private PrintStream _ps;
    
    public LogInputStream(InputStream is, PrintStream ps) {
        super(is);
        if (is == null) {
            throw new NullPointerException("InputStream may not be null!");
        }
        _ps = ps;
    }
    
    public int read() throws IOException {
        int b = super.read();
        if (b > -1) {
            _ps.write(b);
            _ps.flush();
        } else {
            _ps.close();
        }
        return b;
    }
    
    public int read(byte[] b, int off, int len) throws IOException {
        int num = super.read(b, off, len);
        if (num > 0) {
            _ps.write(b,off,num);
            _ps.flush();
        } else {
            _ps.close();
        }
        return num;
    }

    public boolean markSupported() {
        return false;
    }
}