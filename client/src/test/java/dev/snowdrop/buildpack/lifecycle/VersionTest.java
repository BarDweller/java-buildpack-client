package dev.snowdrop.buildpack.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.snowdrop.buildpack.BuildpackException;

public class VersionTest {

  @Test
  void testValidParsing() {
    Version v1 = new Version("1.2");
    assertEquals(1, v1.major);
    assertEquals(2, v1.minor);
    assertEquals("1.2", v1.toString());

    Version v2 = new Version("0.3.5");
    assertEquals(0, v2.major);
    assertEquals(3, v2.minor);
    assertEquals("0.3.5", v2.toString());

    Version v3 = new Version("10.20.rc1");
    assertEquals(10, v3.major);
    assertEquals(20, v3.minor);
    assertEquals("10.20.rc1", v3.toString());
  }

  @Test
  void testInvalidParsing() {
    assertThrows(BuildpackException.class, () -> new Version("1"));
    assertThrows(BuildpackException.class, () -> new Version("abc"));
    assertThrows(BuildpackException.class, () -> new Version(""));
    assertThrows(NullPointerException.class, () -> new Version(null));
  }

  @Test
  void testEqualsAndHashCode() {
    Version v1 = new Version("1.2");
    Version v2 = new Version("1.2.3");
    Version v3 = new Version("1.3");

    // Standard equals contract
    assertEquals(v1, v1);
    assertEquals(v1, v2);
    assertNotEquals(v1, v3);
    assertNotEquals(v1, null);
    assertNotEquals(v1, "1.2"); // different type

    // String convenience equals
    assertTrue(v1.equals("1.2.3"));
    assertFalse(v1.equals("1.3"));

    // HashCode contract
    assertEquals(v1.hashCode(), v2.hashCode());
    assertNotEquals(v1.hashCode(), v3.hashCode());
  }

  @Test
  void testComparisons() {
    Version v12 = new Version("1.2");
    Version v13 = new Version("1.3");
    Version v22 = new Version("2.2");

    // greaterThan
    assertTrue(v13.greaterThan(v12));
    assertTrue(v22.greaterThan(v13));
    assertFalse(v12.greaterThan(v13));
    assertFalse(v12.greaterThan(v12));

    // greaterThan (String convenience)
    assertTrue(v13.greaterThan("1.2"));
    assertFalse(v12.greaterThan("1.3"));

    // lessThan
    assertTrue(v12.lessThan(v13));
    assertTrue(v13.lessThan(v22));
    assertFalse(v13.lessThan(v12));
    assertFalse(v12.lessThan(v12));

    // lessThan (String convenience)
    assertTrue(v12.lessThan("1.3"));
    assertFalse(v13.lessThan("1.2"));

    // atLeast
    assertTrue(v13.atLeast(v12));
    assertTrue(v12.atLeast(v12));
    assertFalse(v12.atLeast(v13));

    // atLeast (String convenience)
    assertTrue(v13.atLeast("1.2"));
    assertTrue(v12.atLeast("1.2"));
    assertFalse(v12.atLeast("1.3"));

    // atMost
    assertTrue(v12.atMost(v13));
    assertTrue(v12.atMost(v12));
    assertFalse(v13.atMost(v12));

    // atMost (String convenience)
    assertTrue(v12.atMost("1.3"));
    assertTrue(v12.atMost("1.2"));
    assertFalse(v13.atMost("1.2"));
  }

}
