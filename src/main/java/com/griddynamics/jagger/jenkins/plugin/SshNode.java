package com.griddynamics.jagger.jenkins.plugin;

/**
 * Created by IntelliJ IDEA.
 * User: amikryukov
 * Date: 2/5/13
 */
public interface SshNode {

    String getUserNameActual();

    void setUserNameActual(String userNameActual);

    String getSshKeyPathActual();

    void setSshKeyPathActual(String sshKeyPathActual);

    String getServerAddressActual();

    String getUserName();

    String getSshKeyPath();

    String getSshOptions();

    String getServerAddress();

    void setServerAddressActual(String s);

    String getJavaHome();

    void setJavaHomeActual(String javaHomeActual);

    void setJavaOptionsActual(String javaOptionsActual);

    String getJavaOptions();
}
