package com.griddynamics.jagger.jenkins.plugin;


import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created with IntelliJ IDEA.
 * User: amikryukov
 * Date: 12/21/12
 */


/**
 * To Make Object of Server that we want to test
 */
public class SuT extends Node implements Describable<SuT> {

    private final boolean useJmx;

    private final String jmxPort;

    private String jmxPortActual;

    @DataBoundConstructor
    public SuT(String serverAddress, String userName, String sshKeyPath,
               String jmxPort, boolean useJmx, boolean setJavaHome, String javaHome, String javaOptions){

         super(serverAddress, userName, sshKeyPath, setJavaHome, javaHome, javaOptions);

        this.jmxPort = jmxPort;
        this.useJmx = useJmx;

        setJmxPortActual(jmxPort);
    }


    public String getJmxPortActual() {
        return jmxPortActual;
    }

    public void setJmxPortActual(String jmxPortActual) {
        this.jmxPortActual = jmxPortActual;
    }

    public boolean isUseJmx() {
        return useJmx;
    }

    public String getJmxPort() {
        return jmxPort;
    }


    public Descriptor getDescriptor() {

        return Hudson.getInstance().getDescriptor(getClass());
    }

    @Extension
    public static class DescriptorNTA extends Descriptor<SuT>{

        @Override
        public String getDisplayName() {
            return "SUT";
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

                if(value.startsWith("$")){
                    return FormValidation.ok();
                }

                new Socket(value,22).close();

            } catch (UnknownHostException e) {
                return FormValidation.error("Unknown Host\t"+value+"\t"+e.getLocalizedMessage());
            } catch (IOException e) {
                return FormValidation.error("Input Output Exception while connecting to \t"+value+"\t"+e.getLocalizedMessage());
            }
            return FormValidation.ok();
        }

         /**
         * testing if jmx ports given correctly
         * @param value jmx port(s)
         * @return FormValidation
         */
        public FormValidation doCheckJmxPort(@QueryParameter String value) {

            if(value == null || value.matches("\\s*")) {
                return FormValidation.warning("Set JMX Port(s)");
            } else if (!value.matches("\\d+(,\\d+)*")) {
                return FormValidation.error("wrong format: split with comas");
            } else {
                return FormValidation.ok();
            }
        }
    }
}