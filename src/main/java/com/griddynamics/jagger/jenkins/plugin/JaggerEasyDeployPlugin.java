package com.griddynamics.jagger.jenkins.plugin;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.xml.bind.PropertyException;
import java.io.*;
import java.util.*;


public class JaggerEasyDeployPlugin extends Builder
{

    //to collect nodes in one field.
    private ArrayList<Node> nodList = new ArrayList<Node>();

    //the collect nodes to attack in one field.
    private ArrayList<NodeToAttack> nodesToAttack = new ArrayList<NodeToAttack>();

    // where we will store properties for Jagger for each node
    private final String PROPERTIES_PATH = "/properties";

    private MyProperties commonProperties ;

    private StringBuilder deploymentScript;

    //path to Jagger Test Suit .zip
    private final String jaggerTestSuitePath;

    private String baseDir = "pwd";

    /**
     * Constructor where fields from *.jelly will be passed
     * @param nodesToAttack
     *                      List of nodes to attack
     * @param nodList
     *               List of nodes to do work
     */
    @DataBoundConstructor
    public JaggerEasyDeployPlugin(ArrayList<NodeToAttack> nodesToAttack, ArrayList<Node> nodList, String jaggerTestSuitePath){

        this.nodesToAttack = nodesToAttack;
        this.nodList = nodList;
        this.jaggerTestSuitePath = jaggerTestSuitePath;

        setUpCommonProperties();
    }

    public String getJaggerTestSuitePath() {
        return jaggerTestSuitePath;
    }

    public ArrayList<NodeToAttack> getNodesToAttack() {
        return nodesToAttack;
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

            checkAddressesOnBuildVars(build.getEnvVars());//build.getEnvVars() this works, but deprecated

            //create folder to collect properties files
            File folder = new File(build.getWorkspace() + PROPERTIES_PATH);
            if(!folder.exists()) {folder.mkdirs();}

            logger.println("\nFOLDER WORKSPACE\n"+folder.toString()+"\n\n");


            for(Node node: nodList){

                generatePropertiesFile(node, folder);
            }

            generateScriptToDeploy();

            logger.println(deploymentScript.toString());

        } catch (Exception e) {
            logger.println("Exception in preBuild: " + e );

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

        startAgents(script);

        startNodes(script);

        copyReports(script);

        copyAllLogs(script);

        script.append("\n\n#mutt -s \"Jenkins[JGR-stable-testplan][$TimeStart]\" jagger@griddynamics.com\n");

        script.append("zip -9 report.zip report.pdf result.xml\n");

        deploymentScript = script;

    }

    private void copyAllLogs(StringBuilder script) {

        script.append("\n");

        copyMastersLogs(script);

        copyKernelsLogs(script);

        copyAgentsLogs(script);
    }

    private void copyAgentsLogs(StringBuilder script) {


        for(NodeToAttack node : nodesToAttack) {
            if(node != null && node.isInstallAgent()) {
                script.append("\necho Copy agents logs\n");
                copyLogs(node.getUserName(), node.getServerAddressActual(), node.getSshKeyPath(), script);
            }
        }
    }

    private void copyKernelsLogs(StringBuilder script) {

        for (Node node: nodList) {
            if(node != null && node.getKernel() != null) {
                script.append("\necho Copy kernels logs\n");
                copyLogs(node.getUserName(), node.getServerAddressActual(), node.getSshKeyPath(), script);
            }
        }
    }

    private void copyMastersLogs(StringBuilder script) {

        for (Node node: nodList) {
            if(node != null && node.getMaster() != null) {
                script.append("\necho Copy kernels logs\n");
                copyLogs(node.getUserName(), node.getServerAddressActual(), node.getSshKeyPath(), script);
            }
        }
    }


