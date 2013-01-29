package com.griddynamics.jagger.jenkins.plugin;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.*;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.*;
import java.util.*;


public class JaggerEasyDeployPlugin extends Builder
{

    //to collect nodes in one field.
    private ArrayList<Node> nodList = new ArrayList<Node>();

    //the collect nodes to attack in one field.
    private ArrayList<SuT> sutsList = new ArrayList<SuT>();

    private final DBOptions dbOptions;

    // where we will store properties for Jagger for each node
    private final String PROPERTIES_PATH = "/properties";

    private MyProperties commonProperties ;

    private StringBuilder deploymentScript;

    //path to Jagger Test Suit .zip
    private final String jaggerTestSuitePath;

    private String baseDir = "pwd";


    /**
     * Constructor where fields from *.jelly will be passed
     * @param sutsList
     *                      List of nodes to test
     * @param nodList
     *               List of nodes to do work
     * @param jaggerTestSuitePath test suite path
     * @param dbOptions  properties for Data Base
     * @throws java.io.IOException s
     */
    @DataBoundConstructor
    public JaggerEasyDeployPlugin(ArrayList<SuT> sutsList, ArrayList<Node> nodList, String jaggerTestSuitePath, DBOptions dbOptions
    ) throws IOException {

        this.dbOptions = dbOptions;
        this.sutsList = sutsList;
        this.nodList = nodList;
        this.jaggerTestSuitePath = jaggerTestSuitePath;

        setUpCommonProperties();
    }

