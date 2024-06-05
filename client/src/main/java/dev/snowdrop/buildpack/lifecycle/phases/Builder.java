package dev.snowdrop.buildpack.lifecycle.phases;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.command.WaitContainerResultCallback;

import dev.snowdrop.buildpack.ContainerLogReader;
import dev.snowdrop.buildpack.docker.ContainerUtils;
import dev.snowdrop.buildpack.lifecycle.ContainerStatus;
import dev.snowdrop.buildpack.lifecycle.LifecyclePhase;
import dev.snowdrop.buildpack.lifecycle.LifecyclePhaseFactory;
import dev.snowdrop.buildpack.utils.LifecycleArgs;


public class Builder implements LifecyclePhase{

    private static final Logger log = LoggerFactory.getLogger(Builder.class);

    final LifecyclePhaseFactory factory;

    public Builder( LifecyclePhaseFactory factory){
        this.factory = factory;
    }

    @Override
    public ContainerStatus runPhase(dev.snowdrop.buildpack.Logger logger, boolean useTimestamps) {

        LifecycleArgs args = new LifecycleArgs("/cnb/lifecycle/builder", null);

        //args.addArg("-analyzed", LifecyclePhaseFactory.LAYERS_VOL_PATH + "/analyzed.toml");
        args.addArg("-app", LifecyclePhaseFactory.WORKSPACE_VOL_PATH + LifecyclePhaseFactory.APP_PATH_PREFIX);
        args.addArg("-layers", LifecyclePhaseFactory.LAYERS_VOL_PATH);
        args.addArg("-platform", LifecyclePhaseFactory.PLATFORM_VOL_PATH);
        args.addArg("-log-level", factory.getLogConfig().getLogLevel());

        //builder process has to run as root.
        int runAsId = factory.getBuilderImage().getUserId();
        String id = factory.getContainerForPhase(args.toArray(), runAsId);
        try{
            log.info("- extender container id " + id+ " will be run with uid "+runAsId);                

            // launch the container!
            log.info("- launching builder container");
            factory.getDockerConfig().getDockerClient().startContainerCmd(id).exec();          

            log.info("- attaching log relay");
            // grab the logs to stdout.
            factory.getDockerConfig().getDockerClient().logContainerCmd(id)
                .withFollowStream(true)
                .withStdOut(true)
                .withStdErr(true)
                .withTimestamps(useTimestamps)
                .exec(new ContainerLogReader(logger));

            // wait for the container to complete, and retrieve the exit code.
            int rc = factory.getDockerConfig().getDockerClient().waitContainerCmd(id).exec(new WaitContainerResultCallback()).awaitStatusCode();
            log.info("Buildpack builder container complete, with exit code " + rc);    

            return ContainerStatus.of(rc,id);
        }catch(Exception e){
            if(id!=null){
                log.info("Exception during builder, removing container "+id);
                ContainerUtils.removeContainer(factory.getDockerConfig().getDockerClient(), id);
                log.info("remove complete");
            }
            throw e;
        }
    }
    
}
