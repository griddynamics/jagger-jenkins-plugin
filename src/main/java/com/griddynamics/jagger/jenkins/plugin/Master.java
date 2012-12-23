package com.griddynamics.jagger.jenkins.plugin;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created with IntelliJ IDEA.
 * User: Andrey
 * Date: 20/12/12
 */
public class Master extends Role implements Describable<Master> {

    @DataBoundConstructor
    public Master(){}

    @Override
    public String toString() {
        return "MASTER";
    }

    public Descriptor<Master> getDescriptor() {
        return Hudson.getInstance().getDescriptor(getClass());
    }


    @Extension
    public static class DescriptorImpl extends Descriptor<Master>{

        @Override
        public String getDisplayName() {
            return "MASTER";
        }
    }

}