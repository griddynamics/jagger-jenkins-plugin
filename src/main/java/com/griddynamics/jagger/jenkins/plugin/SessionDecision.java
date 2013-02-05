package com.griddynamics.jagger.jenkins.plugin;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Created with IntelliJ IDEA.
 * User: nmusienko
 * Date: 04.02.13
 * Time: 19:37
 * To change this template use File | Settings | File Templates.
 */
public class SessionDecision {
    //Xpath strings to decisions
    private static final String XPATH_COMPARISON_DECISION="/jagger/comparison/decision/text()";
    private static final String XPATH_SESSION_DECISION="/jagger/summary/sessionStatus/text()";


    private Decision comparisonDecision;
    private Decision sessionDecision;

    private SessionDecision(){
    }

    /**
     * Reading decisions from XML file
     * @param path - path to xml file
     * @return SessionDecision, which contains decisions from xml
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static SessionDecision create(String path) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        return create(path, null);
    }

    /**
     * Reading decisions from XML file
     * @param path - path to xml file
     * @param logger - stream for logging
     * @return SessionDecision, which contains decisions from xml
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static SessionDecision create(String path, PrintStream logger) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        SessionDecision decision=new SessionDecision();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new FileInputStream(path));
        try{
            decision.setComparisonDecision(readDecision(doc, XPATH_COMPARISON_DECISION));
        } catch (Exception e){
            if(logger!=null){
                logger.println("Exception while reading comparison decision: "+e);
            }
        }
        decision.setSessionDecision(readDecision(doc, XPATH_SESSION_DECISION));
        return decision;
    }

    /**
     * Read decision in Document by xpath
     * @param doc - XML Document
     * @param xpathString - xpath to decision
     * @return - Decision enum instance
     * @throws XPathExpressionException
     */
    private static Decision readDecision(Document doc, String xpathString) throws XPathExpressionException {
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        XPathExpression expr = xpath.compile(xpathString);
        String decisionString=expr.evaluate(doc, XPathConstants.STRING).toString();
        return Decision.valueOf(decisionString);
    }

    /**
     * Makes main decision by session decision and comparison decision
     * @return result decision
     */
    public Decision makeDecision(){
        if (sessionDecision == Decision.FATAL || comparisonDecision == Decision.FATAL){
            return Decision.FATAL;
        } else if (sessionDecision == Decision.WARNING || comparisonDecision == Decision.WARNING){
            return Decision.WARNING;
        } else return Decision.OK;
    }

    public Decision getSessionDecision() {
        return sessionDecision;
    }

    public void setSessionDecision(Decision sessionDecision) {
        this.sessionDecision = sessionDecision;
    }

    public Decision getComparisonDecision() {
        return comparisonDecision;
    }

    public void setComparisonDecision(Decision comparisonDecision) {
        this.comparisonDecision = comparisonDecision;
    }

}
