package com.varutra.webscarab.util;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

public class RegexExpansion {
    
    private String regex;
    private int size = 0;
    private int index = 0;
    private char[][] charsets;
    
    public RegexExpansion(String regex) throws PatternSyntaxException {
        this.regex = regex;
        List<List<Character>> charsets = new LinkedList<List<Character>>();
        List<Character> chars = new LinkedList<Character>();
        
        boolean inClass = false;
        boolean quoted = false;
        String quantifier = null;
        char range = '\0';
        for (int index = 0; index < regex.length(); index++) {
            char ch = regex.charAt(index);
            if (!quoted && !inClass && (ch == '.' || ch == '*' || ch == '?')) {
                throw new PatternSyntaxException("No wildcards permitted", regex, index);
            }
            if (quantifier != null && ch != '}') {
                if (!Character.isDigit(ch)) 
                    throw new PatternSyntaxException("Illegal non-digit character in quantifier", regex, index);
                quantifier = quantifier + ch;
                continue;
            } else if (quoted) {
                chars.add(new Character(ch));
                quoted = false;
            } else
                switch (ch) {
                    case '[' : 
                        inClass = true; 
                        continue;
                    case ']' : 
                        inClass = false; 
                        break;
                    case '\\' : 
                        quoted = true; 
                        continue;
                    case '{' :
                        if (charsets.size()==0)
                            throw new PatternSyntaxException("Illegal quantifier at start of regex", regex, index);
                        quantifier = ""; 
                        continue;
                    case '}' : 
                        try {
                            int c = Integer.parseInt(quantifier);
                            if (c == 0)
                                throw new PatternSyntaxException("Cannot repeat 0 times", regex, index);
                            for (int i=1; i<c; i++)
                                charsets.add(charsets.get(charsets.size()-1));
                        } catch (NumberFormatException nfe) {
                            throw new PatternSyntaxException(nfe.getMessage(), regex, index);
                        }
                        quantifier = null; 
                        continue;
                    case '-' : 
                        if (inClass) {
                            range = ((Character)chars.get(chars.size()-1)).charValue();
                            continue;
                        }
                    default :
                        if (range != '\0') {
                            if (ch<=range) throw new PatternSyntaxException("Illegal range definition", regex, index);
                            for (char q=++range;q<=ch;q++) 
                                chars.add(new Character(q));
                            range = '\0';
                        } else
                            chars.add(new Character(ch));
                }
            if (!inClass) {
                charsets.add(chars);
                chars = new LinkedList<Character>();
            }
        }
        this.charsets = new char[charsets.size()][];
        for (int i=0; i<charsets.size(); i++) {
            chars = (List<Character>) charsets.get(i);
            char[] t = new char[chars.size()];
            for (int j=0; j<chars.size();j++) {
                t[j] = ((Character) chars.get(j)).charValue();
            }
            this.charsets[i] = t;
        }
        this.size = 1;
        for (int i=0; i<this.charsets.length; i++) {
            this.size = this.size * this.charsets[i].length;
            if (size == 0)
                throw new PatternSyntaxException("Pattern expansion overflow at position " + i, regex, 0);
        }
    }
    
    protected RegexExpansion(RegexExpansion re) {
        this.regex = re.regex;
        this.charsets = re.charsets;
        this.size = re.size;
        this.index = 0;
    }
    
    public String getRegex() {
        return this.regex;
    }
    
    public int size() {
        return this.size;
    }
    
    public void setIndex(int index) {
        if (index >= size)
            throw new ArrayIndexOutOfBoundsException("Index out of bounds: " + index + " >= " + size);
        this.index = index;
    }
    
    public int getIndex() {
        return this.index;
    }
    
    public boolean hasNext() {
        return getIndex() < size();
    }
    
    public String next() {
        if (index >= size)
            throw new ArrayIndexOutOfBoundsException("Index out of bounds: " + index + " >= " + size);
        return get(index++);
    }
    
    public String get(int index) {
        if (index >= size)
            throw new ArrayIndexOutOfBoundsException("Index out of bounds: " + index + " >= " + size);
        StringBuffer buff = new StringBuffer(charsets.length);
        for (int i = charsets.length - 1; i >= 0; i--) {
            int mod = index % charsets[i].length;
            index = index / charsets[i].length;
            buff.insert(0,charsets[i][mod]);
        }
        return buff.toString();
    }

}