package dev.snowdrop.buildpack;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;

@ExtendWith(MockitoExtension.class)
public class ContainerLogReaderTest {

  @Mock
  private Logger logger;

  @Test
  void testStdoutFrame() {
    ContainerLogReader reader = new ContainerLogReader(logger);
    byte[] payload = "Hello Stdout".getBytes(StandardCharsets.UTF_8);
    Frame frame = new Frame(StreamType.STDOUT, payload);

    reader.onNext(frame);

    verify(logger).stdout("Hello Stdout");
    verifyNoMoreInteractions(logger); // stdout is the only method called
  }

  @Test
  void testStderrFrame() {
    ContainerLogReader reader = new ContainerLogReader(logger);
    byte[] payload = "Hello Stderr".getBytes(StandardCharsets.UTF_8);
    Frame frame = new Frame(StreamType.STDERR, payload);

    reader.onNext(frame);

    verify(logger).stderr("Hello Stderr");
    verifyNoMoreInteractions(logger); // stderr is the only method called
  }

  @Test
  void testRawFrameIgnored() {
    ContainerLogReader reader = new ContainerLogReader(logger);
    byte[] payload = "Raw bytes".getBytes(StandardCharsets.UTF_8);
    Frame frame = new Frame(StreamType.RAW, payload);

    reader.onNext(frame);

    verifyNoInteractions(logger);
  }
}
