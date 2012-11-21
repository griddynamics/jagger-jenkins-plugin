package com.griddynamics.jagger.jenkins.plugin;


import org.testng.Assert;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * JaggerComparisonPlugin Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>11/20/2012</pre>
 */
public class JaggerComparisonPluginTest {
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testErrorIgnoreFalse() throws Exception {
        JaggerComparisonPlugin jcp = new JaggerComparisonPlugin("src/test/resources/wrongDecision.xml", false);
        jcp.getDicision();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testErrorIgnoreTrue() throws Exception {
        JaggerComparisonPlugin jcp = new JaggerComparisonPlugin("src/test/resources/wrongDecision.xml", true);
        jcp.getDicision();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testExceptionIgnoreFalse() throws Exception {
        JaggerComparisonPlugin jcp = new JaggerComparisonPlugin("src/test/resources/wrongXPath.xml", false);
        jcp.getDicision();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testExceptionIgnoreTrue() throws Exception {
        JaggerComparisonPlugin jcp = new JaggerComparisonPlugin("src/test/resources/wrongXPath.xml", true);
        jcp.getDicision();
    }

    @Test
    public void testFatalIgnoreFalse() throws Exception {
        JaggerComparisonPlugin jcp = new JaggerComparisonPlugin("src/test/resources/fatal.xml", false);
        Assert.assertEquals(jcp.getDicision(), JaggerComparisonPlugin.Decision.FATAL);
    }

    @Test
    public void testFatalIgnoreTrue() throws Exception {
        JaggerComparisonPlugin jcp = new JaggerComparisonPlugin("src/test/resources/fatal.xml", true);
        Assert.assertEquals(jcp.getDicision(), JaggerComparisonPlugin.Decision.FATAL);
    }

    @Test
    public void testOkIgnoreFalse() throws Exception {
        JaggerComparisonPlugin jcp = new JaggerComparisonPlugin("src/test/resources/ok.xml", false);
        Assert.assertEquals(jcp.getDicision(), JaggerComparisonPlugin.Decision.OK);
    }

    @Test
    public void testOkIgnoreTrue() throws Exception {
        JaggerComparisonPlugin jcp = new JaggerComparisonPlugin("src/test/resources/ok.xml", true);
        Assert.assertEquals(jcp.getDicision(), JaggerComparisonPlugin.Decision.OK);
    }

    @Test
    public void testWarningIgnoreFalse() throws Exception {
        JaggerComparisonPlugin jcp = new JaggerComparisonPlugin("src/test/resources/warning.xml", false);
        Assert.assertEquals(jcp.getDicision(), JaggerComparisonPlugin.Decision.WARNING);
    }

    @Test
    public void testWarningIgnoreTrue() throws Exception {
        JaggerComparisonPlugin
                jcp = new JaggerComparisonPlugin("src/test/resources/warning.xml", true);
        Assert.assertEquals(jcp.getDicision(), JaggerComparisonPlugin.Decision.WARNING);
    }

    @Test(expectedExceptions = IOException.class)
    public void testFileDoesNotExistIgnoreFalse() throws Exception {
        JaggerComparisonPlugin jcp = new JaggerComparisonPlugin("src/test/resources/fileDoesNotExist.xml", false);
         jcp.getDicision();
    }

    @Test(expectedExceptions = IOException.class)
    public void testFileDoesNotExistIgnoreTrue() throws Exception {
        JaggerComparisonPlugin jcp = new JaggerComparisonPlugin("src/test/resources/fileDoesNotExist.xml", true);
        jcp.getDicision();
    }

    @Test(expectedExceptions = SAXException.class)
    public void testFileBrokenFalse() throws Exception {
        JaggerComparisonPlugin jcp = new JaggerComparisonPlugin("src/test/resources/broken.xml", false);
         jcp.getDicision();
    }

    @Test(expectedExceptions = SAXException.class)
    public void testFileBrokenTrue() throws Exception {
        JaggerComparisonPlugin jcp = new JaggerComparisonPlugin("src/test/resources/broken.xml", true);
        jcp.getDicision();
    }
}
