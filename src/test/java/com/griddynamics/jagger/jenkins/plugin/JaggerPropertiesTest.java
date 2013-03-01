package com.griddynamics.jagger.jenkins.plugin;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: amikryukov
 * Date: 2/7/13
 */

public class JaggerPropertiesTest extends TestCase{

    static final String KEY = "key";
    static final String VALUE = "value";

    public void testAddValueWithCommaOk() throws Exception {

        JaggerProperties properties = new JaggerProperties();
        properties.setProperty(KEY,VALUE);
        properties.addValueWithComma(KEY,VALUE);

        assertEquals(properties.getProperty(KEY), VALUE + "," + VALUE);
    }

    public void testAddValueWithCommaNoKey() throws Exception {

        JaggerProperties properties = new JaggerProperties();
        properties.addValueWithComma(KEY, VALUE);
        assertEquals(properties.getProperty(KEY), VALUE);
    }

    public void testContainsRoleOk() throws Exception {

        JaggerProperties properties = new JaggerProperties();
        properties.setProperty("chassis.roles", "A,B,C,D");

        assertTrue(properties.containsRole("A"));
        assertTrue(properties.containsRole("B"));
        assertTrue(properties.containsRole("C"));
        assertTrue(properties.containsRole("D"));

        assertFalse(properties.containsRole("E"));
    }

    public void testContainsRoleNoKey() throws Exception {

        JaggerProperties properties = new JaggerProperties();
        properties.clear();

        assertFalse(properties.containsRole("E"));
        assertFalse(properties.containsRole(RoleTypeName.KERNEL));
    }
}
