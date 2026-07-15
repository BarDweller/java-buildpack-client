package dev.snowdrop.buildpack.lifecycle.phases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
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
import org.mockito.MockedStatic;
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
import dev.snowdrop.buildpack.docker.ContainerUtils;
import dev.snowdrop.buildpack.lifecycle.ContainerStatus;
import dev.snowdrop.buildpack.lifecycle.LifecyclePhaseFactory;
import dev.snowdrop.buildpack.lifecycle.Version;

@ExtendWith(MockitoExtension.class)
public class DetectorTest {

    private static final String LOG_LEVEL = "debug";
    private static final String CONTAINER_ID = "999";
    private static final int CONTAINER_RC = 99;
    private static final int USER_ID = 77;
    private static final int GROUP_ID = 88;
    private static final String OUTPUT_IMAGE = "stiletto";
    private static final boolean USE_DAEMON = false;
    private static final boolean HAS_EXTENSIONS = true;

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
        lenient().when(builder.hasExtensions()).thenReturn(HAS_EXTENSIONS);
        lenient().when(factory.getBuilderImage()).thenReturn(builder);

        lenient().when(factory.getContainerForPhase(argsCaptor.capture(), any())).thenReturn(CONTAINER_ID);

        lenient().when(factory.getOutputImage()).thenReturn(new ImageReference(OUTPUT_IMAGE));
    }

    @SuppressWarnings("resource")
    @Test
    void testPre10() {
        lenient().when(builder.hasExtensions()).thenReturn(false);
        lenient().when(factory.getPlatformLevel()).thenReturn(new Version("0.7"));

        try (MockedStatic<? extends ContainerUtils> containerUtils = mockStatic(ContainerUtils.class)) {
            Detector d = new Detector(factory);
            containerUtils.when(() -> ContainerUtils.getFileFromContainer(eq(dockerClient), any(), any())).thenReturn("EmptyTOML".getBytes());

            ContainerStatus cs = d.runPhase(logger, true);

            assertNotNull(cs);
            assertEquals(CONTAINER_ID, cs.getContainerId());
            assertEquals(CONTAINER_RC, cs.getRc());
            assertEquals("EmptyTOML", new String(d.getAnalyzedToml()));
        }

        String[] args = argsCaptor.getValue();
        assertNotNull(args);
        assertEquals("/cnb/lifecycle/detector", args[0]);
        assertNotEquals(args[args.length-1], OUTPUT_IMAGE);

        List<String> argList = Arrays.asList(args);
        assertFalse(argList.contains("-generated"));
        assertFalse(argList.contains("-daemon"));
        assertTrue(argList.contains(LOG_LEVEL));

        verify(logCmd).withTimestamps(true);
        verify(dockerClient).logContainerCmd(CONTAINER_ID);
        verify(factory).getContainerForPhase(any(String[].class), eq(USER_ID));
    }

    @SuppressWarnings("resource")
    @Test
    void testPre10WithExtensions() {
        lenient().when(factory.getPlatformLevel()).thenReturn(new Version("0.7"));

        try (MockedStatic<? extends ContainerUtils> containerUtils = mockStatic(ContainerUtils.class)) {
            Detector d = new Detector(factory);
            containerUtils.when(() -> ContainerUtils.getFileFromContainer(eq(dockerClient), any(), any())).thenReturn("EmptyTOML".getBytes());

            ContainerStatus cs = d.runPhase(logger, true);

            assertNotNull(cs);
            assertEquals(CONTAINER_ID, cs.getContainerId());
            assertEquals(CONTAINER_RC, cs.getRc());
            assertEquals("EmptyTOML", new String(d.getAnalyzedToml()));
        }

        String[] args = argsCaptor.getValue();
        assertNotNull(args);
        assertEquals("/cnb/lifecycle/detector", args[0]);
        assertNotEquals(args[args.length-1], OUTPUT_IMAGE);

        List<String> argList = Arrays.asList(args);
        assertFalse(argList.contains("-generated"));
        assertFalse(argList.contains("-daemon"));
        assertTrue(argList.contains(LOG_LEVEL));

        verify(logCmd).withTimestamps(true);
        verify(dockerClient).logContainerCmd(CONTAINER_ID);
        verify(factory).getContainerForPhase(any(String[].class), eq(USER_ID));
    } 
    
    @SuppressWarnings("resource")
    @Test
    void test10WithExtensions() {
        lenient().when(factory.getPlatformLevel()).thenReturn(new Version("0.10"));

        try (MockedStatic<? extends ContainerUtils> containerUtils = mockStatic(ContainerUtils.class)) {
            Detector d = new Detector(factory);
            containerUtils.when(() -> ContainerUtils.getFileFromContainer(eq(dockerClient), any(), any())).thenReturn("EmptyTOML".getBytes());

            ContainerStatus cs = d.runPhase(logger, true);

            assertNotNull(cs);
            assertEquals(CONTAINER_ID, cs.getContainerId());
            assertEquals(CONTAINER_RC, cs.getRc());
            assertEquals("EmptyTOML", new String(d.getAnalyzedToml()));
        }

        String[] args = argsCaptor.getValue();
        assertNotNull(args);
        assertEquals("/cnb/lifecycle/detector", args[0]);
        assertNotEquals(args[args.length-1], OUTPUT_IMAGE);

        List<String> argList = Arrays.asList(args);
        assertTrue(argList.contains("-generated"));
        assertFalse(argList.contains("-daemon"));
        assertTrue(argList.contains(LOG_LEVEL));

        verify(logCmd).withTimestamps(true);
        verify(dockerClient).logContainerCmd(CONTAINER_ID);
        verify(factory).getContainerForPhase(any(String[].class), eq(USER_ID));
    }    

    @SuppressWarnings("resource")
    @Test
    void test12WithExtensions() {
        lenient().when(factory.getPlatformLevel()).thenReturn(new Version("0.12"));

        try (MockedStatic<? extends ContainerUtils> containerUtils = mockStatic(ContainerUtils.class)) {
            Detector d = new Detector(factory);
            containerUtils.when(() -> ContainerUtils.getFileFromContainer(eq(dockerClient), any(), any())).thenReturn("EmptyTOML".getBytes());

            ContainerStatus cs = d.runPhase(logger, true);

            assertNotNull(cs);
            assertEquals(CONTAINER_ID, cs.getContainerId());
            assertEquals(CONTAINER_RC, cs.getRc());
            assertEquals("EmptyTOML", new String(d.getAnalyzedToml()));
        }

        String[] args = argsCaptor.getValue();
        assertNotNull(args);
        assertEquals("/cnb/lifecycle/detector", args[0]);
        assertNotEquals(args[args.length-1], OUTPUT_IMAGE);

        List<String> argList = Arrays.asList(args);
        assertTrue(argList.contains("-generated"));
        assertFalse(argList.contains("-daemon"));
        assertTrue(argList.contains(LOG_LEVEL));
        assertTrue(argList.contains("-run"));
        assertTrue(argList.contains("/cnb/run.toml"));

        verify(logCmd).withTimestamps(true);
        verify(dockerClient).logContainerCmd(CONTAINER_ID);
        verify(factory).getContainerForPhase(any(String[].class), eq(USER_ID));
    }  

    @SuppressWarnings("resource")
    @Test
    void test10WithoutExtensions() {
        lenient().when(builder.hasExtensions()).thenReturn(false);
        lenient().when(factory.getPlatformLevel()).thenReturn(new Version("0.10"));

        try (MockedStatic<? extends ContainerUtils> containerUtils = mockStatic(ContainerUtils.class)) {
            Detector d = new Detector(factory);
            containerUtils.when(() -> ContainerUtils.getFileFromContainer(eq(dockerClient), any(), any())).thenReturn("EmptyTOML".getBytes());

            ContainerStatus cs = d.runPhase(logger, true);

            assertNotNull(cs);
            assertEquals(CONTAINER_ID, cs.getContainerId());
            assertEquals(CONTAINER_RC, cs.getRc());
            assertEquals("EmptyTOML", new String(d.getAnalyzedToml()));
        }

        String[] args = argsCaptor.getValue();
        assertNotNull(args);
        assertEquals("/cnb/lifecycle/detector", args[0]);
        assertNotEquals(args[args.length-1], OUTPUT_IMAGE);

        List<String> argList = Arrays.asList(args);
        assertFalse(argList.contains("-generated"));
        assertFalse(argList.contains("-daemon"));
        assertTrue(argList.contains(LOG_LEVEL));

        verify(logCmd).withTimestamps(true);
        verify(dockerClient).logContainerCmd(CONTAINER_ID);
        verify(factory).getContainerForPhase(any(String[].class), eq(USER_ID));
    }      

    @SuppressWarnings("resource")
    @Test
    void testDisableTimestamps() {
        lenient().when(factory.getPlatformLevel()).thenReturn(new Version("0.10"));

        try (MockedStatic<? extends ContainerUtils> containerUtils = mockStatic(ContainerUtils.class)) {
            Detector d = new Detector(factory);
            containerUtils.when(() -> ContainerUtils.getFileFromContainer(eq(dockerClient), any(), any())).thenReturn("EmptyTOML".getBytes());

            ContainerStatus cs = d.runPhase(logger, false);

            assertNotNull(cs);
            assertEquals(CONTAINER_ID, cs.getContainerId());
            assertEquals(CONTAINER_RC, cs.getRc());
            assertEquals("EmptyTOML", new String(d.getAnalyzedToml()));
        }

        String[] args = argsCaptor.getValue();
        assertNotNull(args);
        assertEquals("/cnb/lifecycle/detector", args[0]);
        assertNotEquals(args[args.length-1], OUTPUT_IMAGE);

        List<String> argList = Arrays.asList(args);
        assertTrue(argList.contains("-generated"));
        assertFalse(argList.contains("-daemon"));
        assertTrue(argList.contains(LOG_LEVEL));

        verify(logCmd).withTimestamps(false);
        verify(dockerClient).logContainerCmd(CONTAINER_ID);
        verify(factory).getContainerForPhase(any(String[].class), eq(USER_ID));
    }  
}
