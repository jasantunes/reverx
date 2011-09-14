
package automata;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import traces.ByteChars;

public class RegExTest {

  @Test
  public void testConcat() {
    RegEx r0 = new RegEx(new ByteChars("expression1".getBytes()));
    RegEx r1 = new RegEx(new ByteChars("andtwo".getBytes()));
    r0 = RegEx.concat(r0, r1);
    assertTrue(r0.accepts("expression1andtwo"));
    assertFalse(r0.accepts("expression1"));
    assertFalse(r0.accepts("andtwo"));
    assertFalse(r0.accepts("."));

    RegEx r2 = RegEx.concat(new RegEx(new ByteChars("quoted ".getBytes())), new RegEx(
        new ByteChars("\"text\"".getBytes())));
    assertFalse(r2.accepts("anything"));
    assertTrue(r2.accepts("quoted \"text\""));
    assertFalse(r2.accepts("quoted text"));
    assertFalse(r2.accepts("."));

  }

  @Test
  public void testAccepts() {
    RegEx r0 = new RegEx(new ByteChars("ola\neu".getBytes()));
    assertTrue(r0.accepts("ola\neu"));
    assertFalse(r0.accepts("olaeu"));
    assertFalse(r0.accepts("ola\n"));

    RegEx r1 = new RegEx(".*");
    assertTrue(r1.accepts("anything"));
    assertTrue(r1.accepts(""));
    assertTrue(r1.accepts("."));

    RegEx r2 = new RegEx(new ByteChars("quoted \"text\"".getBytes()));
    assertFalse(r2.accepts("anything"));
    assertTrue(r2.accepts("quoted \"text\""));
    assertFalse(r2.accepts("quoted text"));
    assertFalse(r2.accepts("."));
  }

  @Test
  public void testNullCharacter() {
    byte[] empty = new byte[] {
      0
    };
    RegEx r0 = new RegEx(new ByteChars(empty));

    String s = new String(empty);
    assertTrue(r0.accepts(s));
  }

}
