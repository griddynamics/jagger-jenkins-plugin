package com.griddynamics.jagger.jenkins.plugin;

/**
 * Created with IntelliJ IDEA.
 * User: amikryukov
 * Date: 12/19/12
 */
public class Role {
}

class Master extends Role{
    @Override
    public String toString() {
        return "MASTER";
    }
}

class RdbServer extends Role {

    String rdbDriver,
           rdbPort,
           rdbName,
           rdbUserName,
           rdbPassword,
           rdbDialect;


    RdbServer(String rdbDriver, String rdbPort, String rdbName,
              String rdbUserName, String rdbPassword, String rdbDialect){

        this.rdbDriver = rdbDriver;
        this.rdbPort = rdbPort;
        this.rdbName = rdbName;
        this.rdbUserName = rdbUserName;
        this.rdbPassword = rdbPassword;
        this.rdbDialect = rdbDialect;
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
