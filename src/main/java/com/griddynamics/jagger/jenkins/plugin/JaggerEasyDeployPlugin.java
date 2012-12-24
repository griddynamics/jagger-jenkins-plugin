package com.griddynamics.jagger.jenkins.plugin;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;


public class JaggerEasyDeployPlugin extends Builder
{

    private ArrayList<Node> nodList = new ArrayList<Node>();
//    private boolean masterExists;
//    private boolean rdbExists;
//    private boolean reporterExists;
//    private boolean coordinatorExists;
//    private boolean KernelExists;

    private ArrayList<NodeToAttack> nodesToAttack = new ArrayList<NodeToAttack>();

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

//    public boolean isMasterExists() {
//        return masterExists;
//    }
//
//    public boolean isRdbExists() {
//        return rdbExists;
//    }
//
//    public boolean isReporterExists() {
//        return reporterExists;
//    }
//
//    public boolean isCoordinatorExists() {
//        return coordinatorExists;
//    }
//
//    public boolean isKernelExists() {
//        return KernelExists;
//    }


    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        PrintStream logger = listener.getLogger();
        logger.println("\n______Jagger_Easy_Deploy_Started______\n");

        try{
            for(NodeToAttack node:nodesToAttack){
                logger.println("-------------------------");
                logger.println("NodeToAttack address : "+node.getServerAddress());
                logger.println("-------------------------");
                logger.println(node.toString());
                }
                logger.println("-------------------------\n-------------------------\n");

            for(Node node:nodList){
                logger.println("-------------------------");
                logger.println("Node address : "+node.getServerAddress());
                logger.println("-------------------------");
                logger.println("Node's rolespack : ");
                for(Role role: node.getRoles()){
                    logger.println(role.toString());
                }
                logger.println("-------------------------\n-------------------------");

            }

            logger.println("\n______Jagger_Easy_Deploy_Finished______\n");

            return true;

        }catch (Exception e){
            logger.println("Troubles : " +e.getLocalizedMessage() );
        }
            return false;
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl  extends BuildStepDescriptor<Builder>
    {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }


        @Override
        public String getDisplayName() {
            return "Easy Deploy";
        }

        /**
         * To test number of each role
         * @param nodList whole list of nodes that do work
         * @return OK if it's OK, ERROR otherwise
         */
        public FormValidation doCheckNodList(@QueryParameter ArrayList<Node> nodList){

            int numberOfMasters = 0,
                numberOfCoordServers = 0,
                numberOfKernels = 0,
                numberOfRdbServers = 0,
                numberOfReporters = 0;

           // masterExists

            try{

                for(Node node:nodList){
                    for(Role role:node.getRoles()){
                        if (role instanceof Kernel){
                            numberOfKernels ++;
                        } else if (role instanceof Master){
                            numberOfMasters ++;
                        } else if (role instanceof CoordinationServer){
                            numberOfCoordServers ++;
                        } else if (role instanceof Reporter){
                            numberOfReporters ++;
                        } else if (role instanceof RdbServer){
                            numberOfRdbServers ++;
                        } else {
                            throw new Exception("Where this come from? Not role!"); //temporary decision
                        }
                    }
                }

                if(numberOfCoordServers == 0){
                    return FormValidation.error("no COORDINATION_SERVER was found");
                } else if (numberOfCoordServers > 1){
                    return FormValidation.error("more then one COORDINATION_SERVER was found");
                }

                if(numberOfMasters == 0){
                    return FormValidation.error("no MASTER was found");
                } else if (numberOfMasters > 1) {
                    return FormValidation.error("more then one MASTER was found");
                }

                if(numberOfRdbServers == 0){
                    return FormValidation.error("no RDB_SERVER was found");
                } else if (numberOfRdbServers > 1){
                    return FormValidation.error("more then one RDB_SERVER was found");
                }

                if(numberOfReporters == 0){
                    return FormValidation.error("no REPORTER was found");
                } else if (numberOfReporters > 1){
                    return FormValidation.error("more then one REPORTER was found");
                }

                if(numberOfKernels == 0){
                    return FormValidation.error("no KERNEL was found");
                }

                return FormValidation.ok();
            } catch (Exception e) {
                return FormValidation.error("something wrong");
            }
        }




    }



}
