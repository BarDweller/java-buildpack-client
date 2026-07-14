package dev.snowdrop.buildpack.utils;

public enum OperatingSystem {

  WIN,
  LINUX,
  MAC,
  UNKNOWN;

  private static OperatingSystem os;

  public static OperatingSystem getOperatingSystem() {
    if (os == null) {
      String osName = System.getProperty("os.name").toLowerCase();
      if (osName.contains("win")) {
        os = WIN;
      } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
        os = LINUX;
      } else if (osName.contains("mac")) {
        os = MAC;
      } else {
        os = UNKNOWN;
      }
    }
    return os;
  }
}
