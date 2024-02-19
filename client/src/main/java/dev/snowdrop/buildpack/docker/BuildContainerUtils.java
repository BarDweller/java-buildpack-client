package dev.snowdrop.buildpack.docker;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;

import dev.snowdrop.buildpack.BuilderImage;
import dev.snowdrop.buildpack.BuildpackException;
import dev.snowdrop.buildpack.config.ImageReference;
import dev.snowdrop.buildpack.lifecycle.LifecyclePhaseFactory;

//used to create ephemeral builder, streaming tarball content from one image to another.
public class BuildContainerUtils {

    private static InputStream getArchiveStreamFromContainer(DockerClient dc, String containerId, String path){
        CopyArchiveFromContainerCmd copyLifecyleFromImageCmd = dc.copyArchiveFromContainerCmd(containerId, path);
        try{
          return copyLifecyleFromImageCmd.exec();
        }catch(NotFoundException nfe){
            throw BuildpackException.launderThrowable("Unable to locate container '"+containerId+"'", nfe);
        }
    }

    private static void putArchiveStreamToContainer(DockerClient dc, String containerId, String atPath, InputStream tarStream){
        dc.copyArchiveToContainerCmd(containerId).withTarInputStream(tarStream).withRemotePath(atPath).exec();
    }

    private static void processBuildModule(DockerClient dc, String targetContainerId, String moduleImageReference, String fromPath, String toPath){
        String containerId = null;
        List<String> command = Stream.of("").collect(Collectors.toList());
        try{
            containerId = ContainerUtils.createContainer(dc, moduleImageReference, command);
            InputStream stream = getArchiveStreamFromContainer(dc, containerId, fromPath);
            putArchiveStreamToContainer(dc, targetContainerId, toPath, stream);
        }finally{
            if(containerId!=null){
                ContainerUtils.removeContainer(dc, containerId);
            }
        }
    }

    private static void populateMountPointDirs(DockerClient dc, String targetContainerId, int uid, int gid, List<String> dirs){
        try (PipedInputStream in = new PipedInputStream(4096); PipedOutputStream out = new PipedOutputStream(in)) {
            AtomicReference<Exception> writerException = new AtomicReference<>();

            Runnable writer = new Runnable() {
                @Override
                public void run() {
                    try (TarArchiveOutputStream tout = new TarArchiveOutputStream(new GZIPOutputStream(new BufferedOutputStream(out)));) {
                        tout.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
                        for (String dir : dirs) {
                            TarArchiveEntry tae = new TarArchiveEntry(dir + "/");
                            tae.setSize(0);
                            tae.setUserId(uid);
                            tae.setGroupId(gid);
                            tout.putArchiveEntry(tae);
                            tout.closeArchiveEntry();
                        } 
                    } catch (Exception e) {
                        writerException.set(e);
                    }
                } 
            };

            Runnable reader = new Runnable() {
                @Override
                public void run() {
                dc.copyArchiveToContainerCmd(targetContainerId).withRemotePath("/").withTarInputStream(in).exec();
                }
            };

            Thread t1 = new Thread(writer);
            Thread t2 = new Thread(reader);
            t1.start();
            t2.start();
            try {
                t1.join();
                t2.join();
            } catch (InterruptedException ie) {
                throw BuildpackException.launderThrowable(ie);
            }

            // did the write thread complete without issues? if not, bubble the cause.
            Exception wio = writerException.get();
            if (wio != null) {
                throw BuildpackException.launderThrowable(wio);
            }
        } catch (IOException e) {
            throw BuildpackException.launderThrowable(e);
        }
    }


    /**
     * Creates a build image using a builder image as the base, overlaying lifecycle/extensions/buildpack content 
     * from supplied images. 
     * 
     * @param dc          Dockerclient to use
     * @param baseBuilder ImageReference for builder imager to start with
     * @param lifecycle   ImageReference for lifecycle image to take lifecycle from
     * @param extensions  List of ImageReferences to take extensions from
     * @param buildpacks  List of ImageReferences to take buildpacks from
     * @return
     */
    public static BuilderImage createBuildImage(DockerClient dc, BuilderImage baseBuilder, ImageReference lifecycle, List<ImageReference> extensions, List<ImageReference> buildpacks) {

        List<String> command = Stream.of("").collect(Collectors.toList());
        String builderContainerId = ContainerUtils.createContainer(dc, baseBuilder.getImage().getReference(), command);        

        if(lifecycle!=null)
            processBuildModule(dc, builderContainerId, lifecycle.getReference(), "/cnb/lifecycle", "/cnb");

        if(extensions!=null)
            for(ImageReference extension: extensions)
                processBuildModule(dc, builderContainerId, extension.getReference(), "/cnb/extensions", "/cnb");
        
        if(buildpacks!=null)
            for(ImageReference buildpack: buildpacks)
                processBuildModule(dc, builderContainerId, buildpack.getReference(), "/cnb/buildpacks", "/cnb");

        populateMountPointDirs(dc, builderContainerId, baseBuilder.getUserId(), baseBuilder.getGroupId(), 
                               Stream.of(LifecyclePhaseFactory.KANIKO_VOL_PATH,
                                         LifecyclePhaseFactory.WORKSPACE_VOL_PATH,
                                         LifecyclePhaseFactory.LAYERS_VOL_PATH,
                                         LifecyclePhaseFactory.CACHE_VOL_PATH,
                                         LifecyclePhaseFactory.LAUNCH_CACHE_VOL_PATH,
                                         LifecyclePhaseFactory.PLATFORM_VOL_PATH,
                                         LifecyclePhaseFactory.PLATFORM_VOL_PATH+LifecyclePhaseFactory.ENV_PATH_PREFIX)
                                      .collect(Collectors.toList()));                

        return new BuilderImage(baseBuilder,
                                (extensions!=null && !extensions.isEmpty()), 
                                new ImageReference(ContainerUtils.commitContainer(dc, builderContainerId)));
    }
}
