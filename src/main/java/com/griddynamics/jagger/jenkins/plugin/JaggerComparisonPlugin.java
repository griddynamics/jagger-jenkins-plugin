package com.griddynamics.jagger.jenkins.plugin;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;


public class JaggerComparisonPlugin extends Builder {

    private final String path;
    private final boolean stopOnErrors; //ignoring errors in plugin

    @DataBoundConstructor
    public JaggerComparisonPlugin(String path, boolean stopOnErrors) {
        this.path = path;
        this.stopOnErrors = stopOnErrors;
    }

    public String getPath() {
        return path;
    }

    public boolean isStopOnErrors() {
        return stopOnErrors;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        SessionDecision decision;
        try{
            String filePath = build.getEnvironment(listener).expand(getPath());
            FilePath file = new FilePath(build.getExecutor().getCurrentWorkspace(), filePath);
            listener.getLogger().println("Reading file: " + file.absolutize());
            decision = SessionDecision.create(file);

        } catch (Exception e){
            listener.getLogger().println("Plugin exception: " + e.toString());
            if (isStopOnErrors()){
                return false;
            }
            listener.getLogger().println("Ignoring error");
            return true;
        }
        Result currentBuildResult=build.getResult();
        Decision result=decision.makeDecision();
        listener.getLogger().println("Comparison decision: " + decision.getComparisonDecision());
        listener.getLogger().println("Session decision: " + decision.getSessionDecision());
        listener.getLogger().println("Result decision: " + result);

        switch(result){
            case FATAL:
            case WARNING:
                if (currentBuildResult==null || Result.UNSTABLE.isWorseOrEqualTo(currentBuildResult)){
                    build.setResult(Result.UNSTABLE);
                }
                break;
            case OK:
                break;
        }
        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public FormValidation doCheckPath(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0){
                return FormValidation.error("Please set a path");
            }
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Jagger session comparison";
        }
    }
}

