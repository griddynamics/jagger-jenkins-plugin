package com.griddynamics.jagger.jenkins.plugin;

import org.kohsuke.stapler.DataBoundConstructor;

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

    private final String sshOptions;

    private String sshOptionsActual;

    private final boolean setJavaHome;

    private final String javaHome;

    private String javaHomeActual;

    private final String javaOptions;

    private String javaOptionsActual;

    @DataBoundConstructor
    public Node(String serverAddress, String userName,
                String sshKeyPath, String sshOptions,
                boolean setJavaHome, String javaHome, String javaOptions) {

        this.serverAddress = serverAddress;
        this.serverAddressActual = serverAddress;
        this.userName = userName;
        this.userNameActual = userName;
        this.sshKeyPath = sshKeyPath;
        this.sshKeyPathActual = sshKeyPath;
        this.sshOptions = sshOptions;
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

    public String getSshKeyPath() {
        return sshKeyPath;
    }

    public String getSshKeyPathActual() {
        return sshKeyPathActual;
    }

    public void setSshKeyPathActual(String sshKeyPathActual) {
        this.sshKeyPathActual = sshKeyPathActual;
    }

    public String getSshOptions() {
        return sshOptions;
    }

    public String getSshOptionsActual() {
        return sshOptionsActual;
    }

    public void setSshOptionsActual(String sshOptionsActual) {
        this.sshOptionsActual = sshOptionsActual;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getUserName() {
        return userName;
    }

    public void setServerAddressActual(String s) {
        serverAddressActual = s;
    }
}
