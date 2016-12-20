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
 * Created by IntelliJ IDEA.
 * User: amikryukov
 * Date: 2/20/13
 */
public class MasterNode extends Node implements Describable<MasterNode> {

    @DataBoundConstructor
    public MasterNode(String serverAddress, String userName, String sshKeyPath, String sshOptions, boolean setJavaHome, String javaHome, String javaOptions) {
        super(serverAddress, userName, sshKeyPath, sshOptions, setJavaHome, javaHome, javaOptions);
    }

    public Descriptor getDescriptor() {
        return Hudson.getInstance().getDescriptor(getClass());
    }

     @Extension
    public static class DescriptorMN extends Descriptor<MasterNode> {

        @Override
        public String getDisplayName() {
            return "Master Node";
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
                if(value.contains("$")) {
                    return FormValidation.ok();
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
         * test Java Home
         * @param value String from JavaHome form
         * @return FormValidation
         */
        public FormValidation doCheckJavaHome(@QueryParameter String value) {

            if(value == null || value.matches("\\s*")) {
                return FormValidation.warning("Set JavaHome");
            } else {
                return FormValidation.ok();
            }
        }
    }

}
