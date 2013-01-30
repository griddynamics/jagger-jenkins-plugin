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

public class DBOptions implements Describable<DBOptions> {

    public boolean useExternalDB = true;
    private final String rdbDriver;
    private final String rdbClientUrl;
    private final String rdbUserName;
    private final String rdbPassword;
    private final String rdbDialect;


    @DataBoundConstructor
    public DBOptions(String rdbDriver, String rdbClientUrl,
                     String rdbUserName, String rdbPassword, String rdbDialect, boolean useExternalDB){

        this.useExternalDB = useExternalDB;
        this.rdbDriver = rdbDriver;
        this.rdbClientUrl = rdbClientUrl;
        this.rdbUserName = rdbUserName;
        this.rdbPassword = rdbPassword;
        this.rdbDialect = rdbDialect;
    }

    public boolean isUseExternalDB() {
        return useExternalDB;
    }

    public String getRdbDriver() {
        return rdbDriver;
    }

    public String getRdbClientUrl() {
        return rdbClientUrl;
    }

    public String getRdbUserName() {
        return rdbUserName;
    }

    public String getRdbPassword() {
        return rdbPassword;
    }

    public String getRdbDialect() {
        return rdbDialect;
    }

    public Descriptor<DBOptions> getDescriptor() {
        return Hudson.getInstance().getDescriptor(getClass());
    }

    public RoleTypeName getRoleType() {
        return RoleTypeName.RDB_SERVER;
    }


    @Extension
    public static class DescriptorRDBS extends Descriptor<DBOptions>{

        @Override
        public String getDisplayName() {
            return "RDB_SERVER";
        }
    }

    @Override
    public String toString() {
        return "RDB_SERVER{" +
                "rdbDriver='" + rdbDriver + '\'' +
                ", rdbClientUrl='" + rdbClientUrl + '\'' +
                ", rdbUserName='" + rdbUserName + '\'' +
                ", rdbPassword='" + rdbPassword + '\'' +
                ", rdbDialect='" + rdbDialect + '\'' +
                '}';
    }
}
