package com.griddynamics.jagger.jenkins.plugin;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: amikryukov
 * Date: 12/14/12
 */
public class Node implements Describable<Node> {

    private final String serverAddress;

    private final String userName;

    private final String userPassword;

    private final String sshKeyPath ;

    private final String propertiesPath;

    private final boolean usePassword;

    private final boolean setPropertiesByHand;

    private ArrayList<Role> roles = new ArrayList<Role>() ;

    @DataBoundConstructor
    public Node(String serverAddress, String userName,
                String sshKeyPath, boolean usePassword,String userPassword, String propertiesPath,
                boolean setPropertiesByHand,

                boolean isKernel,
                boolean isMaster,
                boolean isCoordinationServer,
                boolean isRdbServer,
                String rdbDrver,
                String rdbPort, String rdbName,String rdbUserName, String rdbPassword, String rdbDialect ,
                boolean isReporter,
                boolean isAgentServer) {

        this.serverAddress = serverAddress;
        this.userName = userName;
        this.userPassword = userPassword;
        this.sshKeyPath = sshKeyPath;
        this.usePassword = usePassword;
        this.propertiesPath = propertiesPath;
        this.setPropertiesByHand = setPropertiesByHand;

        if (setPropertiesByHand){
            fillRoles(
                    isKernel,
                    isMaster,
                    isCoordinationServer,
                    isRdbServer, rdbDrver, rdbPort ,rdbName , rdbUserName , rdbPassword, rdbDialect,
                    isReporter,
                    isAgentServer
                );
        }

    }

    public boolean isSetPropertiesByHand() {
        return setPropertiesByHand;
    }

    public String getPropertiesPath() {
        return propertiesPath;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public String getSshKeyPath() {
        return sshKeyPath;
    }

    public boolean isUsePassword() {
        return usePassword;
    }

    public ArrayList<Role> getRoles() {
        return roles;
    }


    private void fillRoles(boolean kernel,
                           boolean master,
                           boolean coordinationServer,
                           boolean rdbServer,
                           String rdbDriver,
                                String rdbPort, String rdbName,String rdbUserName, String rdbPassword, String rdbDialect,
                           boolean reporter,
                           boolean agentServer) {


        if(master) {
            roles.add(new Master());
        }
        if(rdbServer) {
            roles.add(new RdbServer(rdbDriver,rdbPort,rdbName,rdbUserName,rdbPassword,rdbDialect))  ;
        }



    }

    public Descriptor<Node> getDescriptor() {
        return Hudson.getInstance().getDescriptor(getClass());
    }


    @Extension
    public static class DescriptorImpl extends Descriptor<Node>{

        @Override
        public String getDisplayName() {
            return "Node";
        }
    }

}
