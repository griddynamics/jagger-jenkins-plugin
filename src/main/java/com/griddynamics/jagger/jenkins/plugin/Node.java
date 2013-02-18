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
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: amikryukov
 * Date: 12/14/12
 */
public class Node implements Describable<Node>, SshNode {

    private final String serverAddress;

    private String serverAddressActual;

    private final String userName;

    private String userNameActual;

    private final String sshKeyPath ;

    private String sshKeyPathActual;

    private String rolesWithComas;

    private HashMap<RoleTypeName, Role> hmRoles;

    private final boolean setJavaHome;

    private final String javaHome;

    private String javaHomeActual;

    @DataBoundConstructor
    public Node(String serverAddress, String userName,
                String sshKeyPath,

                Master master,
                CoordinationServer coordinationServer,
                Kernel kernel,
                Reporter reporter,
                boolean setJavaHome, String javaHome) {

        this.serverAddress = serverAddress;
        this.serverAddressActual = serverAddress;
        this.userName = userName;
        this.userNameActual = userName;
        this.sshKeyPath = sshKeyPath;
        this.sshKeyPathActual = sshKeyPath;
        this.setJavaHome = setJavaHome;
        this.javaHome = javaHome;

        setJavaHomeActual(javaHome);

        hmRoles = new HashMap<RoleTypeName, Role>(RoleTypeName.values().length);
        fillRoles(master, coordinationServer, kernel, reporter);
    }

    public boolean isSetJavaHome() {
        return setJavaHome;
    }

    public String getJavaHomeActual() {
        return javaHomeActual;
    }

    public void setJavaHomeActual(String javaHomeActual) {
        this.javaHomeActual = javaHomeActual;
    }

    public String getJavaHome() {
        return javaHome;
    }

    private void fillRoles(Role ... roles) {

        rolesWithComas = "";
        for(Role role : roles){
            if(role != null) {

                hmRoles.put(role.getRoleType(), role);

                if(rolesWithComas.isEmpty()){

                    rolesWithComas += role.getRoleType().toString();

                    if(role.getRoleType().equals(RoleTypeName.MASTER)) {
                        rolesWithComas += ",HTTP_COORDINATION_SERVER";
                    }
                } else {

                    rolesWithComas += "," + role.getRoleType().toString();

                    if(role.getRoleType().equals(RoleTypeName.MASTER)) {
                        rolesWithComas += ",HTTP_COORDINATION_SERVER";
                    }
                }
            }
        }
    }

    public boolean isMaster() {
        return hmRoles.containsKey(RoleTypeName.MASTER);
    }

    public boolean isCoordinationServer() {
        return hmRoles.containsKey(RoleTypeName.COORDINATION_SERVER);
    }

    public boolean isKernel() {
        return hmRoles.containsKey(RoleTypeName.KERNEL);
    }

    public boolean isReporter() {
        return hmRoles.containsKey(RoleTypeName.REPORTER);
    }

    public String getRolesWithComas() {
        return rolesWithComas;
    }

    public CoordinationServer getCoordinationServer() {

        return (CoordinationServer) hmRoles.get(RoleTypeName.COORDINATION_SERVER);
    }

    public Kernel getKernel() {

            return (Kernel) hmRoles.get(RoleTypeName.KERNEL);
    }

    public String getServerAddressActual() {
        return serverAddressActual;
    }

    public Reporter getReporter() {

        return (Reporter) hmRoles.get(RoleTypeName.REPORTER);
    }

    public Master getMaster() {

        return (Master) hmRoles.get(RoleTypeName.MASTER);
    }

    public String getUserNameActual() {
        return userNameActual;
    }

    public void setUserNameActual(String userNameActual) {
        this.userNameActual = userNameActual;
    }

    public String getSshKeyPathActual() {
        return sshKeyPathActual;
    }

    public void setSshKeyPathActual(String sshKeyPathActual) {
        this.sshKeyPathActual = sshKeyPathActual;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getUserName() {
        return userName;
    }

    public String getSshKeyPath() {
        return sshKeyPath;
    }

    public HashMap<RoleTypeName, Role> getHmRoles() {
        return hmRoles;
    }

    public Descriptor<Node> getDescriptor() {
        return Hudson.getInstance().getDescriptor(getClass());
    }

    public void setServerAddressActual(String s) {
        serverAddressActual = s;
    }

    @Extension
    public static class DescriptorN extends Descriptor<Node>{

        @Override
        public String getDisplayName() {
            return "Node";
        }

        /**
         * For test Server Address if it available
         * @param value String from Server Address form
         * @return OK if ping, ERROR otherwise
         */
        public FormValidation doCheckServerAddress(@QueryParameter String value) {

            try {

                if(value == null || value.matches("\\s*")) {
                    return FormValidation.warning("Set Address");
                }
                if(value.contains("$")) {
                    return FormValidation.ok();
                }

                new Socket(value,22).close();

            } catch (UnknownHostException e) {
                return FormValidation.error("Unknown Host");
            } catch (IOException e) {
                return FormValidation.error("Can't Reach Host on 22 port");
            }
            return FormValidation.ok();
        }

        /**
         * test Java Home
         * @param value String from JavaHome form
         * @return FormValidation
         */
        public FormValidation doCheckJavaHome(@QueryParameter String value) {

            if(value == null || value.matches("\\s*")) {
                return FormValidation.warning("Set JavaHome");
            } else {
                return FormValidation.ok();
            }
        }
    }
}
