package dev.snowdrop.buildpack.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.github.dockerjava.api.DockerClient;

import dev.snowdrop.buildpack.BuildpackException;
import dev.snowdrop.buildpack.config.DockerConfig;
import dev.snowdrop.buildpack.config.ImageReference;
import dev.snowdrop.buildpack.docker.ImageUtils;
import dev.snowdrop.buildpack.docker.ImageUtils.ImageInfo;

public class LifecycleMetadataTest {

    @Test
    void testLifecycleMetadataParsingSuccess() throws BuildpackException {
        DockerConfig dc = mock(DockerConfig.class);
        DockerClient dockerClient = mock(DockerClient.class);
        when(dc.getDockerClient()).thenReturn(dockerClient);

        ImageReference lifecycleImage = new ImageReference("lifecycle-image");

        ImageInfo mockImageInfo = new ImageInfo();
        mockImageInfo.labels = new HashMap<>();
        mockImageInfo.labels.put("io.buildpacks.lifecycle.apis", 
            "{\"platform\":{\"supported\":[\"0.7\",\"0.9\",\"0.10\"]},\"buildpack\":{\"supported\":[\"0.6\",\"0.7\"]}}");

        try (MockedStatic<ImageUtils> imageUtils = mockStatic(ImageUtils.class)) {
            imageUtils.when(() -> ImageUtils.inspectImage(any(), any())).thenReturn(mockImageInfo);
            imageUtils.when(() -> ImageUtils.pullImages(any(), any())).thenAnswer(invocation -> null);

            LifecycleMetadata metadata = new LifecycleMetadata(dc, lifecycleImage);

            assertNotNull(metadata.getSupportedPlatformLevels());
            assertEquals(3, metadata.getSupportedPlatformLevels().size());
            assertEquals("0.7", metadata.getSupportedPlatformLevels().get(0));
            assertEquals("0.9", metadata.getSupportedPlatformLevels().get(1));
            assertEquals("0.10", metadata.getSupportedPlatformLevels().get(2));

            assertNotNull(metadata.getSupportedBuildpackLevels());
            assertEquals(2, metadata.getSupportedBuildpackLevels().size());
            assertEquals("0.6", metadata.getSupportedBuildpackLevels().get(0));
            assertEquals("0.7", metadata.getSupportedBuildpackLevels().get(1));
        }
    }

    @Test
    void testLifecycleMetadataParsingFailure() {
        DockerConfig dc = mock(DockerConfig.class);
        DockerClient dockerClient = mock(DockerClient.class);
        when(dc.getDockerClient()).thenReturn(dockerClient);

        ImageReference lifecycleImage = new ImageReference("lifecycle-image");

        ImageInfo mockImageInfo = new ImageInfo();
        mockImageInfo.labels = new HashMap<>();
        // Bad JSON
        mockImageInfo.labels.put("io.buildpacks.lifecycle.apis", "{invalid-json}");

        try (MockedStatic<ImageUtils> imageUtils = mockStatic(ImageUtils.class)) {
            imageUtils.when(() -> ImageUtils.inspectImage(any(), any())).thenReturn(mockImageInfo);
            imageUtils.when(() -> ImageUtils.pullImages(any(), any())).thenAnswer(invocation -> null);

            assertThrows(BuildpackException.class, () -> {
                new LifecycleMetadata(dc, lifecycleImage);
            });
        }
    }
}
