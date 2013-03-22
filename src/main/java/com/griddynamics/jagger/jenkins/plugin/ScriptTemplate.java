package com.griddynamics.jagger.jenkins.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by IntelliJ IDEA.
 * User: amikryukov
 * Date: 3/21/13
 */
public enum ScriptTemplate {

    MAIN("script/main.script"),
    DEPLOYING("script/deploying.script"),
    START_NODES("script/starting.nodes.script"),
    START_MASTER("script/master.starting.script"),
    START_KERNEL("script/kernel.starting.script"),
    START_AGENT("script/agent.starting.script"),
    CHECK_KERNEL("script/kernel.checking.script"),
    CHECK_AGENT("script/agent.checking.script"),
    COPY_LOGS("script/copy.logs.script"),
    COPY_REPORTS("script/copy.reports.script"),
    STOP_AGENT("script/agent.stop.script");

    private String templatePath;

    ScriptTemplate(String path) {
        this.templatePath= path;
    }

    public String getTemplatePath(){
        return templatePath;
    }

    public String getTemplateString() throws IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(getTemplatePath())));

        StringBuilder sb = new StringBuilder();
        while (br.ready()) {
            sb.append(br.readLine()).append("\n");
        }

        return sb.toString();
    }
}
