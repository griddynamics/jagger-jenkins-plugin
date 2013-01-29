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

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.security.PublicKey;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: amikryukov
 * Date: 12/14/12
 */
public class Node implements Describable<Node> {

    private final String serverAddress;

    private String serverAddressActual;

    private final String userName;

    private final String userPassword;

    private final String sshKeyPath ;

    private String propertiesPath;

    private String finalPropertiesPath;

    private final boolean usePassword;

    private final boolean setPropertiesByHand;

    private HashMap<RoleTypeName, Role> hmRoles;

    @DataBoundConstructor
    public Node(String serverAddress, String userName,
                String sshKeyPath, boolean usePassword, String userPassword, String propertiesPath,
                boolean setPropertiesByHand,

                Master master,
         //       DBOptions rdbServer,
                CoordinationServer coordinationServer,
                Kernel kernel,
                Reporter reporter
       //     ,                           String serverAddressName
    ) {

        this.serverAddress = serverAddress;
        this.userName = userName;
        this.userPassword = userPassword;
        this.sshKeyPath = sshKeyPath;
        this.usePassword = usePassword;
        this.propertiesPath = propertiesPath;
        this.setPropertiesByHand = setPropertiesByHand;
        this.serverAddressActual = serverAddress;
        hmRoles = new HashMap<RoleTypeName, Role>(RoleTypeName.values().length);

        if (setPropertiesByHand){
            fillRoles(master, coordinationServer, kernel, reporter );
        }
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

    public boolean isSetPropertiesByHand() {
        return setPropertiesByHand;
    }

    public String getFinalPropertiesPath() {
        return finalPropertiesPath;
    }

    public void setFinalPropertiesPath(String finalPropertiesPath) {
        this.finalPropertiesPath = finalPropertiesPath;
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

    public HashMap<RoleTypeName, Role> getHmRoles() {
        return hmRoles;
    }

    private void fillRoles(Role ... roles) {

        for(Role role: roles){
            if(role != null){
                hmRoles.put(role.getRoleType(),role);
            }
        }
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

                new Socket(value,22).close();

            } catch (UnknownHostException e) {
                return FormValidation.error("Unknown Host");
            } catch (IOException e) {
                return FormValidation.error("Can't Reach Host on 22 port");
            }
            return FormValidation.ok();
        }


        /**
         * Checking properties path
         * @param setPropertiesByHand boolean from form
         * @param propertiesPath String from form
         * @return Form Validation OK / ERROR
         */
        public FormValidation doCheckPropertiesPath(@QueryParameter("setPropertiesByHand") final boolean setPropertiesByHand,
                                                @QueryParameter("propertiesPath")final String propertiesPath) {

            if(setPropertiesByHand){
                return FormValidation.ok();
            } else {

                if(propertiesPath.matches("\\s*")){
                    return FormValidation.error("Set Properties Path, or Set Properties By Hand");
                }
                    if(! new File("path").exists()){
                        return FormValidation.error("File not exist");
                    }
                return FormValidation.ok();
            }
        }


        /**
         * May be we don't need such verification
         * For testing Connection to remote machine(ssh, rdb connection ... )
         * @param  serverAddress         serverAddress
         * @param userName      userName
         * @param sshKeyPath        sshKeyPath
         * @param usePassword        usePassword
         * @param userPassword         userPassword
         * @return   OK if connect, ERROR if there are some fails
         */
        public FormValidation doTestConnection(@QueryParameter("serverAddress") final String serverAddress,
                                                  @QueryParameter("userName") final String userName,
                                                  @QueryParameter("sshKeyPath") final String sshKeyPath,
                                                  @QueryParameter("usePassword") final boolean usePassword,
                                                  @QueryParameter("userPassword") final String userPassword
                                                )
        {
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
