package com.varutra.webscarab.util;

import java.io.OutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.PrintStream;


public class LogOutputStream extends FilterOutputStream {
    OutputStream _os;
    PrintStream _ps;
    
    public LogOutputStream(OutputStream os, PrintStream ps) {
        super(os);
        _os = os;
        _ps = ps;
    }
    
    public void write(int b) throws IOException {
        _os.write(b);
        _ps.write(b);
    }
    
    public void write(byte b[], int off, int len) throws IOException {
        _os.write(b, off, len);
        _ps.write(b, off, len);
    }
    
}