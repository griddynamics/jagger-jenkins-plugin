package com.griddynamics.jagger.jenkins.plugin;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.filters.StringInputStream;
import org.jvnet.winp.NotWindowsException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class JaggerEasyDeployPlugin extends Builder
{

    //to collect nodes in one field.
    private ArrayList<Node> nodList = new ArrayList<Node>();

    //the collect nodes to attack in one field.
    private ArrayList<SuT> sutsList = new ArrayList<SuT>();

    private final DBOptions dbOptions;

    private final AdditionalProperties additionalProperties;

    // where we will store properties for Jagger for each node
    private final String PROPERTIES_PATH = "/properties";

    private MyProperties commonProperties ;

    private final String envProperties;

    private StringBuilder deploymentScript;

    //path to Jagger Test Suit .zip
    private final String jaggerTestSuitePath;

    private String jaggerTestSuitePathActual;

    private String baseDir = "pwd";


    /**
     * Constructor where fields from *.jelly will be passed
     * @param sutsList
     *                      List of nodes to test
     * @param nodList
     *               List of nodes to do work
     * @param jaggerTestSuitePath test suite path
     * @param dbOptions properties of dataBase
     * @param additionalProperties properties from text area
     * @param envProperties properties for all nodes
     */
    @DataBoundConstructor
    public JaggerEasyDeployPlugin(ArrayList<SuT> sutsList, ArrayList<Node> nodList, String jaggerTestSuitePath, DBOptions dbOptions,
                                  AdditionalProperties additionalProperties, String envProperties) {

        this.dbOptions = dbOptions;
        this.sutsList = sutsList;
        this.nodList = nodList;
        this.jaggerTestSuitePath = jaggerTestSuitePath;
        this.jaggerTestSuitePathActual = jaggerTestSuitePath;
        this.additionalProperties = additionalProperties;

        this.envProperties = envProperties;
    }


    public String getEnvProperties() {
        return envProperties;
    }

    public String getJaggerTestSuitePathActual() {
        return jaggerTestSuitePathActual;
    }

    public void setJaggerTestSuitePathActual(String jaggerTestSuitePathActual) {
        this.jaggerTestSuitePathActual = jaggerTestSuitePathActual;
    }

    public DBOptions getDbOptions() {
        return dbOptions;
    }

    public AdditionalProperties getAdditionalProperties() {
        return additionalProperties;
    }

    public String getJaggerTestSuitePath() {
        return jaggerTestSuitePath;
    }

    public ArrayList<SuT> getSutsList() {
        return sutsList;
    }

    public ArrayList<Node> getNodList() {
        return nodList;
    }

    public StringBuilder getDeploymentScript() {
        return deploymentScript;
    }

    /**
     * Loading EnvVars and create properties_files
     * @param build .
     * @param listener .
     * @return true
     */
    @Override
    public boolean prebuild(Build build, BuildListener listener) {

        PrintStream logger = listener.getLogger();

        try {


            checkUsesOfEnvironmentProperties(build.getEnvVars());
            //where will be checked all Nodes, SUTs, DB options, additional envProperties, Jagger suit. !!!
          //  checkAddressesOnBuildVars(build.getEnvVars());//build.getEnvVars() this works, but deprecated

            setUpCommonProperties();
         } catch (IOException e) {
            logger.println(e);
            return false;
        }


        //create folder to collect properties files
        File folder = new File(build.getWorkspace() + PROPERTIES_PATH);

        try {

            //delete previous build properties
            FileUtils.deleteDirectory(folder);
            //make dirs if they not exists
            FileUtils.forceMkdir(folder);

            if(nodList.size() == 1) {
               // generateOneNodePropertiesFile(nodList.get(0), folder);
            } else {
                for(Node node: nodList){

                    generatePropertiesFile(node, folder);
                }
            }

            generateScriptToDeploy();

            logger.println("\n-------------Deployment-Script-------------------\n");
            logger.println(deploymentScript.toString());
            logger.println("\n-------------------------------------------------\n\n");

        } catch (IOException e) {

            logger.println("!!!\nWhile generating properties files/deploy script\nException in preBuild: " + e );
            for(StackTraceElement element : e.getStackTrace()) {
                logger.println(element.getMethodName() + "\t" + element.getLineNumber());
            }

            if(folder.exists()){
                try {
                    FileUtils.deleteDirectory(folder);
                } catch (IOException e1) {
                    logger.println("Can`t delete " + folder + " after failed prebuild");
                }
            }

            return false;
        }

        return true;
    }

    private void generateOneNodePropertiesFile(Node node, File folder) {
        //To change body of created methods use File | Settings | File Templates.
    }


    /**
     * provide ability to use environment properties
     * @param envVars environment properties
     */
    private void checkUsesOfEnvironmentProperties(Map<String, String> envVars) {

        checkNodesOnBuildVars(envVars);
        checkAdditionalPropertiesOnBuildVars(envVars);
        checkJaggerTestSuitOnBuildVars(envVars);
        checkDBOptionsOnBuildVars(envVars);
        checkAgentsOnBuildVars(envVars);


    }

    private void checkAgentsOnBuildVars(Map<String, String> ev) {

        if(sutsList != null) {
            for(SuT node : sutsList) {
                checkSshNodesServerAddresses(ev, node);
                checkSshNodesSSHKeyPath(ev, node);
                checkSshNodesUserName(ev, node);
            }
        }
    }


    private void checkDBOptionsOnBuildVars(Map<String, String> ev) {

        String temp = dbOptions.getRdbDialect();
        Matcher me = Pattern.compile(".*(\\$\\{(.+)\\}).*").matcher(temp);

        while(me.find()) {
            String t = me.group(1);
            String t1 = me.group(2);
            if(ev.containsKey(t1)) {

                temp = temp.replace(t, ev.get(t1));
                dbOptions.setRdbDialectActual(temp);
            }
        }


        temp = dbOptions.getRdbUserName();
        me = Pattern.compile(".*(\\$\\{(.+)\\}).*").matcher(temp);

        while(me.find()) {
            String t = me.group(1);
            String t1 = me.group(2);
            if(ev.containsKey(t1)) {

                temp = temp.replace(t, ev.get(t1));
                dbOptions.setRdbUserNameActual(temp);
            }
        }


        temp = dbOptions.getRdbClientUrl();
        me = Pattern.compile(".*(\\$\\{(.+)\\}).*").matcher(temp);

        while(me.find()) {
            String t = me.group(1);
            String t1 = me.group(2);
            if(ev.containsKey(t1)) {

                temp = temp.replace(t, ev.get(t1));
                dbOptions.setRdbClientUrlActual(temp);
            }
        }


        temp = dbOptions.getRdbDriver();
        me = Pattern.compile(".*(\\$\\{(.+)\\}).*").matcher(temp);

        while(me.find()) {
            String t = me.group(1);
            String t1 = me.group(2);
            if(ev.containsKey(t1)) {

                temp = temp.replace(t, ev.get(t1));
                dbOptions.setRdbDriverActual(temp);
            }
        }

    }

    private void checkJaggerTestSuitOnBuildVars(Map<String, String> ev) {

        String temp = getJaggerTestSuitePath();
        Matcher me = Pattern.compile(".*(\\$\\{(.+)\\}).*").matcher(temp);

        while(me.find()) {
            String t = me.group(1);
            String t1 = me.group(2);
            if(ev.containsKey(t1)) {

                temp = temp.replace(t, ev.get(t1));
                setJaggerTestSuitePathActual(temp);
            }
        }

    }


    private void checkAdditionalPropertiesOnBuildVars(Map<String, String> ev) {

        String temp = additionalProperties.getTextFromArea();
        Matcher me = Pattern.compile(".*(\\$\\{(.+)\\}).*", Pattern.MULTILINE).matcher(temp);

        while(me.find()) {
            String t = me.group(1);
            String t1 = me.group(2);
            if(ev.containsKey(t1)) {

                temp = temp.replace(t, ev.get(t1));

                additionalProperties.setTextFromAreaActual(temp);
            }
        }
    }


    /**
     * Check if Build Variables contain addresses , or VERSION (of Jagger)
     * @param ev Build Variables
     */
    private void checkNodesOnBuildVars(Map<String,String> ev) {

        for(Node node: nodList){

            checkSshNodesServerAddresses(ev, node);
            checkSshNodesUserName(ev, node);
            checkNodesPropertiesPath(ev, node);
            checkSshNodesSSHKeyPath(ev, node);

        }

    }

    private void checkSshNodesSSHKeyPath(Map<String, String> ev, SshNode node) {

        String temp = node.getSshKeyPath();
        Matcher me = Pattern.compile(".*(\\$\\{(.+)\\}).*").matcher(temp);

        if(me.find()) {
            String t = me.group(1);
            String t1 = me.group(2);
            if(ev.containsKey(t1)) {

                temp = temp.replace(t, ev.get(t1));

                node.setSshKeyPathActual(temp);
            }
        }
    }


    private void checkNodesPropertiesPath(Map<String,String> ev, Node node) {

        String temp = node.getPropertiesPath();
        Matcher me = Pattern.compile(".*(\\$\\{(.+)\\}).*").matcher(temp);

        if(me.find()) {
            String t = me.group(1);
            String t1 = me.group(2);
            if(ev.containsKey(t1)) {

                temp = temp.replace(t, ev.get(t1));

                node.setPropertiesPathActual(temp);
            }
        }
    }


    private void checkSshNodesUserName(Map<String,String> ev, SshNode node) {

        String temp = node.getUserName();
        Matcher me = Pattern.compile(".*(\\$\\{(.+)\\}).*").matcher(temp);

        if(me.find()) {
            String t = me.group(1);
            String t1 = me.group(2);
            if(ev.containsKey(t1)) {

                temp = temp.replace(t, ev.get(t1));

                node.setUserNameActual(temp);
            }
        }
    }


    private void checkSshNodesServerAddresses(Map<String,String> ev, SshNode node) {

        String temp = node.getServerAddress();
        Matcher me = Pattern.compile(".*(\\$\\{(.+)\\}).*").matcher(temp);

        if(me.find()) {
            String t = me.group(1);
            String t1 = me.group(2);
            if(ev.containsKey(t1)) {

                temp = temp.replace(t, ev.get(t1));

                node.setServerAddressActual(temp);
            }
        }
    }


    /**
     * generating script like in smoke-test, to execute it in perform method
     * @throws java.io.IOException w
     */
    private void generateScriptToDeploy() throws IOException {

        StringBuilder script = new StringBuilder();
        script.append("#!/bin/bash\n\n");
        script.append("TimeStart=`date +%y/%m/%d_%H:%M`\n\n");

        killOldJagger(script);

        script.append("sleep 5\n");

        startAgents(script);

        startNodes(script);

        script.append("sleep 5\n");

        copyReports(script);

        copyAllLogs(script);

        script.append("\n\n#mutt -s \"Jenkins[JGR-stable-testplan][$TimeStart]\" jagger@griddynamics.com\n");

        String key = "chassis.master.reporting.report.file.name";

        if(commonProperties.containsKey(key)) {

            script.append("zip -9 ").append(baseDir).append("/report.zip ").append(baseDir);
            script.append("/").append(commonProperties.getProperty(key)).append(" ").append(baseDir).append("/result.xml\n");
        } else {

            script.append("zip -9 ").append(baseDir).append("/report.zip ").append(baseDir);
            script.append("/report.pdf ").append(baseDir).append("/result.xml\n");
        }

        deploymentScript = script;

    }

    private void copyAllLogs(StringBuilder script) {

        script.append("\n");

        copyMastersLogs(script);

        copyKernelsLogs(script);

        copyAgentsLogs(script);
    }

    private void copyAgentsLogs(StringBuilder script) {

        if(sutsList != null){
            for(SuT node : sutsList) {
                if(node != null) {
                    script.append("\necho \"Copy agents logs\"\n");
                    copyLogs(node.getUserNameActual(), node.getServerAddressActual(), node.getSshKeyPathActual(), script);
                }
            }
        }
    }

    private void copyKernelsLogs(StringBuilder script) {

        for (Node node: nodList) {
            if(node != null && node.getKernel() != null && node.getMaster() == null) {
                script.append("\necho \"Copy kernels logs\"\n");
                copyLogs(node.getUserNameActual(), node.getServerAddressActual(), node.getSshKeyPathActual(), script);
            }
        }
    }

    private void copyMastersLogs(StringBuilder script) {

        if(nodList.size() == 1) {
             script.append("\necho \"Copy master logs\"\n");
                copyLogs(nodList.get(0).getUserNameActual(), nodList.get(0).getServerAddressActual(), nodList.get(0).getSshKeyPathActual(), script);
        } else  {
            for (Node node: nodList) {
                if(node != null && node.getMaster() != null) {
                    script.append("\necho \"Copy master logs\"\n");
                    copyLogs(node.getUserNameActual(), node.getServerAddressActual(), node.getSshKeyPathActual(), script);
                }
            }
        }
    }


    private void copyLogs(String userName, String address, String keyPath, StringBuilder script) {

        String jaggerHome = "/home/" + userName + "/runned_jagger";

        doOnVmSSH(userName, address, keyPath, "cd " + jaggerHome + "; zip -9 " + address + ".logs.zip jagger.log*", script);
        script.append("\n");

        scpGetKey(userName, address, keyPath, jaggerHome + "/" + address + ".logs.zip", baseDir, script);// + + + ++ + ++  ++ + + ++
    }


    private void copyReports(StringBuilder script) {

        for(Node node : nodList){

            if(nodList.size() == 1){

                String userName = node.getUserNameActual();
                String address = node.getServerAddressActual();
                String keyPath = node.getSshKeyPathActual();
                String jaggerHome = "/home/" + userName + "/runned_jagger";


                script.append("\n\necho \"Copy reports\"\n");

                String key = "chassis.master.reporting.report.file.name";

                if(commonProperties.containsKey(key)) {  //it means that user specify it by plugin, or within properties file
                    scpGetKey(userName,
                            address,
                            keyPath,
                            jaggerHome + "/" + commonProperties.getProperty(key),
                            baseDir,
                            script);
                } else {
                    scpGetKey(userName,
                            address,
                            keyPath,
                            jaggerHome + "/report.pdf", //default value for report file
                            baseDir,
                            script);
                }

                scpGetKey(userName,
                        address,
                        keyPath,
                        jaggerHome + "/result.xml",
                        baseDir,
                        script);
            }
        }

    }


    /**
     * Starting Nodes with specific property file for each
     * @param script deploymentScript
     * @throws java.io.IOException `
     */
    private void startNodes(StringBuilder script) throws IOException {

        //here we should run start.sh with properties file that on our computer, not on Nodes.
        //it means that first - we should transfer it to Node
        script.append("\necho \"Copying properties to remote Nodes and start\"\n");


        if(nodList.size() == 1) {
            String userName = nodList.get(0).getUserNameActual();
            String address = nodList.get(0).getServerAddressActual();
            String keyPath = nodList.get(0).getSshKeyPathActual();
            String propertiesPath = nodList.get(0).getPropertiesPathActual();
            String jaggerHome = "/home/" + userName + "/runned_jagger";

            script.append("echo \"").append(address).append(" : cd ").append(jaggerHome).append("; ./start.sh properties_file\"\n");
//!!!                                                        //for testing
            String command = "source /etc/profile ; cd " + jaggerHome + "; ./start.sh " + propertiesPath + " -Xmx1550m -Xms1550m";
            if(additionalProperties.isDeclared()) {

                BufferedReader br = new BufferedReader(new StringReader(additionalProperties.getTextFromAreaActual()));
                while (br.ready()) {
                    command += " " + br.readLine();
                }
            }

            doOnVmSSH(userName, address, keyPath,
                    command
                    , script);
            script.append(" > /dev/null\n\n");
        } else {
            Node coordinator = null; //COORDINATION_SERVER should start in very last order

            for(Node node : nodList) {
                String userName = node.getUserNameActual();
                String address = node.getServerAddressActual();
                String keyPath = node.getSshKeyPathActual();
                String jaggerHome = "/home/" + userName + "/runned_jagger";
                String newPropPath = jaggerHome + "/tempPropertiesToDeploy.property";

//                File property = new File(newPropPath);
//                if(!property.exists()){
//                    //noinspection ResultOfMethodCallIgnored
//                    property.mkdirs();
//                }
//                scpSendKey(userName, address, keyPath, node.getFinalPropertiesPath(), newPropPath,script);
//                node.setFinalPropertiesPath(newPropPath);
//
//                MyProperties temp = new MyProperties();
//
//                if(!node.getPropertiesPathActual().matches("\\s*")) {
//                    temp.load(new FileInputStream(node.getPropertiesPathActual()));
//                }

                if(node.getCoordinationServer() != null ) {
                    coordinator = node;
                } else {
                    script.append("echo \"").append(address).append(" : cd ").append(jaggerHome).append("; ./start.sh properties_file\"\n");
    //!!!                                                        //for testing
                    doOnVmSSHDaemon(userName, address, keyPath,
                            "source /etc/profile ; cd " + jaggerHome + "; ./start.sh " + node.getPropertiesPathActual() + " -Xmx1550m -Xms1550m"
                            , script);
                    script.append(" > /dev/null\n\n");
                }
            }

            if (coordinator != null) {
                String userName = coordinator.getUserNameActual();
                String address = coordinator.getServerAddressActual();
                String keyPath = coordinator.getSshKeyPathActual();
                String jaggerHome = "/home/" + userName + "/runned_jagger";

                script.append("echo \"").append(address).append(" : cd ").append(jaggerHome).append("; ./start.sh ").append("properties_file\"\n");

                                                                                                                        //-Xmx1550m -Xms1550m
                doOnVmSSH(userName, address, keyPath,
    //!!!                 //for testing
                        "source /etc/profile ;cd " + jaggerHome + "; ./start.sh " + coordinator.getPropertiesPathActual() + " -Xmx1550m -Xms1550m", script);
                script.append(" > /dev/null\n\n");
            } else {
                throw new IllegalArgumentException("no coordinator");
            }
        }
    }


    /**
     * Starting Agents, if it declared
     * @param script deploymentScript
     */
    private void startAgents(StringBuilder script) {

        if (sutsList != null) {
            for(SuT node : sutsList){

                    String jaggerHome = "/home/" + node.getUserNameActual() + "/runned_jagger";

                    killOldJagger1(node.getUserNameActual(), node.getServerAddressActual(), node.getSshKeyPathActual(), jaggerHome, script);

                    script.append("\necho \"Starting Agent\"\n");
                    script.append("echo \"").append(node.getServerAddressActual()).append(" : cd ").append(jaggerHome).append("; ./start_agent.sh\"\n");
    //!!!                           //for testing
                    String command = "source /etc/profile  ;cd " + jaggerHome + "; ./start_agent.sh -Xmx1550m -Xms1550m -Dchassis.coordination.http.url=" + commonProperties.get("chassis.coordination.http.url");
                    doOnVmSSHDaemon(node.getUserNameActual(), node.getServerAddressActual(), node.getSshKeyPathActual(), command, script);
                    script.append("> /dev/null\n");

            }
        }
    }


    /**
     * kill old Jagger , deploy new one , stop processes in jagger
     * @param script String Builder of deployment Script
     */
    private void killOldJagger(StringBuilder script) {

        script.append("\necho \"KILLING old jagger\"\n\n");
        for(Node node:nodList){

            String jaggerHome = "/home/" + node.getUserNameActual() + "/runned_jagger";

            killOldJagger1(node.getUserNameActual(),node.getServerAddressActual(), node.getSshKeyPathActual(), jaggerHome,  script);
        }

    }


    private void killOldJagger1(String userName, String serverAddress, String keyPath, String jaggerHome, StringBuilder script){

        script.append("echo \"TRYING TO DEPLOY NODE ").append(userName).append("@").append(serverAddress).append("\"\n");
        doOnVmSSH(userName, serverAddress, keyPath, "rm -rf " + jaggerHome, script);
        script.append("\n");
        doOnVmSSH(userName, serverAddress, keyPath, "mkdir " + jaggerHome, script);
        script.append("\n");

        scpSendKey(userName,
                serverAddress,
                keyPath,
                jaggerTestSuitePath,
                jaggerHome, script);

        String jaggerFileName = jaggerTestSuitePath;
        int index = jaggerTestSuitePath.lastIndexOf('/');
        if(index >= 0) {
            jaggerFileName = jaggerTestSuitePath.substring(index + 1);
        }

        doOnVmSSH(userName, serverAddress, keyPath,
                "unzip " + jaggerHome + "/" + jaggerFileName + " -d " + jaggerHome, script);
        script.append(" > /dev/null");

        script.append("\n\necho \"KILLING previous processes ").append(userName).append("@").append(serverAddress).append("\"\n");
        doOnVmSSH(userName, serverAddress, keyPath, jaggerHome + "/stop.sh", script);
        script.append("\n");
        doOnVmSSH(userName, serverAddress, keyPath, jaggerHome + "/stop_agent.sh", script);
        script.append("\n");
        doOnVmSSH(userName, serverAddress, keyPath, "rm -rf /home/" + userName + "/jaggerdb", script);
        script.append("\n\n");
    }





    /**
     * rewriting fields on special class foe properties
     * @throws java.io.IOException  if smthg wrong while reading properties file
     */
    private void setUpCommonProperties() throws IOException {

        commonProperties = new MyProperties();

        int minAgents = 0;
        if (sutsList != null) {
            minAgents = sutsList.size();
        }

        int minKernels = 0;

        setUpRdbProperties();

        if(nodList.size() == 1){

            setUpCoordinationServerPropeties(nodList.get(0));

        } else {

//            MyProperties nodesProps = new MyProperties();
//            for(Node node : nodList) {
//
//                nodesProps.clear();
//                if(!node.getPropertiesPathActual().matches("\\s*")){//Actual
//                    nodesProps.load(new FileInputStream(node.getPropertiesPathActual()));
//                }
//
//                if(node.getCoordinationServer() !=  null || nodesProps.containsRole(RoleTypeName.COORDINATION_SERVER)) {
//                    setUpCoordinationServerPropeties(node);
//                }
//
//                if(node.getKernel() != null || nodesProps.containsRole(RoleTypeName.KERNEL)) {
//                    minKernels ++;
//                }
//
//    //            if(node.getMaster() != null || nodesProps.containsRole(RoleTypeName.MASTER)) {
//    //                setUpMasterProperties(node);
//    //            }
//
//                if(node.getReporter() != null || nodesProps.containsRole(RoleTypeName.REPORTER)) {
//             //       setUpReporter(node, nodesProps);
//                }
//            }
        }




        commonProperties.setProperty("chassis.conditions.min.agents.count", String.valueOf(minAgents));
        commonProperties.setProperty("chassis.conditions.min.kernels.count", String.valueOf(minKernels));

        commonProperties.setProperty("jagger.default.environment.properties", "./configuration/basic/default.environment.properties");

    }


    /**
     * Set Up Reporter properties : file name, format - html, pdf;
     * if Reporter wont be set, or set via properties file, default values ​​will be used
     * @param node node that plays Reporter Role
     * @param nodesProps  properties from node.propertiesPath
     */
    private void setUpReporter(Node node, MyProperties nodesProps) {

        if(node.getReporter() != null) {
//            commonProperties.setProperty("chassis.master.reporting.report.format", node.getReporter().getFormat());
//            commonProperties.setProperty("chassis.master.reporting.report.file.name", node.getReporter().getFileName());
        } else {

            String defaultFormat = "PDF";

            String key = "chassis.master.reporting.report.format";
            String format = nodesProps.getProperty(key);
            if(format != null){
                commonProperties.setProperty(key, format);
            } else {
                commonProperties.setProperty(key, defaultFormat);
            }

            key = "chassis.master.reporting.report.file.name";
            String fileName = nodesProps.getProperty(key);
            if(fileName != null){
                commonProperties.setProperty(key, fileName);
            } else {
                if(format != null) {
                    commonProperties.setProperty(key, "report." + format.toLowerCase());
                } else {
                    commonProperties.setProperty(key, "report." + defaultFormat.toLowerCase());
                }
            }
        }

    }


//set Up Master Properties .
//    private void setUpMasterProperties(Node node) {
//seems that tcpPort is hardcode property
//            if(!dbOptions.isUseExternalDB()) {
//                if(!node.getPropertiesPath().matches("\\s*")) {
//                    String key = "tcpPort" ;          // seems tcpPort only Jaggers property, hardcode
//                    try {
//                        Properties prope = new Properties();
//                        prope.load(new FileInputStream(node.getPropertiesPath()));
//                        String temp = prope.getProperty(key);
//                        if(temp == null) {
//                            commonProperties.setProperty(key, "8043");
//                        } else {
//                            commonProperties.setProperty(key, temp);
//                        }
//                    } catch (IOException e) {
//                        commonProperties.setProperty("IOException","while reading " + node.getPropertiesPath());
//                    }
//                }
//            }
//    }


//   Decide to point services in additional properties
//    /**
//     * Setting up Common Properties for SUTes
//     * @param node node to attack
//     */
//    private void setUpNodeToAttack(SuT node) {
//
//        String key = "test.service.endpoints";
//        if(commonProperties.get(key) == null){
//            commonProperties.setProperty(key, node.getServerAddressActual());
//        } else {
//            commonProperties.addValueWithComma(key, node.getServerAddressActual());
//        }
//    }

    /**
     * Setting up Common Properties for Nodes
     * @param node Node that play CoordinationServer Role
     */
    private void setUpCoordinationServerPropeties(Node node) {

            commonProperties.setProperty("chassis.coordinator.zookeeper.endpoint", node.getServerAddressActual() +
                    ":2181");          //hardcode for a while - seems that that is hardcode in jager
            //Is this property belong to Coordination Server
            commonProperties.setProperty("chassis.storage.fs.default.name","hdfs://"+node.getServerAddressActual() + "/");
            commonProperties.setProperty("chassis.coordination.http.url","http://" + node.getServerAddressActual() + ":8089");  //hardcode for a while - seems that that is hardcode in jager
            //port of http.url hard code? or it can be set somewhere
    }

    /**
     * Setting up Common Properties for Nodes
     * @throws java.io.IOException while loading properties from file
     */
    private void setUpRdbProperties() throws IOException {


        if(!dbOptions.isUseExternalDB() && nodList.size() == 1) {

            String port = "8043";

            commonProperties.setProperty("chassis.storage.rdb.client.driver", "org.h2.Driver");
            commonProperties.setProperty("chassis.storage.rdb.client.url","jdbc:h2:tcp://" +
                            nodList.get(0).getServerAddressActual() + ":" + port +"/jaggerdb/db");
            commonProperties.setProperty("chassis.storage.rdb.username","jagger");
            commonProperties.setProperty("chassis.storage.rdb.password","rocks");
            commonProperties.setProperty("chassis.storage.hibernate.dialect","org.hibernate.dialect.H2Dialect");

        } else if (!dbOptions.isUseExternalDB() && nodList.size() > 1){

            String address = "NO_MASTER_DETECTED";

            String port = "8043";

            MyProperties nodeProp = new MyProperties();
            for(Node node: nodList) {

                if(!node.getPropertiesPathActual().matches("\\s*")){
                    nodeProp.clear();
        //            nodeProp.load(new FileInputStream(node.getPropertiesPathActual()));
                }
                if(node.getMaster() != null || nodeProp.containsRole(RoleTypeName.MASTER.toString())) {
                    address = node.getServerAddressActual();
                    break;
                }
            }

            commonProperties.setProperty("chassis.storage.rdb.client.driver", "org.h2.Driver");
            commonProperties.setProperty("chassis.storage.rdb.client.url","jdbc:h2:tcp://" +
                            address + ":" + port +"/jaggerdb/db");
            commonProperties.setProperty("chassis.storage.rdb.username","jagger");
            commonProperties.setProperty("chassis.storage.rdb.password","rocks");
            commonProperties.setProperty("chassis.storage.hibernate.dialect","org.hibernate.dialect.H2Dialect");

        } else {

            commonProperties.setProperty("chassis.storage.rdb.client.driver", dbOptions.getRdbDriverActual());
            commonProperties.setProperty("chassis.storage.rdb.client.url", dbOptions.getRdbClientUrlActual());
            commonProperties.setProperty("chassis.storage.rdb.username", dbOptions.getRdbUserNameActual());
            commonProperties.setProperty("chassis.storage.rdb.password", dbOptions.getRdbPassword());
            commonProperties.setProperty("chassis.storage.hibernate.dialect", dbOptions.getRdbDialectActual());
        }


    }


    /**
     * Generating properties file for Node
     * @param node specified node
     * @param folder where to write file
     * @throws java.io.IOException /
     */
    private void generatePropertiesFile(Node node, File folder) throws IOException {

        File filePath = new File(folder+"/"+node.getServerAddressActual()+".properties");
  //      if(filePath.exists()){ filePath.delete();}

        MyProperties properties = new MyProperties();

        //adding this properties to EACH Node
        if(additionalProperties.isDeclared()){
            addAdditionalProperties(properties);
        }

        properties.setProperty("jagger.default.environment.properties", commonProperties.getProperty("jagger.default.environment.properties"));

        properties.setProperty("chassis.workspace","./jaggerworkspace");

//        if(node.getPropertiesPathActual() != null && !node.getPropertiesPathActual().matches("\\s*")) {
//            //validation of properties path
//            properties.load(new FileInputStream(node.getPropertiesPathActual()));
//        }

        if(node.getMaster() != null || properties.containsRole(RoleTypeName.MASTER.toString())) {
            addMasterProperties(properties);
        }

        if(node.getCoordinationServer() != null || properties.containsRole(RoleTypeName.COORDINATION_SERVER.toString())) {
            addCoordinationServerProperties(properties);
        }

        if(node.getReporter() != null || properties.containsRole(RoleTypeName.REPORTER)) {
            addReporterServerProperties(properties);
        }

        if(node.getKernel() != null || properties.containsRole(RoleTypeName.KERNEL.toString())) {
            addKernelProperties(properties);
        }

        properties.store(new FileOutputStream(filePath), "generated automatically");
        node.setFinalPropertiesPath(filePath.toString());
        //finalPropertiesPath - Path that Jenkins will use to run start.sh

    }


    /**
     * Adding properties from Additional properties textarea
     * @param properties property of specific node
     * @throws java.io.IOException ~
     */
    private void addAdditionalProperties(MyProperties properties) throws IOException {

        properties.load(new StringInputStream(additionalProperties.getTextFromAreaActual()));
    }


    /**
     * Adding Reporter Server Properties
     * @param properties    property of specified Node
     */
    private void addKernelProperties(MyProperties properties) {

        String key = "chassis.roles";
        if(properties.get(key) == null){
            properties.setProperty(key, RoleTypeName.KERNEL.toString());
        } else if (!properties.containsRole(RoleTypeName.KERNEL)) {
            properties.addValueWithComma(key, RoleTypeName.KERNEL.toString());
        }

        key = "chassis.coordinator.zookeeper.endpoint";
        properties.setProperty(key, commonProperties.getProperty(key));

        key = "chassis.storage.fs.default.name";
        properties.setProperty(key, commonProperties.getProperty(key));

        addDBProperties(properties);

    }


    /**
     * Adding Reporter Server Properties
     * @param properties    property of specified Node
     */
    private void addReporterServerProperties(MyProperties properties) {

        String key = "chassis.roles";
        if(properties.get(key) == null){
            properties.setProperty(key, RoleTypeName.REPORTER.toString());
        } else if (!properties.containsRole(RoleTypeName.REPORTER.toString())) {
            properties.addValueWithComma(key, RoleTypeName.REPORTER.toString());
        }

        key = "chassis.master.reporting.report.format";
        if(commonProperties.containsKey(key)) {
            properties.setProperty(key, commonProperties.getProperty(key));
        }

        key = "chassis.master.reporting.report.file.name";
        if(commonProperties.containsKey(key)) {
            properties.setProperty(key, commonProperties.getProperty(key));
        }

    }


    /**
     * Adding Coordination Server Properties
     * @param properties    property of specified Node
     */
    private void addCoordinationServerProperties(MyProperties properties) {

        String key = "chassis.roles";
        if(properties.get(key) == null){
            properties.setProperty(key, RoleTypeName.COORDINATION_SERVER.toString());
        } else if (!properties.containsRole(RoleTypeName.COORDINATION_SERVER)) {
            properties.addValueWithComma(key, RoleTypeName.COORDINATION_SERVER.toString());
        }

        key = "chassis.conditions.min.agents.count";
        if(Integer.parseInt(commonProperties.getProperty(key)) > 0){
            properties.setProperty("chassis.conditions.monitoring.enable","true");
            properties.setProperty(key, commonProperties.getProperty(key));
        }

        key = "chassis.conditions.min.kernels.count";
        properties.setProperty(key, commonProperties.getProperty(key));
    }


    /**
     * Adding Master Properties
     * @param properties    property of specified Node
     */
    private void addMasterProperties(MyProperties properties) {

        String key = "chassis.roles";
        if(properties.getProperty(key) == null){
            properties.setProperty(key, RoleTypeName.MASTER.toString());
        } else if ( !properties.containsRole(RoleTypeName.MASTER)) {
            properties.addValueWithComma(key, RoleTypeName.MASTER.toString());
        }
        //Http coordinator will always be on Master node (on port 8089?)!
        if(properties.getProperty(key) != null && !properties.containsRole("HTTP_COORDINATION_SERVER")) {
            properties.addValueWithComma(key, "HTTP_COORDINATION_SERVER");
        }

        if (!dbOptions.isUseExternalDB()) {

            if(!properties.getProperty(key).contains("RDB_SERVER")){
                properties.addValueWithComma(key,"RDB_SERVER");
            }

            String port;
            port = commonProperties.getProperty("tcpPort");

            if(port == null){
                properties.setProperty("tcpProperty","8043");
            } else {
                properties.setProperty("tcpProperty",port);
            }

        }

        key = "chassis.coordinator.zookeeper.endpoint";
        properties.setProperty(key, commonProperties.getProperty(key));

        key = "chassis.storage.fs.default.name";
        properties.setProperty(key, commonProperties.getProperty(key));

        addDBProperties(properties);

    }


    /**
     * Adding Data Base Properties
     * @param properties    property of specified Node
     */
    private void addDBProperties(MyProperties properties) {

        String key;

        key = "chassis.storage.rdb.client.driver";
        properties.setProperty(key, commonProperties.getProperty(key));

        key = "chassis.storage.rdb.client.url";
        properties.setProperty(key, commonProperties.getProperty(key));

        key = "chassis.storage.rdb.username";
        properties.setProperty(key, commonProperties.getProperty(key));

        key = "chassis.storage.rdb.password";
        properties.setProperty(key, commonProperties.getProperty(key));

        key = "chassis.storage.hibernate.dialect";
        properties.setProperty(key, commonProperties.getProperty(key));
    }


    // Start's processes on computer where jenkins run ProcStarter is not serializable
    transient private Launcher.ProcStarter procStarter = null;


    /**
     * This method will be called in build time (when you build job)
     * @param build   .
     * @param launcher .
     * @param listener .
     * @return boolean : true if build passed, false in other way
     * @throws InterruptedException
     * @throws IOException
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)  throws InterruptedException, IOException {

        PrintStream logger = listener.getLogger();
        logger.println("\n______Jagger_Easy_Deploy_Started______\n");
        String pathToDeploymentScript = build.getWorkspace() + "/deploy-script.sh";

        try{

            setUpProcStarter(launcher,build,listener);

            createBaseDir();

            createScriptFile(pathToDeploymentScript);

            logger.println("\n-----------------Deploying--------------------\n\n");

            int exitCode = procStarter.cmds(stringToCmds("./deploy-script.sh")).start().join();

            logger.println("exit code : " + exitCode);

            listener.getLogger().flush();

            logger.println("\n\n----------------------------------------------\n\n");

            return exitCode == 0;

        } catch (IOException e) {

            logger.println("!!!\nException in perform " + e +
                    "can't create script file or run script");

            if(new File(pathToDeploymentScript).delete()) {
                logger.println(pathToDeploymentScript + " has been deleted");
            } else {
                logger.println(pathToDeploymentScript + " haven't been created");
            }
        }

        return false;

    }

    private void createBaseDir() throws IOException {

        procStarter.cmds(stringToCmds("rm -rf "+baseDir)).start();

        procStarter.cmds(stringToCmds("mkdir "+baseDir)).start();


    }


    /**
     * creating script file to execute later
     * @throws IOException  if can't create file  or ru cmds.
     * @param file 5
     */
    private void createScriptFile(String file) throws IOException {

      //  new File(System.getProperty("user.home") + "/deploy-script.sh").createNewFile();
        PrintWriter fw = null;
        try{
            fw = new PrintWriter(new FileOutputStream(file));
            fw.write(deploymentScript.toString());  //<<-- deploymentScript.toString()

        } finally {
            if(fw != null){
                fw.close();
            }
        }

        procStarter.cmds(stringToCmds("chmod +x " + file)).start();
    }


    /**
     * Copy files via scp using public key autorisation
     * @param userName user name
     * @param address   address of machine
     * @param keyPath   path of private key
     * @param filePathFrom  file path that we want to copy
     * @param filePathTo  path where we want to store file
     * @param script String Builder for deployment script
     */
    private void scpGetKey(String userName, String address, String keyPath, String filePathFrom, String filePathTo, StringBuilder script) {

        script.append("scp -i ");
        script.append(keyPath);
        script.append(" ");
        script.append(userName);
        script.append("@");
        script.append(address);
        script.append(":");
        script.append(filePathFrom);
        script.append(" ");
        script.append(filePathTo).append("\n");

    }


    /**
     * Copy files via scp using public key autorisation
     * @param userName user name
     * @param address   address of machine
     * @param keyPath   path of private key
     * @param filePathFrom  file path that we want to copy
     * @param filePathTo  path where we want to store file
     * @param script String Builder for deployment script
     */
    private void scpSendKey(String userName, String address, String keyPath, String filePathFrom, String filePathTo, StringBuilder script) {

        script.append("scp -i ");
        script.append(keyPath);
        script.append(" ");
        script.append(filePathFrom);
        script.append(" ");
        script.append(userName);
        script.append("@");
        script.append(address);
        script.append(":");
        script.append(filePathTo).append("\n");

    }


    private void setUpProcStarter(Launcher launcher, AbstractBuild<?, ?> build, BuildListener listener) {

        procStarter = launcher.new ProcStarter();
        procStarter.envs();
        procStarter.pwd(build.getWorkspace());
        procStarter.stdout(listener);
    }


    /**
     * do commands on remote machine via ssh using public key authorisation
     * @param userName user name
     * @param address address of machine
     * @param keyPath path to private key
     * @param commandString command
     * @param script String Builder where we merge all commands
     */
    private void doOnVmSSH(String userName, String address, String keyPath, String commandString,StringBuilder script) {

        script.append("ssh -i ").append(keyPath).append(" ").append(userName).append("@").append(address).append(" \"").append(commandString).append("\"");
    }


//    /**
//     * not yet implemented
//     * do commands on remote machine via ssh using password key authorisation
//     * @param userName /                 look doOnVmSSh(...)
//     * @param address   /
//     * @param password   password of user
//     * @param commandString /
//     * @throws java.io.IOException /
//     * @throws InterruptedException /
//     */
//    private void doOnVmSSHPass(String userName, String address, String password, String commandString) throws IOException, InterruptedException {
//       //not yet implemented
//       // procStarter.cmds(stringToCmds("ssh " + userName + "@" + address + " " + commandString)).start().join();
//    }


    /**
     * do commands daemon on remote machine via ssh using public key authorisation
     *
     * @param userName user name
     * @param address address of machine
     * @param keyPath path to private key
     * @param commandString command
     * @param script String Builder where we merge all commands
     */
    private void doOnVmSSHDaemon(String userName, String address, String keyPath, String commandString,StringBuilder script) {

        script.append("ssh -f -i ").append(keyPath).append(" ").append(userName).append("@").append(address).append(" \"").append(commandString).append("\"");
    }


    /**
     * String to array
     * cd directory >> [cd, directory]
     * @param str commands in ine string
     * @return array of commands
     */
    private String[] stringToCmds(String str){
        return QuotedStringTokenizer.tokenize(str);
    }


    /**
     *  log information about all Nodes
     * @param logger listener.getLogger from perform method
     */
    private void logInfoAboutNodes(PrintStream logger) {

        if(sutsList != null) {
            for (SshNode node : sutsList) {
                logger.println("-------------------------");
                logger.println(node.getServerAddress() + "\t" + node.getServerAddressActual());
                logger.println(node.getSshKeyPath() + "\t" + node.getSshKeyPathActual());
                logger.println(node.getUserName() + "\t" + node.getUserNameActual());
            }
                    logger.println("-------------------------\n\n");
        }
        for(Node node : nodList){
            logger.println("-------------------------");
            logger.println(node.getServerAddress() + "\t" + node.getServerAddressActual());
            logger.println(node.getSshKeyPath() + "\t" + node.getSshKeyPathActual());
            logger.println(node.getUserName() + "\t" + node.getUserNameActual());
        }


        logger.println("\n\n------------------------------------");
        logger.println(dbOptions.getRdbClientUrl() + "\t" + dbOptions.getRdbClientUrlActual());
        logger.println(dbOptions.getRdbDialect() + "\t" + dbOptions.getRdbDialectActual());
        logger.println(dbOptions.getRdbDriver() + "\t" + dbOptions.getRdbDriverActual());
        logger.println(dbOptions.getRdbUserName() + "\t" + dbOptions.getRdbUserNameActual());

        logger.println("\n\n------------------------------------");
        logger.println(additionalProperties.getTextFromArea() + "\n\n" + additionalProperties.getTextFromAreaActual());

        logger.println("\n\n------------------------------------");
        logger.println(jaggerTestSuitePath + "\t" + jaggerTestSuitePathActual);
    }


    /**
     * Unnecessary, but recommended for more type safety
     * @return Descriptor of this class
     */
    @Override
    public Descriptor<Builder> getDescriptor() {
        return (DescriptorJEDP)super.getDescriptor();
    }

    @Extension
    public static final class DescriptorJEDP  extends BuildStepDescriptor<Builder>
    {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            //how it names in build step config
            return "Jagger Easy Deploy";
        }

        public FormValidation doCheckJaggerTestSuitePath(@QueryParameter final String value) {

            if(value.matches("\\s*")){
                return FormValidation.warning("set path, please");
            }
            String temp = value;
            if(value.startsWith("~")){
                temp = temp.substring(1,temp.length());
                temp = System.getProperty("user.home") + temp;
            }
            if(!new File(temp).exists()){
                return FormValidation.error("file not exists");
            }

            return FormValidation.ok();
        }

    }

}
