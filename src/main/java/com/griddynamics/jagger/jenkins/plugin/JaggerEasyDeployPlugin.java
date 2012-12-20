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

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class JaggerEasyDeployPlugin extends Builder
{

    private ArrayList<Node> nodList = new ArrayList<Node>();
    private boolean masterExists;
    private boolean rdbExists;
    private boolean reporterExists;
    private boolean coordinatorExists;
    private boolean KernelExists;

    @DataBoundConstructor
    public JaggerEasyDeployPlugin(ArrayList<Node> nodList){

        this.nodList = nodList;
    }

    public ArrayList<Node> getNodList() {
        return nodList;
    }

    public boolean isMasterExists() {
        return masterExists;
    }

    public boolean isRdbExists() {
        return rdbExists;
    }

    public boolean isReporterExists() {
        return reporterExists;
    }

    public boolean isCoordinatorExists() {
        return coordinatorExists;
    }

    public boolean isKernelExists() {
        return KernelExists;
    }

//    public Node doAddNode(){
//
//        Node node = new Node("","","",false,"",false,false,false,false,false,false);
//        nodList.add(node);
//
//        return node;
//
//    }


    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        PrintStream logger = listener.getLogger();
        logger.println("\n______Jagger_Easy_Deploy_Started______\n");

        for(Node node:nodList){
            logger.println("Node address : "+node.getServerAddress());
            logger.println("Node's Roles : ");
            for(Role role: node.getRoles()){
                logger.println(role);
            }

        }

        logger.println("\n______Jagger_Easy_Deploy_Finished______\n");

        return true;
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

//        public ArrayList<Node> getDefNodeList() {
//            ArrayList<Node> list = new ArrayList<Node>();
//            list.add(new Node("","","",false,"",false,false,false,false,false,false));
//            list.add(new Node("","","",false,"",false,false,false,false,false,false));
//            return list;
//        }



        @Override
        public String getDisplayName() {
            return "Easy Deploy";
        }

    }



}
