package com.griddynamics.jagger.jenkins.plugin;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created with IntelliJ IDEA.
 * User: amikryukov
 * Date: 12/21/12
 */


/**
 * To Make Object of Server that we want to test
 */
public class NodeToAttack extends Role implements Describable<NodeToAttack> {

    private final String serverAddress;

    private final boolean installAgent;


    @DataBoundConstructor
    public NodeToAttack(String serverAddress , boolean installAgent){

        this.serverAddress = serverAddress;
        this.installAgent = installAgent;
    }

    public String getMyString(){
        return "My String!" ;
    }




    public String getServerAddress() {
        return serverAddress;
    }

    public boolean isInstallAgent() {
        return installAgent;
    }

    public Descriptor<NodeToAttack> getDescriptor() {

        return Hudson.getInstance().getDescriptor(getClass());
    }


    @Extension
    public static class DescriptorImpl extends Descriptor<NodeToAttack>{

        @Override
        public String getDisplayName() {
            return "NodeToAttack";
        }
    }

}