    private void copyLogs(String userName, String address, String keyPath, StringBuilder script) {

                String jaggerHome = "/home/" + userName + "/runned_jagger";

                doOnVmSSH(userName, address, keyPath, "cd " + jaggerHome + "; zip -9 " + address + ".logs.zip jagger.log*", script);
                scpGetKey(userName, address, keyPath, jaggerHome + "/" + address + ".logs.zip", baseDir, script);
    }


    private void copyReports(StringBuilder script) {

        for(Node node : nodList){

            if(node.getReporter() != null){

                String userName = node.getUserName();
                String address = node.getServerAddressActual();
                String keyPath = node.getFinalPropertiesPath();
                String jaggerHome = "/home/" + userName + "/runned_jagger";


                script.append("\n\necho Copy reports\n");
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
        script.append("\nCopying properties to remote Nodes and start\n\n");

        Node coordinator = null; //COORDINATION_SERVER should start in very last order

        for(Node node : nodList) {
            String userName = node.getUserName();
            String address = node.getServerAddressActual();
            String keyPath = node.getSshKeyPath();
            String jaggerHome = "/home/" + userName + "/runned_jagger";
            String newPropPath = jaggerHome + "/tempProperiesToDeploy";

            File property = new File(newPropPath);
            if(!property.exists()){
                //noinspection ResultOfMethodCallIgnored
                property.mkdirs();
            }
            scpSendKey(userName, address, keyPath, node.getFinalPropertiesPath(), newPropPath,script);
            node.setFinalPropertiesPath(newPropPath + "/" + node.getServerAddressActual() + ".properties");

            if(node.getCoordinationServer() != null) {
                coordinator = node;
            } else {
                script.append("echo ").append(address).append(" : cd ").append(jaggerHome).append("; ./start.sh property_file\n");
                doOnVmSSHDaemon(userName, address, keyPath, "cd " + jaggerHome + "; ./start.sh " + node.getFinalPropertiesPath(), script);
                script.append("> /dev/null\n\n");
            }
        }

        if (coordinator != null) {
            String userName = coordinator.getUserName();
            String address = coordinator.getServerAddressActual();
            String keyPath = coordinator.getSshKeyPath();
            String jaggerHome = "/home/" + userName + "/runned_jagger";

            script.append("echo ").append(address).append(" : cd ").append(jaggerHome).append("; ./start.sh ").append("properties_file\n");
            doOnVmSSH(userName, address, keyPath, "cd " + jaggerHome + "; ./start.sh " + coordinator.getFinalPropertiesPath(), script);
            script.append("> /dev/null\n\n");
        } else {
            //throw new IllegalArgumentException("no coordinator");
        }
    }


    /**
     * Starting Agents, if it declared
     * @param script deploymentScript
     */
    private void startAgents(StringBuilder script) {

        for(NodeToAttack node : nodesToAttack){
            if(node.isInstallAgent()) {
                String jaggerHome = "/home/" + node.getUserName() + "/runned_jagger";

                killOldJagger1(node.getUserName(), node.getServerAddressActual(), node.getSshKeyPath(), jaggerHome, script);

                script.append("\necho Starting Agents\n");
                script.append("echo \"").append(node.getServerAddressActual()).append(" : cd ").append(jaggerHome).append("; ./start_agent.sh\"\n");

                String command = "cd " + jaggerHome + "; ./start_agent.sh -Dchassis.coordination.http.url=" + commonProperties.get("chassis.coordination.http.url");
                doOnVmSSHDaemon(node.getUserName(), node.getServerAddress(), node.getSshKeyPath(), command, script);
                script.append("> /dev/null");

            }
        }
    }


    /**
     * kill old Jagger , deploy new one , stop processes in jagger
     * @param script String Builder of deployment Script
     */
    private void killOldJagger(StringBuilder script) {

        script.append("\necho Killing old jagger\n\n");
        for(Node node:nodList){

            String jaggerHome = "/home/" + node.getUserName() + "/runned_jagger";

            killOldJagger1(node.getUserName(),node.getServerAddressActual(), node.getSshKeyPath(), jaggerHome,  script);
        }

    }


    private void killOldJagger1(String userName, String serverAddress, String keyPath, String jaggerHome, StringBuilder script){

        script.append("echo TRYING TO DEPLOY NODE ").append(userName).append("@").append(serverAddress).append("\n");
        doOnVmSSH(userName, serverAddress, keyPath, "rm -rf " + jaggerHome, script);
        doOnVmSSH(userName, serverAddress, keyPath, "mkdir " + jaggerHome, script);

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

        script.append("\necho KILLING PREVIOUS PROCESS ").append(userName).append("@").append(serverAddress).append("\n");
        doOnVmSSH(userName, serverAddress, keyPath, jaggerHome + "/stop.sh", script);
        doOnVmSSH(userName, serverAddress, keyPath, jaggerHome + "/stop_agent.sh", script);
        doOnVmSSH(userName, serverAddress, keyPath, jaggerHome + " rm -rf /home/" + userName + "/jaggerdb", script);
        script.append("\n");
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

        for(NodeToAttack node : nodesToAttack){

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
     */
    private void setUpCommonProperties() {

        commonProperties = new MyProperties();

        for(Node node : nodList) {
            if(node.getRdbServer() != null) {
                setUpRdbProperties(node);
            }
            if(node.getCoordinationServer() !=  null) {
                setUpCoordinationServerPropeties(node);
            }
        }

        for(NodeToAttack node : nodesToAttack) {
            setUpNodeToAttack(node);
        }

    }

    /**
     * Setting up Common Properties for Nodes
     * @param node node to attack
     */
    private void setUpNodeToAttack(NodeToAttack node) {

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
     * @param node Node that play RdbServer Role
     */
    private void setUpRdbProperties(Node node) {
        //if using H2 !!!
        commonProperties.setProperty("chassis.storage.rdb.client.driver", "org.h2.Driver");
        commonProperties.setProperty("chassis.storage.rdb.client.url","jdbc:h2:tcp://" +
                        node.getServerAddressActual() + ":" + node.getRdbServer().getRdbPort() +"/jaggerdb/db");
        commonProperties.setProperty("chassis.storage.rdb.username","jagger");
        commonProperties.setProperty("chassis.storage.rdb.password","rocks");
        commonProperties.setProperty("chassis.storage.hibernate.dialect","org.hibernate.dialect.H2Dialect");
        //standard port 8043 ! can it be changed? or hard code for ever?
        //if external bd ...
    }


    /**
     * Generating properties file for Node
     * @param node specified node
     * @param folder where to write file
     * @throws java.io.IOException /
     * @throws javax.xml.bind.PropertyException /
     */
    private void generatePropertiesFile(Node node, File folder) throws PropertyException, IOException {

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
        if(node.getRdbServer() != null){
            addRdbServerProperties(node, properties);
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
        } else {
            properties.addValueWithComma(key, node.getKernel().getRoleType().toString());
        }

        key = "chassis.coordinator.zookeeper.endpoint";
        properties.setProperty(key, commonProperties.getProperty(key));

        key = "chassis.storage.fs.default.name";
        properties.setProperty(key, commonProperties.getProperty(key));

        addDBProperties(properties);

        key = "test.service.endpoints";
        properties.setProperty(key, commonProperties.getProperty(key));
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
        } else {
            properties.addValueWithComma(key,node.getReporter().getRoleType().toString());
        }
    }


    /**
     * Adding RDB Server Properties
     * @param node  Node instance
     * @param properties    property of specified Node
     */
    private void addRdbServerProperties(Node node, MyProperties properties) {

        String key = "chassis.roles";
        if(properties.get(key) == null){
            properties.setProperty(key, node.getRdbServer().getRoleType().toString());
        } else {
            properties.addValueWithComma(key,node.getRdbServer().getRoleType().toString());
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
        } else {
            properties.addValueWithComma(key,node.getCoordinationServer().getRoleType().toString());
        }
    }


    /**
     * Adding Master Properties
     * @param node  Node instance
     * @param properties    property of specified Node
     * @throws javax.xml.bind.PropertyException /
     */
    private void addMasterProperties(Node node, MyProperties properties) throws PropertyException {

        String key = "chassis.roles";
        if(properties.getProperty(key) == null){
            properties.setProperty(key, node.getMaster().getRoleType().toString());
        } else {
            properties.addValueWithComma(key,node.getMaster().getRoleType().toString());
        }
        //Http coordinator will always be on Master node (on port 8089?)!
        properties.addValueWithComma(key, "HTTP_COORDINATION_SERVER");

        key = "chassis.coordinator.zookeeper.endpoint";
        properties.setProperty(key, commonProperties.getProperty(key));

        key = "chassis.storage.fs.default.name";
        properties.setProperty(key, commonProperties.getProperty(key));

        addDBProperties(properties);

        key = "test.service.endpoints";
        properties.setProperty(key, commonProperties.getProperty(key));
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
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        PrintStream logger = listener.getLogger();
        logger.println("\n______Jagger_Easy_Deploy_Started______\n");
   //     logger.println("\n______DEBUG_INFORMATION_NODES_WITH_ROLES______\n");
    //    logInfoAboutNodes(logger);
        try{

            setUpProcStarter(launcher,build);

            createScriptFile(build.getWorkspace());

            procStarter.cmds(stringToCmds("./deploy-script.sh")).start();

            return true;

        }catch (Exception e){
            logger.println("Troubles : " +e);
            return false;
        }

    }

    /**
     * creating script file to execute later
     * @throws FileNotFoundException  wow
     * @param workspace
     */
    private void createScriptFile(FilePath workspace) throws IOException, InterruptedException {

      //  new File(System.getProperty("user.home") + "/deploy-script.sh").createNewFile();
        PrintWriter fw = null;
        try{
            fw = new PrintWriter(new FileOutputStream(workspace + "/deploy-script.sh"));
            fw.write("#!/bin/bash\n");  //<<-- deploymentScript.toString()
            fw.write("mkdir YYYYEEESSSS");

        } finally {
            if(fw != null){
                fw.close();
            }
        }

        procStarter.cmds(stringToCmds("chmod +x deploy-script.sh")).start();
    }


    /**
     * Copy files via scp using public key autorisation
     * @param userName user name
     * @param address   address of machine
     * @param keyPath   path of private key
     * @param filePathFrom  file path that we want to copy
     * @param filePathTo  path where we want to store file
     */
    private void scpGetKey(String userName, String address, String keyPath, String filePathFrom, String filePathTo, StringBuilder scripr) {

        scripr.append("scp -i ");
        scripr.append(keyPath);
        scripr.append(" ");
        scripr.append(userName);
        scripr.append("@");
        scripr.append(address);
        scripr.append(":");
        scripr.append(filePathFrom);
        scripr.append(" ");
        scripr.append(filePathTo).append("\n");

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


    private void setUpProcStarter(Launcher launcher, AbstractBuild<?, ?> build) {

        procStarter = launcher.new ProcStarter();
        procStarter.envs();
        procStarter.pwd(build.getWorkspace());
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

        script.append("ssh -i ").append(keyPath).append(" ").append(userName).append("@").append(address).append(" ").append(commandString).append("\n");

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
    private void doOnVmSSHPass(String userName, String address, String password, String commandString) throws IOException, InterruptedException {
       //not yet implemented
        procStarter.cmds(stringToCmds("ssh " + userName + "@" + address + " " + commandString)).start().join();
    }


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

        script.append("ssh -f -i ").append(keyPath).append(" ").append(userName).append("@").append(address).append(" ").append(commandString).append("\n");
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

        for(NodeToAttack node:nodesToAttack){
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
