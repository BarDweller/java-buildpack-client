package dev.snowdrop.buildpack.lifecycle.phases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;
import com.github.dockerjava.api.command.WaitContainerResultCallback;

import dev.snowdrop.buildpack.BuilderImage;
import dev.snowdrop.buildpack.Logger;
import dev.snowdrop.buildpack.config.DockerConfig;
import dev.snowdrop.buildpack.config.ImageReference;
import dev.snowdrop.buildpack.config.LogConfig;
import dev.snowdrop.buildpack.lifecycle.ContainerStatus;
import dev.snowdrop.buildpack.lifecycle.LifecyclePhaseFactory;
import dev.snowdrop.buildpack.lifecycle.Version;

@ExtendWith(MockitoExtension.class)
public class CreatorTest {

    private static final String LOG_LEVEL = "debug";
    private static final String CONTAINER_ID = "999";
    private static final int CONTAINER_RC = 99;
    private static final int USER_ID = 77;
    private static final int GROUP_ID = 88;
    private static final String OUTPUT_IMAGE = "stiletto";
    private static final boolean USE_DAEMON = false;

    @Captor
    ArgumentCaptor<String[]> argsCaptor;

    @Mock LifecyclePhaseFactory factory;
    @Mock BuilderImage builder;
    @Mock LogConfig logConfig;
    @Mock DockerConfig dockerConfig;
    @Mock DockerClient dockerClient;
    @Mock StartContainerCmd startCmd;
    @Mock LogContainerCmd logCmd;
    @Mock WaitContainerCmd waitCmd;
    @Mock WaitContainerResultCallback waitResult;
    @Mock Logger logger;

    @BeforeEach
    void setUp() {
        lenient().when(dockerConfig.getUseDaemon()).thenReturn(USE_DAEMON);
        lenient().when(dockerConfig.getDockerClient()).thenReturn(dockerClient);        
        lenient().when(factory.getDockerConfig()).thenReturn(dockerConfig);

        lenient().doNothing().when(startCmd).exec();
        lenient().when(dockerClient.startContainerCmd(any())).thenReturn(startCmd);

        lenient().when(logCmd.withFollowStream(any())).thenReturn(logCmd);
        lenient().when(logCmd.withStdOut(any())).thenReturn(logCmd);
        lenient().when(logCmd.withStdErr(any())).thenReturn(logCmd);
        lenient().when(logCmd.withTimestamps(any())).thenReturn(logCmd);
        lenient().when(logCmd.exec(any())).thenReturn(null);
        lenient().when(dockerClient.logContainerCmd(any())).thenReturn(logCmd);

        lenient().when(waitCmd.exec(any())).thenReturn(waitResult);
        lenient().when(waitResult.awaitStatusCode()).thenReturn(CONTAINER_RC);
        lenient().when(dockerClient.waitContainerCmd(any())).thenReturn(waitCmd);

        lenient().when(logConfig.getLogLevel()).thenReturn(LOG_LEVEL);
        lenient().when(factory.getLogConfig()).thenReturn(logConfig);

        lenient().when(builder.getUserId()).thenReturn(USER_ID);
        lenient().when(builder.getGroupId()).thenReturn(GROUP_ID);
        lenient().when(builder.getRunImages(any())).thenReturn(Stream.of(new ImageReference("runimage1"), new ImageReference("runimage2")).collect(Collectors.toList()).toArray(new ImageReference[]{}));        
        lenient().when(factory.getBuilderImage()).thenReturn(builder);

        lenient().when(factory.getContainerForPhase(argsCaptor.capture(), any())).thenReturn(CONTAINER_ID);

        lenient().when(factory.getOutputImage()).thenReturn(new ImageReference(OUTPUT_IMAGE));
    }

