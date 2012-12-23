package com.griddynamics.jagger.jenkins.plugin;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created with IntelliJ IDEA.
 * User: amikryukov
 * Date: 12/21/12
 */
public class Reporter extends Role implements Describable<Reporter> {

    @DataBoundConstructor
    public Reporter(){}

    @Override
    public String toString() {
        return "Reporter";
    }

    public Descriptor<Reporter> getDescriptor() {
        return Hudson.getInstance().getDescriptor(getClass());
    }


    @Extension
    public static class DescriptorImpl extends Descriptor<Reporter>{

        @Override
        public String getDisplayName() {
            return "Reporter";
        }
    }
}
