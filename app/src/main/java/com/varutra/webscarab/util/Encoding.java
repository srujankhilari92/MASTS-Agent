package com.varutra.webscarab.util;

import java.util.Arrays;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.net.URLDecoder;
import java.net.URLEncoder;

public final class Encoding {
    
    private Encoding() {}
    private static byte[] _base64en;
    private static byte[] _base64de;
    private static final byte B64INV = (byte) 0x80;
    
    static {
        _base64en = new byte[65];
        _base64de = new byte[256];
        Arrays.fill( _base64de, B64INV );
        for ( byte i = 0; i < 26; i++ ) {
            _base64en[ i ] = (byte) (65 + i);
            _base64en[ 26 + i ] = (byte) (97 + i);
            _base64de[ 65 + i ] = i;
            _base64de[ 97 + i ] = (byte) (26 + i);
        }
        for ( byte i = 48; i < 58; i++ ) {
            _base64en[ 4 + i ] = i;
            _base64de[ i ] = (byte) (4 + i);
        }
        _base64en[ 62 ] = 43;
        _base64en[ 63 ] = 47;
        _base64en[ 64 ] = 61;
        _base64de[ 43 ] = 62;
        _base64de[ 47 ] = 63;
        _base64de[ 61 ] = 0; // sic!
    }
    
    public static String base64encode( byte[] code ) {
        return base64encode(code, true);
    }
    
    public static String base64encode( byte[] code, boolean crlf) {
        if ( null == code )
            return null;
        if ( 0 == code.length )
            return new String();
        int len = code.length;
        int rem = len % 3;
        byte[] dst = new byte[4 + (((len - 1) / 3) << 2) + (crlf ? len / 57 : 0)];
        int column = 0;
        int spos = 0;
        int dpos = 0;
        len -= 2;
        while ( spos < len ) {
            byte b0 = code[ spos ];
            byte b1 = code[ spos + 1 ];
            byte b2 = code[ spos + 2 ];
            dst[ dpos++ ] = _base64en[ 0x3f & (b0 >>> 2) ];
            dst[ dpos++ ] = _base64en[ (0x30 & (b0 << 4)) + (0x0f & (b1 >>> 4)) ];
            dst[ dpos++ ] = _base64en[ (0x3c & (b1 << 2)) + (0x03 & (b2 >>> 6)) ];
            dst[ dpos++ ] = _base64en[ 0x3f & b2 ];
            spos += 3;
            column += 3;
            if ( crlf && 57 == column ) {
                dst[ dpos++ ] = 10;
                column = 0;
            }
        }
        if ( 0 != rem ) {
            byte b0 = code[ spos ];
            dst[ dpos++ ] = _base64en[ 0x3f & (b0 >>> 2) ];
            if ( 1 == rem ) {
                dst[ dpos++ ] = _base64en[ 0x30 & (b0 << 4) ];
                dst[ dpos++ ] = 61;
            } else {
                byte b1 = code[ spos + 1 ];
                dst[ dpos++ ] = _base64en[ (0x30 & (b0 << 4)) + (0x0f & (b1 >>> 4)) ];
                dst[ dpos++ ] = _base64en[ 0x3c & (b1 << 2) ];
            }
            dst[ dpos++ ] = 61;
        }
        return new String( dst );
    }
    
    public static byte[] base64decode( String coded ) {
        return base64decode(coded, true);
    }
    
