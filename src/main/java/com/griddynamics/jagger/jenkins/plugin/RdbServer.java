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

public class RdbServer extends Role implements Describable<RdbServer> {

    private final String rdbDriver,
            rdbPort,
            rdbName,
            rdbUserName,
            rdbPassword,
            rdbDialect;

    @DataBoundConstructor
    public RdbServer(String rdbDriver, String rdbPort, String rdbName,
              String rdbUserName, String rdbPassword, String rdbDialect){

        this.rdbDriver = rdbDriver;
        this.rdbPort = rdbPort;
        this.rdbName = rdbName;
        this.rdbUserName = rdbUserName;
        this.rdbPassword = rdbPassword;
        this.rdbDialect = rdbDialect;
    }


    public String getRdbDriver() {
        return rdbDriver;
    }

    public String getRdbPort() {
        return rdbPort;
    }

    public String getRdbName() {
        return rdbName;
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

    public Descriptor<RdbServer> getDescriptor() {
        return Hudson.getInstance().getDescriptor(getClass());
    }


    @Extension
    public static class DescriptorImpl extends Descriptor<RdbServer>{

        @Override
        public String getDisplayName() {
            return "RDB_SERVER";
        }
    }



    @Override
    public String toString() {
        return "RDB_SERVER{" +
                "rdbDriver='" + rdbDriver + '\'' +
                ", rdbPort='" + rdbPort + '\'' +
                ", rdbName='" + rdbName + '\'' +
                ", rdbUserName='" + rdbUserName + '\'' +
                ", rdbPassword='" + rdbPassword + '\'' +
                ", rdbDialect='" + rdbDialect + '\'' +
                '}';
    }
}
