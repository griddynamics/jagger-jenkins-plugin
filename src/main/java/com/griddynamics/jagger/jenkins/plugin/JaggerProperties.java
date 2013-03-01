package com.griddynamics.jagger.jenkins.plugin;

import java.util.Arrays;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: amikryukov
 * Date: 1/15/13
 */
public class JaggerProperties extends Properties{

    /**
     * Add to Value new Value like this : >>old,new<<
     * If there is no key in properties, then key ,value sets like usual
     * @param key key
     * @param value value
     */
    public void addValueWithComma(String key, String value) {

        if(getProperty(key) != null) {
            setProperty(key, getProperty(key) + "," + value);
        } else {
            setProperty(key, value);
        }
    }



    public boolean containsRole(String role) {

        if(keySet().contains("chassis.roles")) {

            String roles = getProperty("chassis.roles");
            if(roles != null) {
                if(Arrays.asList(roles.split(",")).contains(role)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean containsRole(RoleTypeName role) {

       return containsRole(role.toString());
    }
}
