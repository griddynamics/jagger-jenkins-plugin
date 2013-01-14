package com.griddynamics.jagger.jenkins.plugin;

import hudson.Extension;
import hudson.Launcher;
import hudson.Proc;
import hudson.console.ConsoleNote;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.w3c.dom.NodeList;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class JaggerEasyDeployPlugin extends Builder
{

    //to collect nodes in one field.
    private ArrayList<Node> nodList = new ArrayList<Node>();

    //the collect nodes to attack in one field.
    private ArrayList<NodeToAttack> nodesToAttack = new ArrayList<NodeToAttack>();


    /**
     * Constructor where fields from *.jelly will be passed
     * @param nodesToAttack
     *                      List of nodes to attack
     * @param nodList
     *               List of nodes to do work
     */
    @DataBoundConstructor
    public JaggerEasyDeployPlugin(ArrayList<NodeToAttack> nodesToAttack, ArrayList<Node> nodList){

        this.nodesToAttack = nodesToAttack;
        this.nodList = nodList;
    }

    public ArrayList<NodeToAttack> getNodesToAttack() {
        return nodesToAttack;
    }

    public ArrayList<Node> getNodList() {
        return nodList;
    }

    /**
     * To load EnvVars and change
     * @param build .
     * @param listener .
     * @return true
     */
    @Override
    public boolean prebuild(Build build, BuildListener listener) {

        Map<String,String> ev = build.getEnvVars();
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

     //   listener.getLogger().println(System.getProperties().stringPropertyNames());

        return true;
    }


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
        logger.println("\n______DEBUG_INFORMATION_NODES_WITH_ROLES______\n");

        try{

            for(NodeToAttack node:nodesToAttack){
                logger.println("-------------------------");
                logger.println(node.toString());
                }
                logger.println("-------------------------\n\n");

            for(Node node:nodList){
                logger.println("-------------------------");
                logger.println("Node address : "+node.getServerAddressActual());
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

            Launcher.ProcStarter procStarter = launcher.new ProcStarter();

            procStarter.cmds("ssh -i ~/.ssh/id_rsa amikryukov@amikryukov-ws ls".split("\\s"));
            //procStarter.envs(build.getEnvVars());
            procStarter.envs();
            procStarter.pwd(build.getWorkspace());   ///home/amikryukov/temp/

            Proc proc = launcher.launch(procStarter);
            logger.println(proc.getStdout());
            int exitCode = proc.join();
            if(exitCode != 0){
                logger.println("launcher.launch code " + exitCode);
                return false;
            }

           logger.println(build.getBuildVariables());






            return true;

        }catch (Exception e){
            logger.println("Troubles : " +e);
        }
            return false;
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


////    validation for nodeList not yet implemented
//        /**
//         * To test number of each role
//         * @param nodList whole list of nodes that do work
//         * @return OK if it's OK, ERROR otherwise
//         */
//        public FormValidation doCheckNodList(@QueryParameter final ArrayList<Node> nodList){
//
//
//
//            int numberOfMasters = 0,
//                numberOfCoordServers = 0,
//                numberOfKernels = 0,
//                numberOfRdbServers = 0,
//                numberOfReporters = 0;
//
//            try{
//
//                for(Node node:(List<Node>)nodList){
//                    for(Role role:node.getHmRoles().values()){
//                        if (role instanceof Kernel){
//                            numberOfKernels ++;
//                        } else if (role instanceof Master){
//                            numberOfMasters ++;
//                        } else if (role instanceof CoordinationServer){
//                            numberOfCoordServers ++;
//                        } else if (role instanceof Reporter){
//                            numberOfReporters ++;
//                        } else if (role instanceof RdbServer){
//                            numberOfRdbServers ++;
//                        } else {
//                            throw new Exception("Where this come from? Not role!"); //temporary decision
//                        }
//                    }
//                }
//
//                if(numberOfCoordServers == 0){
//                    return FormValidation.error("no COORDINATION_SERVER was found");
//                } else if (numberOfCoordServers > 1){
//                    return FormValidation.error("more then one COORDINATION_SERVER was found");
//                }
//
//                if(numberOfMasters == 0){
//                    return FormValidation.error("no MASTER was found");
//                } else if (numberOfMasters > 1) {
//                    return FormValidation.error("more then one MASTER was found");
//                }
//
//                if(numberOfRdbServers == 0){
//                    return FormValidation.error("no RDB_SERVER was found");
//                } else if (numberOfRdbServers > 1){
//                    return FormValidation.error("more then one RDB_SERVER was found");
//                }
//
//                if(numberOfReporters == 0){
//                    return FormValidation.error("no REPORTER was found");
//                } else if (numberOfReporters > 1){
//                    return FormValidation.error("more then one REPORTER was found");
//                }
//
//                if(numberOfKernels == 0){
//                    return FormValidation.error("no KERNEL was found");
//                }
//
//                return FormValidation.ok(nodList.getClass().getName());
//            } catch (Exception e) {
//                return FormValidation.error("something wrong");
//            }
//        }
    }

}
