package com.griddynamics.jagger.jenkins.plugin;


import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.FormValidation;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.net.*;
import java.security.PublicKey;

/**
 * Created with IntelliJ IDEA.
 * User: amikryukov
 * Date: 12/21/12
 */


/**
 * To Make Object of Server that we want to test
 */
public class SuT implements Describable<SuT>, SshNode {

    private final String serverAddress;   //to view in Jenkins - could be $Parametr for example

    private String serverAddressActual;  //actual address

    private final String userName;

    private String userNameActual;

    private final String userPassword;

    private final String sshKeyPath ;

    private String sshKeyPathActual;

    private final boolean usePassword;


    @DataBoundConstructor
    public SuT(String serverAddress, String userName, String sshKeyPath,
               boolean usePassword, String userPassword){

        this.serverAddress = serverAddress;
        this.serverAddressActual = serverAddress;
        this.userName = userName;
        this.userNameActual = userName;
        this.sshKeyPath = sshKeyPath;
        this.sshKeyPathActual = sshKeyPath;
        this.usePassword = usePassword;
        this.userPassword = userPassword;
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

    public String getServerAddressActual() {
        return serverAddressActual;
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

    public String getServerAddress() {
        return serverAddress;
    }

    public Descriptor<SuT> getDescriptor() {

        return Hudson.getInstance().getDescriptor(getClass());
    }

    @Override
    public String toString() {
        return "SuT{" +
                "serverAddress='" + serverAddressActual + '\'' +
                ", userName='" + userName + '\'' +
                ", userPassword='" + userPassword + '\'' +
                ", sshKeyPath='" + sshKeyPath + '\'' +
                ", usePassword=" + usePassword +
                '}';
    }

    public void setServerAddressActual(String s) {
        serverAddressActual = s;
    }

    @Extension
    public static class DescriptorNTA extends Descriptor<SuT>{

        @Override
        public String getDisplayName() {
            return "SuT";
        }

        /**
         * For test Server Address if it available
         * @param value String from Server Address form
         * @param installAgent then we can test ssh port 22
         * @return OK if ping, ERROR otherwise
         */
        public FormValidation doCheckServerAddress(@QueryParameter String value,@QueryParameter("installAgent") boolean installAgent) {

            try {

                if(value == null || value.matches("\\s*")) {
                    return FormValidation.warning("Set Address");
                }

                if(value.startsWith("$")){
                    return FormValidation.ok();
                }

                if(installAgent){
                    new Socket(value,22).close();
                } else {

                    String cmd = "";
                    if(System.getProperty("os.name").startsWith("windows")){
                        cmd = "ping -n 1 " + value;
                    } else {
                        cmd = "ping -c 1 " + value;
                    }
                    Process p1 = java.lang.Runtime.getRuntime().exec(cmd);
                    if(p1.waitFor() == 0) {
                        return FormValidation.ok();
                    } else {
                        return FormValidation.error("Server unreachable");
                    }
                }
            } catch (UnknownHostException e) {
                return FormValidation.error("Unknown Host\t"+value+"\t"+e.getLocalizedMessage());
            } catch (IOException e) {
                return FormValidation.error("Can't Reach Host on 22 port");
            } catch (InterruptedException e) {
                return FormValidation.error("Interrapted Exception");
            }
            return FormValidation.ok();
        }

        /**
         * For test ssh connection / permissions to make 'magic'
         * @param serverAddress   STRING host address
         * @param userName        STRING user name
         * @param sshKeyPath      STRING path of ssh Key
         * @param usePassword     BOOLEAN if password in use
         * @param userPassword    STRING user password
         * @return   ok if connect, error otherwise
         */
        public FormValidation doTestSSHConnection(@QueryParameter("serverAddress") final String serverAddress,
                                               @QueryParameter("userName") final String userName,
                                               @QueryParameter("sshKeyPath") final String sshKeyPath,
                                               @QueryParameter("usePassword") final boolean usePassword,
                                               @QueryParameter("userPassword") final String userPassword) {
            try {

                final SSHClient ssh = new SSHClient();

                ssh.addHostKeyVerifier(new HostKeyVerifier() {
                    public boolean verify(String arg0, int arg1, PublicKey arg2) {
                        return true;  // don't bother verifying
                    }
                });

                ssh.connect(serverAddress);
                try{
                    if (usePassword){
                        ssh.authPassword(userName,userPassword);
                    } else {
                        ssh.authPublickey(userName,sshKeyPath);
                    }

                    net.schmizz.sshj.connection.channel.direct.Session session = null;
                    try{
                        session = ssh.startSession();
                    } finally {
                        if(session != null){
                            session.close();
                        }
                    }

                } finally {
                    ssh.disconnect();
                }


                return FormValidation.ok("ok");

            } catch (ConnectException e) {
                return FormValidation.error("can't make even connection");
            } catch (IOException e) {
                return FormValidation.error("Can't connect with such configuration\n"+e.getLocalizedMessage());
            }
        }

    }

}