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
public class Reporter implements Role, Describable<Reporter> {

    @DataBoundConstructor
    public Reporter(){}

    @Override
    public String toString() {
        return "Reporter";
    }

    public Descriptor<Reporter> getDescriptor() {
        return Hudson.getInstance().getDescriptor(getClass());
    }

    public RoleTypeName getRoleType() {
        return RoleTypeName.REPORTER;
    }


    @Extension
    public static class DescriptorR extends Descriptor<Reporter>{

        @Override
        public String getDisplayName() {
            return "Reporter";
        }
    }
}
