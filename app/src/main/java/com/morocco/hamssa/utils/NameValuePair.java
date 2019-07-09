package com.morocco.hamssa.utils;

/**
 * Created by hmontaner on 06/06/15.
 */
public class NameValuePair {
    String name, value;
    public NameValuePair(String name, String value){
        this.name = name;
        this.value = value;
    }
    public NameValuePair(String name, Long value){
        this.name = name;
        this.value = value != null ? value+"" : null;
    }
    public NameValuePair(String name, Integer value){
        this.name = name;
        this.value = value != null ? value+"" : null;
    }
    public String getName(){
        return name;
    }
    public String getValue(){
        return value;
    }
}
