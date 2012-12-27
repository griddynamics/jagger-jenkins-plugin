package com.griddynamics.jagger.jenkins.plugin;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.FormValidation;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.security.PublicKey;
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

                Process p1 = java.lang.Runtime.getRuntime().exec("ping -c 1 "+value);

                if(p1.waitFor() == 0) {
                    return FormValidation.ok();
                } else {
                    return FormValidation.error("Server unreachable");
                }
            } catch(UnknownHostException e){

                return FormValidation.error("Bad Server Address");
            } catch (IOException e) {

                return FormValidation.error("Server With That Address Unavailable");
            } catch (InterruptedException e) {
                return FormValidation.error("");
            }
        }

        public FormValidation doCheckPropertiesPath(@QueryParameter("setPropertiesByHand") final boolean setPropertiesByHand,
                                                @QueryParameter("propertiesPath")final String propertiesPath) {

            if(setPropertiesByHand){
                return FormValidation.ok();
            } else {

                if(propertiesPath.matches("\\s*")){
                    return FormValidation.error("Set Properties Path, or Set Properties By Hand");
                }
                return FormValidation.warning("not yet implemented : "+propertiesPath);
            }

        }

        /**
         * For testing Connection to remote machine(ssh, rdb connection ... )
         * @param  serverAddress         serverAddress
         * @param userName      userName
         * @param sshKeyPath        sshKeyPath
         * @param usePassword        usePassword
         * @param userPassword         userPassword
         * @param propertiesPath        propertiesPath
//       * @param master               master
//       * @param rdbServer             rdbServer
//       * @param coordinationServer       coordinationServer
//       * @param kernel       kernel
//       * @param reporter            reporter
         * @return   OK if connect, ERROR if there are some fails
         */
        public FormValidation doTestConnection(@QueryParameter("serverAddress") final String serverAddress,
                                                  @QueryParameter("userName") final String userName,
                                                  @QueryParameter("sshKeyPath") final String sshKeyPath,
                                                  @QueryParameter("usePassword") final boolean usePassword,
                                                  @QueryParameter("userPassword") final String userPassword,
                                                  @QueryParameter("propertiesPath") final String propertiesPath
//                                                  ,@QueryParameter("master") final Master master,
//                                                  @QueryParameter("rdbServer") final RdbServer rdbServer,
//                                                  @QueryParameter("coordinationServer") final CoordinationServer coordinationServer,
//                                                  @QueryParameter("kernel") final Kernel kernel,
//                                                  @QueryParameter("reporter") final Reporter reporter
                                                ) {
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
                        ssh.authPublickey(userName);
                    }
                    final Session session = ssh.startSession();
                    session.close();

                } finally {
                    ssh.disconnect();
                }



                return FormValidation.ok();

            } catch (IOException e) {
                return FormValidation.error("Can't connect with such configuration\n"+e.getLocalizedMessage());
            }
        }

        /**
         * To test RdbConnection
         * @param serverAddress     serverAddress
         * @param rdbServer          rdbServer
         * @return null if connection OK FormValidation otherwise
         */
        public FormValidation doCheckRdbConnection(String serverAddress, RdbServer rdbServer){

            return  null;
        }

    }

}
