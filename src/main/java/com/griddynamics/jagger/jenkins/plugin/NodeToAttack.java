package com.griddynamics.jagger.jenkins.plugin;


import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.FormValidation;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.*;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.security.PublicKey;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: amikryukov
 * Date: 12/21/12
 */


/**
 * To Make Object of Server that we want to test
 */
public class NodeToAttack implements Describable<NodeToAttack> {

    private final String serverAddress;

    private final boolean installAgent;

    private final String userName;

    private final String userPassword;

    private final String sshKeyPath ;

    private final boolean usePassword;


    @DataBoundConstructor
    public NodeToAttack(String serverAddress , boolean installAgent, String userName, String sshKeyPath,
                        boolean usePassword, String userPassword){
//
//        if(serverAddress.matches("\\$\\{\\.+\\}")){
//            this.serverAddress
//        }

        this.serverAddress = serverAddress;
        this.installAgent = installAgent;
        this.userName = userName;
        this.sshKeyPath = sshKeyPath;
        this.usePassword = usePassword;
        this.userPassword = userPassword;
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

    public boolean isInstallAgent() {
        return installAgent;
    }

    public Descriptor<NodeToAttack> getDescriptor() {

        return Hudson.getInstance().getDescriptor(getClass());
    }

    @Override
    public String toString() {
        return "NodeToAttack{" +
                "serverAddress='" + serverAddress + '\'' +
                ", installAgent=" + installAgent +
                ", userName='" + userName + '\'' +
                ", userPassword='" + userPassword + '\'' +
                ", sshKeyPath='" + sshKeyPath + '\'' +
                ", usePassword=" + usePassword +
                '}';
    }

    @Extension
    public static class DescriptorNTA extends Descriptor<NodeToAttack>{

        @Override
        public String getDisplayName() {
            return "NodeToAttack";
        }

        /**
         * For test Server Address if it available
         * @param value String from Server Address form
         * @return OK if ping, ERROR otherwise
         */
        public FormValidation doCheckServerAddress(@QueryParameter String value,@QueryParameter("installAgent") boolean installAgent) {

            try {

                if(value == null || value.matches("\\s*")) {
                    return FormValidation.warning("Set Address");
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