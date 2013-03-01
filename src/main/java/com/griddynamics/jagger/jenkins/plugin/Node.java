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
public class Node implements SshNode {

    private final String serverAddress;

    private String serverAddressActual;

    private final String userName;

    private String userNameActual;

    private final String sshKeyPath ;

    private String sshKeyPathActual;

    private final boolean setJavaHome;

    private final String javaHome;

    private String javaHomeActual;

    private final String javaOptions;

    private String javaOptionsActual;

    @DataBoundConstructor
    public Node(String serverAddress, String userName,
                String sshKeyPath,

                boolean setJavaHome, String javaHome, String javaOptions) {

        this.serverAddress = serverAddress;
        this.serverAddressActual = serverAddress;
        this.userName = userName;
        this.userNameActual = userName;
        this.sshKeyPath = sshKeyPath;
        this.sshKeyPathActual = sshKeyPath;
        this.setJavaHome = setJavaHome;
        this.javaHome = javaHome;
        this.javaOptions = javaOptions;

    }

    public void setJavaOptionsActual(String javaOptionsActual) {
        this.javaOptionsActual = javaOptionsActual;
    }

    public String getJavaOptions() {
        return javaOptions;
    }

    public String getJavaOptionsActual() {
        return javaOptionsActual;
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

    public String getServerAddressActual() {
        return serverAddressActual;
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


    public void setServerAddressActual(String s) {
        serverAddressActual = s;
    }

}
