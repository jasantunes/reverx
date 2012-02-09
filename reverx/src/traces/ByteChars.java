/*****************************************************************************
 * [Simplified BSD License]
 *
 * Copyright 2011 João Antunes. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JOÃO ANTUNES ''AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL JOÃO ANTUNES OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of João Antunes.
 *****************************************************************************/

package traces;

import automata.RegEx;

public class ByteChars implements CharSequence, java.io.Serializable {
  private static final long serialVersionUID = 1L;

  protected byte[] buf; // reuse the same buffer, just change offset+length
  protected int off = 0;// offset
  protected int len = 0; // length

  public ByteChars() {
    this(new byte[0]);
  }

  public ByteChars(byte[] arr) {
    this(arr, 0, arr.length);
  }

  public ByteChars(byte[] arr, int offset, int length) {
    off = offset;
    len = length;
    buf = arr;
  }

  public char charAt(int idx) {
    return (char)(buf[off + idx] & 0xFF);
  }

  public int length() {
    return len;
  }

  public byte[] getByteArray() {
    if (off == 0 && len == buf.length)
      return buf;
    else {
      byte[] array = new byte[len];
      System.arraycopy(buf, off, array, 0, len);
      return array;
    }
  }

  /**
   * Creates a ByteChars (CharSequence) with the same buffer but with updated
   * offset and length.
   */
  public CharSequence subSequence(int start_index, int end_index) {
    return new ByteChars(buf, this.off + start_index, end_index - start_index);
  }

  public String toString() {
    StringBuffer sb = new StringBuffer(buf.length * 3);
    for (int i = off; i < off + len; i++) {
      if (isASCIIPrintable(buf[i]))
        sb.append((char)buf[i]);
      else
        sb.append(RegEx.escape(buf[i]));
    }
    return sb.toString();
  }

  public static boolean isASCIIPrintable(byte b) {
    return (b >= 32 && b <= 126);
  }

  public static boolean isASCIIAlpha(byte b) {
    return ((b >= 65 && b <= 90) || (b >= 97 && b <= 122));
  }

  public static boolean isASCIIDigit(byte b) {
    return (b >= 48 && b <= 57);
  }

  // /*
  // * We use ISO-8859-1 because it maps every byte in the range 0x00..0xFF to a
  // * valid character and each of those characters has the same numeric value
  // * as its latin1 encoding.
  // */
  // private static final String CHARSET = "ISO-8859-1";
  //
  // public String toString2() {
  // try {
  // return new String(buf, off, len, CHARSET).replaceAll("\r|\n", "\\\\n");
  // } catch (UnsupportedEncodingException e) {
  // e.printStackTrace();
  // }
  // return null;
  // }

  @Override
  public int hashCode() {
    return len;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ByteChars) {
      ByteChars chars = (ByteChars)obj;
      if (this.len != chars.len)
        return false;
      for (int i = off, j = chars.off; i < len; i++, j++)
        if (buf[i] != chars.buf[j])
          return false;
      return true;
    }
    return super.equals(obj);
  }

  public static ByteChars concat(ByteChars arg0, ByteChars arg1) {
    byte[] data = new byte[arg0.length() + arg1.length()];
    System.arraycopy(arg0.buf, arg0.off, data, 0, arg0.len);
    System.arraycopy(arg1.buf, arg1.off, data, arg0.len, arg1.len);
    return new ByteChars(data);
  }

}
