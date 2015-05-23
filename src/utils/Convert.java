/*******************************************************************************
 * Copyright 2011 Joao Antunes
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package utils;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class Convert {

  /**
   * Copies sizeOfInt bytes of an integer into a byte array.
   * 
   * @param converted Previously allocated byte array that will hold the integer
   *          value.
   * @param beginIndex First position in converted array to start copy to
   *          (inclusive).
   * @param sizeOfInt Number of the relevant bytes of the integer value to copy.
   * @param integer Number to convert.
   */
  public static void toByteInPlace(byte[] converted, int beginIndex, int sizeOfInt, int integer) {
    int byte_pos = (sizeOfInt - 1) * 8;
    int max_length = (sizeOfInt > 4) ? 4 : sizeOfInt;

    for (int i = 0; i < max_length; i++)
      converted[beginIndex + i] = (byte)((integer << i * 8) >> byte_pos);
  }

  /**
   * Converts an integer to a byte array. Since it calls <b>toByte(byte[]
   * converted, int from, int to, int integer)</b> you might want to use that
   * instead (i.e. if you already have an allocated byte array).
   * 
   * @param integer number to convert.
   * @param sizeOfInt number of bytes of the byte array.
   * @return return the byte array with the converted number.
   */
  public static byte[] toByte(int integer, int sizeOfInt) {
    byte[] converted = new byte[sizeOfInt];
    toByteInPlace(converted, 0, sizeOfInt, integer);
    return converted;
  }

  /* Returns an integer with the content of the byte. */
  public static int unsignedByteToInt(byte b) {
    return b & 0xFF;
  }

  /**
   * Converts the first bytes in an array into a 32-bit (signed) integer.
   * 
   * @param bytes Byte array which holds the integer.
   * @return Converted 32 bits in a integer variable.
   */
  public static int toInt32(byte[] bytes) {
    return unsignedByteToInt(bytes[3]) | (unsignedByteToInt(bytes[2]) << 8)
        | (unsignedByteToInt(bytes[1]) << 16) | (unsignedByteToInt(bytes[0]) << 24);
  }

  /**
   * Converts a variable byte array to a 32-bit (signed) integer, which may then
   * be downcasted (smaller type).
   * 
   * @param bytes byte array which holds the integer.
   * @param beginIndex position in the byte array, where the integer to be
   *          converted starts (inclusive).
   * @param sizeOfInt size of integer in bytes.
   * @return converted integer.
   */
  public static int toInteger(byte[] bytes, int beginIndex, int sizeOfInt) {
    int endIndex = beginIndex + sizeOfInt;
    int integer = 0;
    for (int i = 0; i < sizeOfInt; i++)
      integer |= unsignedByteToInt(bytes[--endIndex]) << (i * 8);

    return integer;
  }

  public static long toLong(byte[] bytes, int index) {
    int endIndex = index + 8;
    long integer = 0;
    for (int i = 0; i < 8; i++)
      integer |= unsignedByteToInt(bytes[--endIndex]) << (i * 8);

    return integer;
  }

  public static String concatArrayList(ArrayList<String> arraylist) {
    String result = new String();
    for (int i = 0; i < arraylist.size(); i++)
      result += arraylist.get(i);
    return result;
  }

  public static String sanitize(String s) {
    s = s.replaceAll("\\\\n", "\n");
    s = s.replaceAll("\\\\r", "\r");
    s = s.replaceAll("\\\\f", "\f");
    s = s.replaceAll("\\\\t", "\t");
    s = s.replaceAll("\\\\0", "\0");
    return s;
  }

  /**
   * Modified from Bits._byte_to_bin() Converts a byte array to its binary
   * string representation (e.g., 100110010)
   */
  public static String toBinaryString(byte[] data) {
    StringBuffer out = new StringBuffer(data.length);

    for (int i = 0; i < data.length; i++) {
      for (int bit = 8 - 1; bit >= 0; bit--) {
        if ((data[i] & (1 << bit)) != 0)
          out.append('1');
        else
          out.append('0');
        // if (bit % 8 == 0 && i != data.length - 1)
        // out.append(' ');
      }
    }
    return new String(out);
  }

  public static String toDecimalString(float number, int decimals) {
    char[] decs = new char[decimals];
    for (int i = 0; i < decs.length; i++)
      decs[i] = '0';
    String format_str = "0." + new String(decs);
    return (new DecimalFormat(format_str)).format(number);
  }

  public static String toDecimalString(double number, int decimals) {
    char[] decs = new char[decimals];
    for (int i = 0; i < decs.length; i++)
      decs[i] = '0';
    String format_str = "0." + new String(decs);
    return (new DecimalFormat(format_str)).format(number);
  }

  private static final String HEXES = "0123456789ABCDEF";

  public static String toHexadecimalString(byte[] data) {
    if (data == null) {
      return null;
    }
    final StringBuilder hex = new StringBuilder(2 * data.length);
    for (final byte b : data) {
      hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
    }
    return hex.toString();
  }

}
