package dev.snowdrop.buildpack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class LoggerTest {

  private static class TestLogger implements Logger {
    private final boolean ansiEnabled;

    TestLogger(boolean ansiEnabled) {
      this.ansiEnabled = ansiEnabled;
    }

    @Override
    public void stdout(String message) {}

    @Override
    public void stderr(String message) {}

    @Override
    public boolean isAnsiColorEnabled() {
      return ansiEnabled;
    }
  }

  @Test
  void testPrepareEndsWithNewline() {
    Logger logger = new TestLogger(true);
    assertEquals("hello", logger.prepare("hello\n"));
    assertEquals("hello", logger.prepare("hello"));
    assertEquals("", logger.prepare("\n"));
  }

  @Test
  void testPrepareAnsiStripping() {
    Logger noAnsi = new TestLogger(false);
    // The current regex in Logger.java is: " [^m]+m"
    assertEquals("hello", noAnsi.prepare("hello\u001b[31m"));
    
    Logger ansi = new TestLogger(true);
    assertEquals("hello\u001b[31m", ansi.prepare("hello\u001b[31m"));
  }

  @Test
  void testDefaultIsAnsiColorEnabled() {
    Logger defaultLogger = new Logger() {
      @Override public void stdout(String message) {}
      @Override public void stderr(String message) {}
    };
    assertFalse(defaultLogger.isAnsiColorEnabled());
  }

}