    public static byte[] base64decode( String coded, boolean crlf ) {
        if ( null == coded )
            return null;
        byte[] src = coded.getBytes();
        int len = src.length;
        int dlen = len - (crlf ? len/77 : 0);
        dlen = (dlen >>> 2) + (dlen >>> 1);
        int rem = 0;
        if ( 61 == src[ len - 1 ] )
            rem++;
        if ( 61 == src[ len - 2 ] )
            rem++;
        dlen -= rem;
        byte[] dst = new byte[ dlen ];
        
        int pos = 0;
        int dpos = 0;
        int col = 0;
        len -= 4;
        
        while ( pos < len ) {
            byte b0 = _base64de[ src[ pos++ ] ];
            byte b1 = _base64de[ src[ pos++ ] ];
            byte b2 = _base64de[ src[ pos++ ] ];
            byte b3 = _base64de[ src[ pos++ ] ];
            
            if ( B64INV == b0 || B64INV == b1 || B64INV == b2 || B64INV == b3 )
                throw new RuntimeException( "Invalid character at or around position " + pos );
            
            dst[ dpos++ ] = (byte) ((b0 << 2) | ((b1 >>> 4) & 0x03));
            dst[ dpos++ ] = (byte) ((b1 << 4) | ((b2 >>> 2) & 0x0f));
            dst[ dpos++ ] = (byte) ((b2 << 6) | (b3 & 0x3f));
            col += 4;
            if ( crlf && 76 == col ) {
                if ( 10 != src[ pos++ ] )
                    throw new RuntimeException( "No linefeed found at position " + (pos - 1 ) );
                col = 0;
            }
        }
        
        byte b0 = _base64de[ src[ pos++ ] ];
        byte b1 = _base64de[ src[ pos++ ] ];
        byte b2 = _base64de[ src[ pos++ ] ];
        byte b3 = _base64de[ src[ pos++ ] ];
        if ( B64INV == b0 || B64INV == b1 || B64INV == b2 || B64INV == b3 )
            throw new RuntimeException( "Invalid character at or around position " + pos );
        
        dst[ dpos++ ] = (byte) ((b0 << 2) | ((b1 >>> 4) & 0x03));
        if ( 2 == rem )
            return dst;
        dst[ dpos++ ] = (byte) ((b1 << 4) | ((b2 >>> 2) & 0x0f));
        if ( 1 == rem )
            return dst;
        dst[ dpos++ ] = (byte) ((b2 << 6) | (b3 & 0x3f));
        
        return dst;
    }
    
    public static String toHexString( byte[] b ) {
        if ( null == b )
            return null;
        int len = b.length;
        byte[] hex = new byte[ len << 1 ];
        for ( int i = 0, j = 0; i < len; i++, j+=2 ) {
            hex[ j ] = (byte) ((b[ i ] & 0xF0) >> 4);
            hex[ j ] += 10 > hex[ j ] ? 48 : 87;
            hex[ j + 1 ] = (byte) (b[ i ] & 0x0F);
            hex[ j + 1 ] += 10 > hex[ j + 1 ] ? 48 : 87;
        }
        return new String( hex );
    }

    public static String hashMD5( String str ) {
        return hashMD5(str.getBytes());
    }
    
    public static String hashMD5( byte[] bytes ) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance( "MD5" );
            md.update( bytes );
        }
        catch ( NoSuchAlgorithmException e ) {
            e.printStackTrace();
        }
        return toHexString( md.digest() );
    }
    
    
    
    public static String hashSHA( String str ) {
        byte[] b = str.getBytes();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance( "SHA1" );
            md.update( b );
        }
        catch ( NoSuchAlgorithmException e ) {
            e.printStackTrace();
        }
        return toHexString( md.digest() );
    }
    
    public static synchronized String rot13( String input ) {
        StringBuffer output = new StringBuffer();
        if ( input != null ) {
            for ( int i = 0; i < input.length(); i++ ) {
                char inChar = input.charAt( i );
                if ( ( inChar >= 'A' ) & ( inChar <= 'Z' ) ) {
                    inChar += 13;
                    if ( inChar > 'Z' ) {
                        inChar -= 26;
                    }
                }
                if ( ( inChar >= 'a' ) & ( inChar <= 'z' ) ) {
                    inChar += 13;
                    if ( inChar > 'z' ) {
                        inChar -= 26;
                    }
                }
                output.append( inChar );
            }
        }
        return output.toString();
    }
    public static String urlDecode( String str ) {
        try {
            return ( URLDecoder.decode( str, "utf-8" ) );
        }
        catch ( Exception e ) {
            return ( "Decoding error" );
        }
    }
    
    
    public static String urlEncode( String str ) {
        try {
            return ( URLEncoder.encode( str, "utf-8" ) );
        }
        catch ( Exception e ) {
            return ( "Encoding error" );
        }
    }
    

}