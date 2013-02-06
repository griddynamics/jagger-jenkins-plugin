package com.griddynamics.jagger.jenkins.plugin;


import static org.testng.Assert.*;
import org.testng.annotations.Test;
import org.xml.sax.SAXParseException;

import java.io.File;

/**
 * JaggerComparisonPlugin Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>11/20/2012</pre>
 */
public class JaggerComparisonPluginTest {
    @Test(expectedExceptions = SAXParseException.class)
    public void brokenTest() throws Exception{
        SessionDecision.create(new File("src/test/resources/broken.xml")).makeDecision();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void wrongXPathTest() throws Exception{
        SessionDecision.create(new File("src/test/resources/wrongXPath.xml")).makeDecision();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void wrongDecisionTest() throws Exception{
        SessionDecision.create(new File("src/test/resources/wrongDecision.xml")).makeDecision();
    }

    @Test
    public void okTest() throws Exception{
        assertEquals(SessionDecision.create(new File("src/test/resources/ok.xml")).makeDecision(),Decision.OK);
    }

    @Test
    public void fatalTest() throws Exception{
        assertEquals(SessionDecision.create(new File("src/test/resources/fatal.xml")).makeDecision(),Decision.FATAL);
    }

    @Test
    public void warningTest() throws Exception{
        assertEquals(SessionDecision.create(new File("src/test/resources/warning.xml")).makeDecision(),Decision.WARNING);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void onlyComparisonTest() throws Exception{
        assertEquals(SessionDecision.create(new File("src/test/resources/onlyComparison.xml")).makeDecision(),Decision.WARNING);
    }

    @Test
    public void onlySummaryTest() throws Exception{
        assertEquals(SessionDecision.create(new File("src/test/resources/onlySummary.xml")).makeDecision(),Decision.OK);
    }
}

