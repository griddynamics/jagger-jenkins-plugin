package com.griddynamics.jagger.jenkins.plugin;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.*;
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

    private final AdditionalProperties additionalProperties;

    private JaggerProperties commonProperties ;

    //environment properties file for test suit
    private final String envProperties;

    private String envPropertiesActual;

    private StringBuilder deploymentScript;

    //path to Jagger Test Suit .zip
    private final String jaggerTestSuitePath;

    private String jaggerTestSuitePathActual;

    private final String baseDir = "result";


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
        this.envPropertiesActual = envProperties;
    }

    public String getEnvPropertiesActual() {
        return envPropertiesActual;
    }

    public void setEnvPropertiesActual(String envPropertiesActual) {
        this.envPropertiesActual = envPropertiesActual;
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
            checkUsesOfEnvironmentProperties(build, listener);
        } catch (IOException e) {
            logger.println("EXCEPTION WHILE CHECKING USES OF ENV VARAIBLES");
            logger.println(e);
            return false;
        } catch (InterruptedException e) {
            logger.println("EXCEPTION WHILE CHECKING USES OF ENV VARAIBLES");
            logger.println(e);
            return false;
        }

        setUpCommonProperties();

        generateDeploymentScript();

        logger.println("\n-------------Deployment-Script-------------------\n");
        logger.println(getDeploymentScript().toString());
        logger.println("\n-------------------------------------------------\n\n");

        return true;
    }


    /**
     * New One Node Config
     */
    private void generateDeploymentScript() {

        deploymentScript = new StringBuilder();
        deploymentScript.append("#!/bin/bash\n\n");
        deploymentScript.append("TimeStart=`date +%y/%m/%d_%H:%M`\n\n");

        deploymentScript.append("rm -rf ").append(baseDir).append("\n");
        deploymentScript.append("mkdir ").append(baseDir).append("\n\n");

        killOldJagger(deploymentScript);

        deploymentScript.append("sleep 5\n\n");

        startAgents(deploymentScript);

        startNodes(deploymentScript);

        deploymentScript.append("sleep 5\n\n");

        copyReports(deploymentScript);

        copyAllLogs(deploymentScript);

        deploymentScript.append("\n\n#mutt -s \"Jenkins[JGR-stable-testplan][$TimeStart]\" jagger@griddynamics.com\n");

        deploymentScript.append("cd ").append(baseDir).append("\n");

        deploymentScript.append("zip -9 ").append("report.zip *.pdf *.html *.xml\n");


    }


    /**
     * provide ability to use environment properties
     * @param listener /
     * @param build    /
     * @throws java.io.IOException /
     * @throws InterruptedException /
     */
    private void checkUsesOfEnvironmentProperties(Build build, BuildListener listener) throws IOException, InterruptedException {

        checkNodesOnBuildVars(build, listener);
        checkAdditionalPropertiesOnBuildVars(build, listener);
        checkJaggerTestSuitOnBuildVars(build, listener);
        checkDBOptionsOnBuildVars(build, listener);
        checkAgentsOnBuildVars(build, listener);
        checkEnvPropertiesOnBuildVars(build, listener);


    }

    private void checkEnvPropertiesOnBuildVars(Build build, BuildListener listener) throws IOException, InterruptedException {

        String temp = getEnvProperties();

        setEnvPropertiesActual(build.getEnvironment(listener).expand(temp));
    }

    private void checkAgentsOnBuildVars(Build build, BuildListener listener) throws IOException, InterruptedException {

        if(sutsList != null) {
            for(SuT node : sutsList) {
                checkSshNodesServerAddresses(build, listener, node);
                checkSshNodesSSHKeyPath(build, listener, node);
                checkSshNodesUserName(build, listener, node);
            }
        }
    }


    private void checkDBOptionsOnBuildVars(Build build, BuildListener listener) throws IOException, InterruptedException {

        String temp = dbOptions.getRdbDialect();
        dbOptions.setRdbDialectActual(build.getEnvironment(listener).expand(temp));

        temp = dbOptions.getRdbUserName();
        dbOptions.setRdbUserNameActual(build.getEnvironment(listener).expand(temp));

        temp = dbOptions.getRdbClientUrl();
        dbOptions.setRdbClientUrlActual(build.getEnvironment(listener).expand(temp));

        temp = dbOptions.getRdbDriver();
        dbOptions.setRdbDriverActual(build.getEnvironment(listener).expand(temp));

    }

    private void checkJaggerTestSuitOnBuildVars(Build build, BuildListener listener) throws IOException, InterruptedException {

        String temp = getJaggerTestSuitePath();
        setJaggerTestSuitePathActual(build.getEnvironment(listener).expand(temp));
    }


    private void checkAdditionalPropertiesOnBuildVars(Build build, BuildListener listener) throws IOException, InterruptedException {

    String temp = getAdditionalProperties().getTextFromArea();
        additionalProperties.setTextFromAreaActual(build.getEnvironment(listener).expand(temp));
    }


    /**
     * Check if Build Variables contain addresses , or VERSION (of Jagger)
     * @param build  /
     * @param listener /
     * @throws java.io.IOException /
     * @throws InterruptedException /
     */
    private void checkNodesOnBuildVars(Build build, BuildListener listener) throws IOException, InterruptedException {

        for(Node node: nodList){

            checkSshNodesServerAddresses(build, listener, node);
            checkSshNodesUserName(build, listener, node);
            checkSshNodesSSHKeyPath(build, listener, node);

        }

    }

    private void checkSshNodesSSHKeyPath(Build build, BuildListener listener, SshNode node) throws IOException, InterruptedException {

        String temp = node.getSshKeyPath();
        node.setSshKeyPathActual(build.getEnvironment(listener).expand(temp));
    }

    private void checkSshNodesUserName(Build build, BuildListener listener, SshNode node) throws IOException, InterruptedException {

        String temp = node.getUserName();
        node.setUserNameActual(build.getEnvironment(listener).expand(temp));
    }


    private void checkSshNodesServerAddresses(Build build, BuildListener listener, SshNode node) throws IOException, InterruptedException {

        String temp = node.getServerAddress();
        node.setServerAddressActual(build.getEnvironment(listener).expand(temp));
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

        if(getNodList().size() != 1) {
            for (Node node: nodList) {
                if(node.getKernel() != null && node.getMaster() == null) {
                    script.append("\necho \"Copy kernels logs\"\n");
                    copyLogs(node.getUserNameActual(), node.getServerAddressActual(), node.getSshKeyPathActual(), script);
                }
            }
        }
    }

    private void copyMastersLogs(StringBuilder script) {

        if(getNodList().size() == 1) {
             script.append("\necho \"Copy master logs\"\n");
                copyLogs(getNodList().get(0).getUserNameActual(), getNodList().get(0).getServerAddressActual(),
                        getNodList().get(0).getSshKeyPathActual(), script);
        } else  {
            for (Node node: nodList) {
                if(node.getMaster() != null) {
                    script.append("\necho \"Copy master logs\"\n");
                    copyLogs(node.getUserNameActual(), node.getServerAddressActual(), node.getSshKeyPathActual(), script);
                }
            }
        }
    }


    private void copyLogs(String userName, String address, String keyPath, StringBuilder script) {

        String jaggerHome = "runned_jagger";
                                                                                     //this how we take Nodes logs,and Agents logs
        doOnVmSSH(userName, address, keyPath, "cd " + jaggerHome + "; zip -9 " + address + ".logs.zip jagger*.log*", script);
        script.append("\n");

        scpGetKey(userName, address, keyPath, jaggerHome + File.separator + address + ".logs.zip", baseDir, script);
    }


    private void copyReports(StringBuilder script) {

        if(getNodList().size() == 1) {

            String userName = getNodList().get(0).getUserNameActual();
            String address = getNodList().get(0).getServerAddressActual();
            String keyPath = getNodList().get(0).getSshKeyPathActual();
            String jaggerHome = "runned_jagger" + File.separator;


            script.append("\n\necho \"Copy reports\"\n");

            scpGetKey(userName,
                    address,
                    keyPath,
                    "\"" + jaggerHome + "*.xml " + jaggerHome + "*.pdf " + jaggerHome + "*.html\"",
                    baseDir,
                    script);
        } else {
            // in this case we should peak one that have Master role (, or Reporter)
        }


    }


    /**
     * Starting Nodes New
     * @param script deploymentScript
     */
    private void startNodes(StringBuilder script) {

        script.append("\n\necho \"Copying properties to remote Nodes and start\"\n");

        if(nodList.size() == 1) {

            startNodesOneNodeConfiguration(script);

        } else {
            //never mind for now
        }
    }

    private void startNodesOneNodeConfiguration(StringBuilder script) {

        String userName = nodList.get(0).getUserNameActual();
        String address = nodList.get(0).getServerAddressActual();
        String keyPath = nodList.get(0).getSshKeyPathActual();
        String jaggerHome = "runned_jagger";

        script.append("echo \"").append(address).append(" : cd ").append(jaggerHome).append("; ./start.sh properties_file\"\n");

        StringBuilder command = new StringBuilder();
        command.append("cd ").append(jaggerHome).append("; ./start.sh ");
        command.append(getEnvPropertiesActual()).append(" \'\\\n\t-Xmx1550m\\\n\t-Xms1550m\\\n");

        if(getAdditionalProperties().isDeclared()) {
            for(String line: getAdditionalProperties().getTextFromAreaActual().split("\\n")) {
                command.append("\t-D").append(line.trim()).append("\\\n");
            }
        }

        if(getSutsList() != null) {
            command.append("\t-Dchassis.conditions.min.agents.count=").append(getSutsList().size()).append("\\\n");
        }

        if(getDbOptions().isUseExternalDB()) {
            command.append("\t-Dchassis.roles=MASTER,KERNEL,COORDINATION_SERVER,HTTP_COORDINATION_SERVER");
        } else {
            command.append("\t-Dchassis.roles=MASTER,KERNEL,COORDINATION_SERVER,HTTP_COORDINATION_SERVER,RDB_SERVER");
        }

        command.append("\'");


        doOnVmSSH(userName, address, keyPath,
                command.toString()
                , script);
        script.append(" > ").append(File.separator).append("dev").append(File.separator).append("null\n\n");
    }


    /**
     * Starting Agents, if it declared
     * @param script deploymentScript
     */
    private void startAgents(StringBuilder script) {

        if (sutsList != null) {
            for(SuT node : sutsList){

                String jaggerHome = "runned_jagger";

                killOldJagger1(node.getUserNameActual(), node.getServerAddressActual(), node.getSshKeyPathActual(), jaggerHome, script);
                script.append("sleep 5\n");

                script.append("\necho \"Starting Agent\"\n");
                script.append("echo \"").append(node.getServerAddressActual()).append(" : cd ").append(jaggerHome).append("; ./start_agent.sh\"\n");

                StringBuilder command = new StringBuilder();
                command.append("cd ").append(jaggerHome);
                command.append("; ./start_agent.sh\\\n\t-Xmx1550m\\\n\t-Xms1550m\\\n\t-Dchassis.coordination.http.url=");
                command.append(commonProperties.get("chassis.coordination.http.url"));

                doOnVmSSHDaemon(node.getUserNameActual(), node.getServerAddressActual(), node.getSshKeyPathActual(), command.toString(), script);
                script.append(" > ").append(File.separator).append("dev").append(File.separator).append("null\n\n");

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

            String jaggerHome = "runned_jagger";

            killOldJagger1(node.getUserNameActual(),node.getServerAddressActual(), node.getSshKeyPathActual(), jaggerHome,  script);
        }

    }


    private void killOldJagger1(String userName, String serverAddress, String keyPath, String jaggerHome, StringBuilder script){

        script.append("echo \"TRYING TO DEPLOY JAGGER to ").append(userName).append("@").append(serverAddress).append("\"\n");
        doOnVmSSH(userName, serverAddress, keyPath, "rm -rf " + jaggerHome, script);
        script.append("\n");
        doOnVmSSH(userName, serverAddress, keyPath, "mkdir " + jaggerHome, script);
        script.append("\n");

        scpSendKey(userName,
                serverAddress,
                keyPath,
                getJaggerTestSuitePathActual(),
                jaggerHome, script);

        //here we take name of file from path: '~/path/to/file' -> 'file'
        String jaggerFileName = getJaggerTestSuitePathActual();
        int index = getJaggerTestSuitePathActual().lastIndexOf(File.separator);
        if(index >= 0) {
            jaggerFileName = getJaggerTestSuitePathActual().substring(index + 1);
        }

        doOnVmSSH(userName, serverAddress, keyPath,
                "unzip " + jaggerHome + File.separator + jaggerFileName + " -d " + jaggerHome,
                script);
        script.append(" > ").append(File.separator).append("dev").append(File.separator).append("null\n\n");

        script.append("\n\necho \"KILLING previous processes ").append(userName).append("@").append(serverAddress).append("\"\n");
        doOnVmSSH(userName, serverAddress, keyPath, jaggerHome + File.separator + "stop.sh", script);
        script.append("\n");
        doOnVmSSH(userName, serverAddress, keyPath, jaggerHome + File.separator + "stop_agent.sh", script);
        script.append("\n");
        doOnVmSSH(userName, serverAddress, keyPath, "rm -rf jaggerdb", script);
        script.append("\n\n");
    }


    /**
     *  Common Properties that will be used
     */
    private void setUpCommonProperties()  {

        commonProperties = new JaggerProperties();

        if(getNodList().size() == 1) {

            setUpRdbProperties();
            commonProperties.setProperty("chassis.coordination.http.url",
                    "http://" + getNodList().get(0).getServerAddressActual() + ":8089");

        } else {
//               . . .
        }
    }


    /**
     * Setting up Common Properties for Nodes
     */
    private void setUpRdbProperties() {


        if(!dbOptions.isUseExternalDB() && getNodList().size() == 1) {

            String port = "8043";

            commonProperties.setProperty("chassis.storage.rdb.client.driver", "org.h2.Driver");
            commonProperties.setProperty("chassis.storage.rdb.client.url","jdbc:h2:tcp://" +
                            nodList.get(0).getServerAddressActual() + ":" + port +"/jaggerdb/db");
            commonProperties.setProperty("chassis.storage.rdb.username","jagger");
            commonProperties.setProperty("chassis.storage.rdb.password","rocks");
            commonProperties.setProperty("chassis.storage.hibernate.dialect","org.hibernate.dialect.H2Dialect");

        } else if (!getDbOptions().isUseExternalDB() && getNodList().size() > 1){
//          ...
        } else {

            commonProperties.setProperty("chassis.storage.rdb.client.driver", getDbOptions().getRdbDriverActual());
            commonProperties.setProperty("chassis.storage.rdb.client.url", getDbOptions().getRdbClientUrlActual());
            commonProperties.setProperty("chassis.storage.rdb.username", getDbOptions().getRdbUserNameActual());
            commonProperties.setProperty("chassis.storage.rdb.password", getDbOptions().getRdbPassword());
            commonProperties.setProperty("chassis.storage.hibernate.dialect", getDbOptions().getRdbDialectActual());
        }
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
        String pathToDeploymentScript = build.getWorkspace() + File.separator + "deploy-script.sh";

        try{

            setUpProcStarter(launcher,build,listener);

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

        return true;
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
            fw.write(deploymentScript.toString());

        } finally {
            if(fw != null){
                fw.close();
            }
        }

        //setting permissions for executing
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

        script.append("ssh -i ").append(keyPath).append(" ").append(userName).append("@").append(address);
        script.append(" \"").append(commandString).append("\"");
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

        script.append("ssh -f -i ").append(keyPath).append(" ").append(userName).append("@").append(address);
        script.append(" \"").append(commandString).append("\"");
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
            if(value.contains("$")) {
                return FormValidation.ok();
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
