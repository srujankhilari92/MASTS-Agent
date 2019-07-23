package com.varutra.webscarab.httpclient;

import java.util.ArrayList;
import java.util.logging.Logger;
import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.IOException;

public class ChunkedInputStream extends FilterInputStream {
    byte[] chunk = null;
    int start = 0;
    int size = 0;
    String[][] _trailer = null;
    private Logger _logger = Logger.getLogger(this.getClass().getName());
    
    public ChunkedInputStream(InputStream in) throws IOException {
        super(in);
        readChunk();
    }
    
    public String[][] getTrailer() {
        return _trailer;
    }
    
    private void readChunk() throws IOException {
        String line = readLine().trim();
        try {
            size = Integer.parseInt(line.trim(),16);
            _logger.finest("Expecting a chunk of " + size + " bytes");
            chunk = new byte[size];
            int read = 0;
            while (read < size) {
                int got = in.read(chunk,read, Math.min(1024,size-read));
                _logger.finest("read " + got + " bytes");
                if (got>0) {
                    read = read + got;
                } else if (read == 0) {
                    _logger.info("read 0 bytes from the input stream! Huh!?");
                } else {
                    _logger.info("No more bytes to read from the stream, read " + read + " of " + size);
                    continue;
                }
            }
            _logger.finest("Got " + size + " bytes");
            if (size == 0) { 
                readTrailer();
            } else {
                readLine(); 
            }
            start = 0;
        } catch (NumberFormatException nfe) {
            _logger.severe("Error parsing chunk size from '" + line + "' : " + nfe);
        }
    }
    
    public int read() throws IOException {
        if (size == 0) {
            return -1;
        }
        if (start == size) {
            readChunk();
        }
        if (size == 0) {
            return -1;
        }
        return chunk[start++];
    }
    
    public int read(byte[] b) throws IOException {
        return read(b,0,b.length);
    }
    
    public int read(byte[] b, int off, int len) throws IOException {
        if (size == 0) {
            return -1;
        }
        if (start == size) {
            readChunk();
        }
        if (size == 0) {
            return -1;
        }
        if (len - off < available()) {
        } else {
            len = available();
        }
        System.arraycopy(chunk, start, b, off, len);
        start += len;
        return len;
    }
    
    public int available() throws IOException {
        return size - start;
    }
    
    public boolean markSupported() {
        return false;
    }
    
    private String readLine() throws IOException {
        String line = new String();
        int i;
        byte[] b={(byte)0x00};
        i = in.read();
        while (i > -1 && i != 10 && i != 13) {
            b[0] = (byte)(i & 0xFF);
            String input = new String(b,0,1);
            line = line.concat(input);
            i = in.read();
        }
        if (i == 13) { 
            i = in.read();
        }
        _logger.finest("Read '" + line + "'");
        return line;
    }
    
    private void readTrailer() throws IOException {
        String line = readLine();
        ArrayList<String[]> trailer = new ArrayList<String[]>();
        while (!line.equals("")) {
            String[] pair = line.split(": *",2);
            if (pair.length == 2) {
                trailer.add(pair);
            }
            line = readLine();
        }
        if (trailer.size()>0) {
            _trailer = new String[trailer.size()][2];
            for (int i=0; i<trailer.size(); i++) {
                String[] pair = (String[]) trailer.get(i);
                _trailer[i][0] = pair[0];
                _trailer[i][1] = pair[1];
            }
        }
    }
}
