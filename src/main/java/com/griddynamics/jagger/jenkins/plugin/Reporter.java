package com.griddynamics.jagger.jenkins.plugin;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.codehaus.groovy.tools.shell.IO;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.w3c.tidy.Report;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: amikryukov
 * Date: 12/21/12
 */
public class Reporter implements Role, Describable<Reporter> {

    @DataBoundConstructor
    public Reporter(){

    }

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