    @Test
    void testPre9() {
        lenient().when(factory.getPlatformLevel()).thenReturn(new Version("0.7"));

        Creator c = new Creator(factory);

        ContainerStatus cs = c.runPhase(logger, true);

        assertNotNull(cs);
        assertEquals(CONTAINER_ID, cs.getContainerId());
        assertEquals(CONTAINER_RC, cs.getRc());

        String[] args = argsCaptor.getValue();
        assertNotNull(args);
        assertEquals("/cnb/lifecycle/creator", args[0]);
        assertEquals(new ImageReference(OUTPUT_IMAGE).getReferenceWithLatest(), args[args.length-1]);        

        List<String> argList = Arrays.asList(args);
        assertFalse(argList.contains("-launch-cache"));
        assertFalse(argList.contains("-daemon"));
        assertTrue(argList.contains(LOG_LEVEL));

        verify(logCmd).withTimestamps(true);
        verify(dockerClient).logContainerCmd(CONTAINER_ID);
        verify(factory).getContainerForPhase(any(String[].class), eq(0));
    }

    @Test
    void testDisableTimestamps() {
        lenient().when(factory.getPlatformLevel()).thenReturn(new Version("0.9"));

        Creator c = new Creator(factory);

        ContainerStatus cs = c.runPhase(logger, false);

        assertNotNull(cs);
        assertEquals(CONTAINER_ID, cs.getContainerId());
        assertEquals(CONTAINER_RC, cs.getRc());

        String[] args = argsCaptor.getValue();
        assertNotNull(args);
        assertEquals("/cnb/lifecycle/creator", args[0]);
        assertEquals(new ImageReference(OUTPUT_IMAGE).getReferenceWithLatest(), args[args.length-1]);        

        List<String> argList = Arrays.asList(args);
        assertFalse(argList.contains("-launch-cache"));
        assertFalse(argList.contains("-daemon"));
        assertTrue(argList.contains(LOG_LEVEL));

        verify(logCmd).withTimestamps(false);
        verify(dockerClient).logContainerCmd(CONTAINER_ID);
        verify(factory).getContainerForPhase(any(String[].class), eq(0));
    }  

    @Test
    void test9OnwardsWithoutDaemon() {
        lenient().when(factory.getPlatformLevel()).thenReturn(new Version("0.9"));

        Creator c = new Creator(factory);

        ContainerStatus cs = c.runPhase(logger, true);

        assertNotNull(cs);
        assertEquals(CONTAINER_ID, cs.getContainerId());
        assertEquals(CONTAINER_RC, cs.getRc());

        String[] args = argsCaptor.getValue();
        assertNotNull(args);
        assertEquals("/cnb/lifecycle/creator", args[0]);
        assertEquals(new ImageReference(OUTPUT_IMAGE).getReferenceWithLatest(), args[args.length-1]);        

        List<String> argList = Arrays.asList(args);
        assertFalse(argList.contains("-launch-cache"));
        assertFalse(argList.contains("-daemon"));
        assertTrue(argList.contains(LOG_LEVEL));

        verify(logCmd).withTimestamps(true);
        verify(dockerClient).logContainerCmd(CONTAINER_ID);
        verify(factory).getContainerForPhase(any(String[].class), eq(0));
    }     

    @Test
    void test9OnwardsWithDaemon() {
        lenient().when(dockerConfig.getUseDaemon()).thenReturn(true);
        lenient().when(factory.getPlatformLevel()).thenReturn(new Version("0.9"));

        Creator c = new Creator(factory);

        ContainerStatus cs = c.runPhase(logger, true);

        assertNotNull(cs);
        assertEquals(CONTAINER_ID, cs.getContainerId());
        assertEquals(CONTAINER_RC, cs.getRc());

        String[] args = argsCaptor.getValue();
        assertNotNull(args);
        assertEquals("/cnb/lifecycle/creator", args[0]);
        assertEquals(new ImageReference(OUTPUT_IMAGE).getReferenceWithLatest(), args[args.length-1]);

        List<String> argList = Arrays.asList(args);
        assertTrue(argList.contains("-launch-cache"));
        assertTrue(argList.contains("-daemon"));
        assertTrue(argList.contains(LOG_LEVEL));

        verify(logCmd).withTimestamps(true);
        verify(dockerClient).logContainerCmd(CONTAINER_ID);
        verify(factory).getContainerForPhase(any(String[].class), eq(0));
    }    
}
