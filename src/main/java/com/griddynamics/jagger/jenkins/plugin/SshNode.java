package com.griddynamics.jagger.jenkins.plugin;

/**
 * Created by IntelliJ IDEA.
 * User: amikryukov
 * Date: 2/5/13
 */
public interface SshNode {

    public String getUserNameActual();

    public void setUserNameActual(String userNameActual);

    public String getSshKeyPathActual();

    public void setSshKeyPathActual(String sshKeyPathActual);

    public String getServerAddressActual();

    public String getUserName();

    public String getSshKeyPath() ;

    public String getServerAddress();

    public void setServerAddressActual(String s);

    public String getJavaHome();

    public void setJavaHomeActual(String javaHomeActual);

    public void setJavaOptionsActual(String javaOptionsActual);

    public String getJavaOptions();
}
