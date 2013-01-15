package com.griddynamics.jagger.jenkins.plugin;

import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: amikryukov
 * Date: 1/15/13
 */
public class MyProperties extends Properties{

    /**
     * Add to Value new Value like this : >>old,new<<
     * @param key key
     * @param value value
     */
    public void addValueWithComma( String key, String value){
        setProperty(key,getProperty(key) + "," + value);
    }
}
