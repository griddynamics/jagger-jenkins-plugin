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
public class CoordinationServer implements Role, Describable<CoordinationServer> {

    @DataBoundConstructor
    public CoordinationServer(){

    }

    public RoleTypeName getRoleType() {
        return RoleTypeName.COORDINATION_SERVER;
    }

    @Extension
    public static class DescriptorCS extends Descriptor<CoordinationServer>{

        @Override
        public String getDisplayName() {
            return "COORDINATION_SERVER";
        }
    }


    public Descriptor<CoordinationServer> getDescriptor() {
        return Hudson.getInstance().getDescriptor(getClass());
    }
}
