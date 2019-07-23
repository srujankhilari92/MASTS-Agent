package com.varutra.webscarab.plugin.proxy;

import java.io.PrintStream;

public interface BeanShellUI {
    
    PrintStream getOut();
    
    PrintStream getErr();
    
}