    public DBOptions getDbOptions() {
        return dbOptions;
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

        commonProperties = new MyProperties();

        PrintStream logger = listener.getLogger();
        //create folder to collect properties files
        File folder = new File(build.getWorkspace() + PROPERTIES_PATH);

        try {

            checkAddressesOnBuildVars(build.getEnvVars());//build.getEnvVars() this works, but deprecated
            //delete previous build properties
            FileUtils.deleteDirectory(folder);
            //make dirs if they not exists
            FileUtils.forceMkdir(folder);

            for(Node node: nodList){

                generatePropertiesFile(node, folder);
            }

            generateScriptToDeploy();

            logger.println("\n-------------Deployment-Script-------------------\n");
            logger.println(getDeploymentScript().toString());
            logger.println("\n-------------------------------------------------\n\n");

        } catch (IOException e) {

            logger.println("!!!\nWhile generating properties files\nException in preBuild: " + e );
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


    /**
     * generating script like in smoke-test, to execute it in perform method
     */
    private void generateScriptToDeploy() {

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

        script.append("zip -9 ").append(baseDir).append("/report.zip ").append(baseDir).append("/report.pdf ").append(baseDir).append("/result.xml\n");

        deploymentScript = script;

    }

    private void copyAllLogs(StringBuilder script) {

        script.append("\n");

        copyMastersLogs(script);

        copyKernelsLogs(script);

        copyAgentsLogs(script);
    }

    private void copyAgentsLogs(StringBuilder script) {


        for(SuT node : sutsList) {
            if(node != null && node.isInstallAgent()) {
                script.append("\necho \"Copy agents logs\"\n");
                copyLogs(node.getUserName(), node.getServerAddressActual(), node.getSshKeyPath(), script);
            }
        }
    }

    private void copyKernelsLogs(StringBuilder script) {

        for (Node node: nodList) {
            if(node != null && node.getKernel() != null && node.getMaster() == null) {
                script.append("\necho \"Copy kernels logs\"\n");
                copyLogs(node.getUserName(), node.getServerAddressActual(), node.getSshKeyPath(), script);
            }
        }
    }

    private void copyMastersLogs(StringBuilder script) {

        for (Node node: nodList) {
            if(node != null && node.getMaster() != null) {
                script.append("\necho \"Copy master logs\"\n");
                copyLogs(node.getUserName(), node.getServerAddressActual(), node.getSshKeyPath(), script);
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

            if(node.getReporter() != null){

                String userName = node.getUserName();
                String address = node.getServerAddressActual();
                String keyPath = node.getSshKeyPath();
                String jaggerHome = "/home/" + userName + "/runned_jagger";


                script.append("\n\necho \"Copy reports\"\n");
                scpGetKey(userName,
                        address,
                        keyPath,
                        jaggerHome + "/report.pdf",
                        baseDir,
                        script);

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
     */
    private void startNodes(StringBuilder script) {

        //here we should run start.sh with properties file that on our computer, not on Nodes.
        //it means that first - we should transfer it to Node
        script.append("\necho \"Copying properties to remote Nodes and start\"\n\n");

        Node coordinator = null; //COORDINATION_SERVER should start in very last order

        for(Node node : nodList) {
            String userName = node.getUserName();
            String address = node.getServerAddressActual();
            String keyPath = node.getSshKeyPath();
            String jaggerHome = "/home/" + userName + "/runned_jagger";
            String newPropPath = jaggerHome + "/tempPropertiesToDeploy.property";

            File property = new File(newPropPath);
            if(!property.exists()){
                //noinspection ResultOfMethodCallIgnored
                property.mkdirs();
            }
            scpSendKey(userName, address, keyPath, node.getFinalPropertiesPath(), newPropPath,script);
            node.setFinalPropertiesPath(newPropPath);

            if(node.getCoordinationServer() != null) {
                coordinator = node;
            } else {
                script.append("echo \"").append(address).append(" : cd ").append(jaggerHome).append("; ./start.sh property_file\"\n");
                //                                           this is only for testing on my machine
                doOnVmSSHDaemon(userName, address, keyPath, "source /etc/profile; cd " + jaggerHome + "; ./start.sh " + node.getFinalPropertiesPath(), script);   //----------------------------
                script.append(" > /dev/null\n\n");
            }
        }

        if (coordinator != null) {
            String userName = coordinator.getUserName();
            String address = coordinator.getServerAddressActual();
            String keyPath = coordinator.getSshKeyPath();
            String jaggerHome = "/home/" + userName + "/runned_jagger";

            script.append("echo \"").append(address).append(" : cd ").append(jaggerHome).append("; ./start.sh ").append("properties_file\"\n");
            //                                     this is only for testing on my machine
            doOnVmSSH(userName, address, keyPath, "source /etc/profile; cd " + jaggerHome + "; ./start.sh " + coordinator.getFinalPropertiesPath(), script);  //==================================
            script.append(" > /dev/null\n\n");
        } else {
            throw new IllegalArgumentException("no coordinator");
        }
    }


    /**
     * Starting Agents, if it declared
     * @param script deploymentScript
     */
    private void startAgents(StringBuilder script) {

        for(SuT node : sutsList){
            if(node.isInstallAgent()) {
                String jaggerHome = "/home/" + node.getUserName() + "/runned_jagger";

                killOldJagger1(node.getUserName(), node.getServerAddressActual(), node.getSshKeyPath(), jaggerHome, script);

                script.append("\necho \"Starting Agent\"\n");
                script.append("echo \"").append(node.getServerAddressActual()).append(" : cd ").append(jaggerHome).append("; ./start_agent.sh\"\n");

                String command = "cd " + jaggerHome + "; ./start_agent.sh -Dchassis.coordination.http.url=" + commonProperties.get("chassis.coordination.http.url");
                doOnVmSSHDaemon(node.getUserName(), node.getServerAddress(), node.getSshKeyPath(), command, script);
                script.append("> /dev/null\n");

            }
        }
    }


    /**
     * kill old Jagger , deploy new one , stop processes in jagger
     * @param script String Builder of deployment Script
     */
    private void killOldJagger(StringBuilder script) {

        script.append("\necho \"Killing old jagger\"\n\n");
        for(Node node:nodList){

            String jaggerHome = "/home/" + node.getUserName() + "/runned_jagger";

            killOldJagger1(node.getUserName(),node.getServerAddressActual(), node.getSshKeyPath(), jaggerHome,  script);
        }

    }


    private void killOldJagger1(String userName, String serverAddress, String keyPath, String jaggerHome, StringBuilder script){

        script.append("echo \"TRYING TO DEPLOY NODE").append(userName).append("@").append(serverAddress).append("\"\n");
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

        script.append("\n\necho \"Killing previous processes ").append(userName).append("@").append(serverAddress).append("\"\n");
        doOnVmSSH(userName, serverAddress, keyPath, jaggerHome + "/stop.sh", script);
        script.append("\n");
        doOnVmSSH(userName, serverAddress, keyPath, jaggerHome + "/stop_agent.sh", script);
        script.append("\n");
        doOnVmSSH(userName, serverAddress, keyPath, "rm -rf /home/" + userName + "/jaggerdb", script);
        script.append("\n\n");
    }


    /**
     * Check if Build Variables contain addresses , or VERSION (of Jagger)
     * @param ev Build Variables
     */
    private void checkAddressesOnBuildVars(Map<String,String> ev) {

        String address;

        for(Node node: nodList){

                address = node.getServerAddress();
                if(address.matches("\\$.+")) {

                    address = address.substring(1,address.length());

                    if(address.matches("\\{.+\\}")){
                        address = address.substring(1,address.length()-1);
                    }

                    if(ev.containsKey(address)){
                        node.setServerAddressActual(ev.get(address));
                    }
                }
        }

        for(SuT node : sutsList){

            address = node.getServerAddress();
            if(address.matches("\\$.+")) {

                address = address.substring(1,address.length());

                if(address.matches("\\{.+\\}")){
                    address = address.substring(1,address.length()-1);
                }

                if(ev.containsKey(address)){
                    node.setServerAddressActual(ev.get(address));
                }
            }
        }
    }


    /**
     * rewriting fields on special class foe properties
     * @throws java.io.IOException  f
     */
    private void setUpCommonProperties() throws IOException {

        commonProperties = new MyProperties();

        int minAgents = 0;
        int minKernels = 0;

        for(Node node : nodList) {

            if(node.getKernel() != null) {
                minKernels ++;
            }
            if(node.getCoordinationServer() !=  null) {
                setUpCoordinationServerPropeties(node);
            }
            if(node.getMaster() != null) {
                setUpMasterProperties(node);
            }
        }

        setUpRdbProperties();

        for(SuT node : sutsList) {
            if(node.isInstallAgent()) {
                minAgents ++;
            }
            setUpNodeToAttack(node);
        }

        commonProperties.setProperty("chassis.conditions.min.agents.count", String.valueOf(minAgents));
        commonProperties.setProperty("chassis.conditions.min.kernels.count",String.valueOf(minKernels));

    }

    private void setUpMasterProperties(Node node) throws IOException {

        try {
            if(dbOptions.isDoUseH2()) {
                if(!node.getPropertiesPath().matches("\\s*")) {

                    String key = "tcpPort" ;
                    Properties prope = new Properties();
                    prope.load(new FileInputStream(node.getPropertiesPath()));
                    String temp = prope.getProperty(key);
                    if(temp == null) {
                        commonProperties.setProperty(key, "8043");
                    } else {
                        commonProperties.setProperty(key, temp);
                    }
                }
            }
        } catch (IOException e) {
            throw new IOException("Exception in reading " + node.getPropertiesPath() + "\t");
        }
    }

    /**
     * Setting up Common Properties for Nodes
     * @param node node to attack
     */
    private void setUpNodeToAttack(SuT node) {

        String key = "test.service.endpoints";
        if(commonProperties.get(key) == null){
            commonProperties.setProperty(key, node.getServerAddressActual());
        } else {
            commonProperties.addValueWithComma(key, node.getServerAddressActual());
        }
    }

    /**
     * Setting up Common Properties for Nodes
     * @param node Node that play CoordinationServer Role
     */
    private void setUpCoordinationServerPropeties(Node node) {

        commonProperties.setProperty("chassis.coordinator.zookeeper.endpoint",node.getServerAddressActual() +
                                            ":" + node.getCoordinationServer().getPort());
        //Is this property belong to Coordination Server
        commonProperties.setProperty("chassis.storage.fs.default.name","hdfs://"+node.getServerAddressActual() + "/");
        commonProperties.setProperty("chassis.coordination.http.url","http://" + node.getServerAddressActual() + ":8089");
        //port of http.url hard code? or it can be set somewhere
    }

    /**
     * Setting up Common Properties for Nodes
     */
    private void setUpRdbProperties() {

        if(dbOptions.isDoUseH2()){

            String address = "NO_MASTER_DETECTED";

            String port = commonProperties.getProperty("tcpPort");
            if(port == null) {
                port = "8043";
                commonProperties.setProperty("tcpPort",port);
            }

            for(Node node: nodList) {
                if(node.getMaster() != null) {
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

            commonProperties.setProperty("chassis.storage.rdb.client.driver", dbOptions.getRdbDriver());
            commonProperties.setProperty("chassis.storage.rdb.client.url", dbOptions.getRdbClientUrl());
            commonProperties.setProperty("chassis.storage.rdb.username", dbOptions.getRdbUserName());
            commonProperties.setProperty("chassis.storage.rdb.password", dbOptions.getRdbPassword());
            commonProperties.setProperty("chassis.storage.hibernate.dialect", dbOptions.getRdbDialect());
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

        MyProperties properties = new MyProperties();

        if(node.getPropertiesPath() != null && !node.getPropertiesPath().matches("\\s*")) {
            //validation of properties path
            properties.load(new FileInputStream(node.getPropertiesPath()));
        }

        if(node.getMaster() != null){
            addMasterProperties(node, properties);
        }
        if(node.getCoordinationServer() != null){
            addCoordinationServerProperties(node, properties);
        }
        if(node.getReporter() != null){
            addReporterServerProperties(node,properties);
        }
        if(node.getKernel() != null){
            addKernelProperties(node, properties);
        }

        properties.store(new FileOutputStream(filePath), "generated automatically");
        node.setFinalPropertiesPath(filePath.toString());
        //finalPropertiesPath - Path that Jenkins will use to run start.sh

    }


    /**
     * Adding Reporter Server Properties
     * @param node  Node instance
     * @param properties    property of specified Node
     */
    private void addKernelProperties(Node node, MyProperties properties) {

        String key = "chassis.roles";
        if(properties.get(key) == null){
            properties.setProperty(key, node.getKernel().getRoleType().toString());
        } else if (!properties.getProperty(key).contains(node.getKernel().getRoleType().toString())) {
            properties.addValueWithComma(key, node.getKernel().getRoleType().toString());
        }

        key = "chassis.coordinator.zookeeper.endpoint";
        properties.setProperty(key, commonProperties.getProperty(key));

        key = "chassis.storage.fs.default.name";
        properties.setProperty(key, commonProperties.getProperty(key));

        addDBProperties(properties);

    }


    /**
     * Adding Reporter Server Properties
     * @param node  Node instance
     * @param properties    property of specified Node
     */
    private void addReporterServerProperties(Node node, MyProperties properties) {

        String key = "chassis.roles";
        if(properties.get(key) == null){
            properties.setProperty(key, node.getReporter().getRoleType().toString());
        } else if (!properties.getProperty(key).contains(node.getReporter().getRoleType().toString())) {
            properties.addValueWithComma(key,node.getReporter().getRoleType().toString());
        }
    }


    /**
     * Adding Coordination Server Properties
     * @param node  Node instance
     * @param properties    property of specified Node
     */
    private void addCoordinationServerProperties(Node node, MyProperties properties) {

        String key = "chassis.roles";
        if(properties.get(key) == null){
            properties.setProperty(key, node.getCoordinationServer().getRoleType().toString());
        } else if (!properties.getProperty(key).contains(node.getCoordinationServer().getRoleType().toString())) {
            properties.addValueWithComma(key,node.getCoordinationServer().getRoleType().toString());
        }

        key = "chassis.conditions.min.kernels.count";
        properties.setProperty(key,commonProperties.getProperty(key));
        key = "chassis.conditions.min.agents.count";
        properties.setProperty(key,commonProperties.getProperty(key));
    }


    /**
     * Adding Master Properties
     * @param node  Node instance
     * @param properties    property of specified Node
     */
    private void addMasterProperties(Node node, MyProperties properties) {

        String key = "chassis.roles";
        if(properties.getProperty(key) == null){
            properties.setProperty(key, node.getMaster().getRoleType().toString());
        } else if ( !properties.getProperty(key).contains(node.getMaster().getRoleType().toString())) {
            properties.addValueWithComma(key,node.getMaster().getRoleType().toString());
        }
        //Http coordinator will always be on Master node (on port 8089?)!
        if(properties.getProperty(key) != null && !properties.getProperty(key).contains("HTTP_COORDINATION_SERVER")) {
            properties.addValueWithComma(key, "HTTP_COORDINATION_SERVER");
        }

        if (dbOptions.isDoUseH2()) {
            if(properties.getProperty(key) != null && !properties.getProperty(key).contains("RDB_SERVER")){
                properties.addValueWithComma(key,"RDB_SERVER");
            }
        }

        key = "chassis.coordinator.zookeeper.endpoint";
        properties.setProperty(key, commonProperties.getProperty(key));

        key = "chassis.storage.fs.default.name";
        properties.setProperty(key, commonProperties.getProperty(key));

        addDBProperties(properties);

        key = "test.service.endpoints";
        properties.setProperty(key, commonProperties.getProperty(key));

        key = "tcpPort";
        properties.setProperty(key,commonProperties.getProperty(key));
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


    // Start's processes on computer where jenkins runs. ProcStarter is not serializable
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

         //   commented to check how properties and script file has been created.
         //   logger.println("exit code : " + procStarter.cmds(stringToCmds("./deploy-script.sh")).start().join());

        //    show terminal output while executing script
        //    listener.getLogger().flush();

            logger.println("\n\n----------------------------------------------\n\n");

            return true;

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

        PrintWriter fw = null;
        try{
            fw = new PrintWriter(new FileOutputStream(file));
            fw.write(getDeploymentScript().toString());

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
<<<<<<< HEAD
     * @param script StringBuilder that collecting script commands
=======
     * @param script  s
>>>>>>> 8a0633949d00000889a205698e7044bd9afbff92
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
     * @param script StringBuilder for deployment script
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


    /**
     * not yet implemented
     * do commands on remote machine via ssh using password key authorisation
     * @param userName /                 look doOnVmSSh(...)
     * @param address   /
     * @param password   password of user
     * @param commandString /
     * @throws java.io.IOException /
     * @throws InterruptedException /
     */
//    private void doOnVmSSHPass(String userName, String address, String password, String commandString) throws IOException, InterruptedException {
//       //not yet implemented
//       // procStarter.cmds(stringToCmds("ssh " + userName + "@" + address + " " + commandString)).start().join();
//    }


    /**
     * do commands daemon on remote machine via ssh using public key authorisation
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
     *  helpful while debugging
     * @param logger listener.getLogger from perform method
     */
    private void logInfoAboutNodes(PrintStream logger) {

        for (SuT node : sutsList) {
            logger.println("-------------------------");
            logger.println(node.toString());
        }
                logger.println("-------------------------\n\n");
        for(Node node:nodList){

            logger.println("-------------------------");
            logger.println("Node address : "+node.getServerAddressActual());
            logger.println("-------------------------");
            logger.println("Node properties path : "+node.getPropertiesPath());
            logger.println("-------------------------");
            logger.println("Node's roles : ");

            if(!node.getHmRoles().isEmpty()){
                for(Role role: node.getHmRoles().values()){
                    logger.println(role.toString());
                }
            } else {
                logger.println(node.getPropertiesPath());
            }
            logger.println("-------------------------\n-------------------------");
        }
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
