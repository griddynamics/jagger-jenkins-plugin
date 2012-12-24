package com.griddynamics.jagger.jenkins.plugin;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

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

                Master master,
                RdbServer rdbServer,
                CoordinationServer coordinationServer,
                Kernel kernel,
                Reporter reporter ,
                NodeToAttack nodeToAttack


    ) {

        this.serverAddress = serverAddress;
        this.userName = userName;
        this.userPassword = userPassword;
        this.sshKeyPath = sshKeyPath;
        this.usePassword = usePassword;
        this.propertiesPath = propertiesPath;
        this.setPropertiesByHand = setPropertiesByHand;

        if (setPropertiesByHand){
            fillRoles(nodeToAttack ,master, rdbServer , coordinationServer, kernel, reporter );
        }

    }

    public NodeToAttack getNodeToAttack() {
        for(Role role:getRoles()){
            if(role instanceof NodeToAttack){
                return (NodeToAttack) role;
            }
        }
        return null;
    }

    public CoordinationServer getCoordinationServer() {
        for(Role role:getRoles()){
            if(role instanceof CoordinationServer){
                return (CoordinationServer) role;
            }
        }
        return null;
    }

    public Kernel getKernel() {
        for(Role role:getRoles()){
            if(role instanceof Kernel){
                return (Kernel) role;
            }
        }
        return null;
    }

    public Reporter getReporter() {
        for(Role role:getRoles()){
            if(role instanceof Reporter){
                return (Reporter) role;
            }
        }
        return null;
    }

    public Master getMaster() {
        for(Role role:getRoles()){
            if(role instanceof Master){
                return (Master) role;
            }
        }
        return null;
    }

    public RdbServer getRdbServer() {
        for(Role role:getRoles()){
            if(role instanceof RdbServer){
                return (RdbServer) role;
            }
        }
        return null;
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


    private void fillRoles(Role ... roles) {

        for(Role role : roles) {
            if(role != null){
                this.roles.add(role);
            }
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

        public FormValidation doCheckServerAddress(@QueryParameter String value) {

            try {


                if(value == null || value.matches("\\s*")) {
                    return FormValidation.error("Set Address");
                }

                InetAddress.getByName(value).isReachable(1000);
                return FormValidation.ok();

            } catch(UnknownHostException e){

                return FormValidation.error("Bad Server Address");
            } catch (IOException e) {

                return FormValidation.error("Server With That Address Unavailable");
            }
        }

    }

}
