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

    private final String userName;

    private final String userPassword;

    private final String sshKeyPath ;

    private final boolean usePassword;


    @DataBoundConstructor
    public NodeToAttack(String serverAddress , boolean installAgent, String userName, String sshKeyPath,
                        boolean usePassword, String userPassword){

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
    public static class DescriptorImpl extends Descriptor<NodeToAttack>{

        @Override
        public String getDisplayName() {
            return "NodeToAttack";
        }

        /**
         * For test Server Address if it available
         * @param value String from Server Address form
         * @return OK if ping, ERROR otherwise
         */
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

        public FormValidation doTestSSHConnection(@QueryParameter("serverAddress") final String serverAddress,
                                               @QueryParameter("installAgent") final boolean installAgent,
                                               @QueryParameter("userName") final String userName,
                                               @QueryParameter("sshKeyPath") final String sshKeyPath,
                                               @QueryParameter("usePassword") final boolean usePassword,
                                               @QueryParameter("userPassword") final String userPassword) {
            try {
                return FormValidation.ok("serverAddress :"+serverAddress+"\t"+
                        "install Agent :"+installAgent+"\t"+"userName :"+userName+"\t"+
                        "sshKeyPath :"+sshKeyPath+"\t"+"usePassword :"+usePassword+"\t"+
                        "userPassword :"+userPassword);
            } catch (Exception e) {
                return FormValidation.error("Bad Luck");
            }
        }

    }

}