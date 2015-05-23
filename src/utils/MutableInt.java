/**
 * 
 */

package utils;

/**
 * @author jantunes
 */
public class MutableInt {
  private int val;

  public MutableInt() {
    this(0);
  }

  public MutableInt(int val) {
    this.val = val;
  }

  public int getValue() {
    return this.val;
  }

  public void setValue(int val) {
    this.val = val;
  }

  public MutableInt inc() {
    this.val++;
    return this;
  }

  public MutableInt dec() {
    this.val--;
    return this;
  }

}
