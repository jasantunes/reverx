/*****************************************************************************
 * [Simplified BSD License]
 *
 * Copyright 2011 Joao Antunes. All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED BY JOAO ANTUNES ''AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL JOAO ANTUNES OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of Joao Antunes.
 *****************************************************************************/

package automata;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import traces.ByteChars;
import dot.DotGraph;

public class RegEx implements Symbol, MessageType, java.io.Serializable {
  protected static final long serialVersionUID = Symbol.serialVersionUID;
  private static final int FLAGS = Pattern.DOTALL | Pattern.MULTILINE;

  /* Protocol definitions. */
  private static boolean IS_TEXT_BASED_PROTOCOL = true;
  private static Pattern PATTERN_TOKEN = (IS_TEXT_BASED_PROTOCOL) ?
  // text- or binary-based initial field division
  Pattern.compile(" |\\r\\n", FLAGS) // at every space or CRLF
      : Pattern.compile(".", FLAGS); // at every byte

  private Pattern _pattern;

  public RegEx(Pattern pattern) {
    _pattern = pattern;
  }

  public RegEx(String regex) {
    setPattern(regex);
  }

  public RegEx(CharSequence data) {
    setPattern(quote(data));
    // length_BINARY = data.length();
  }

  public void setPattern(String regex) {
    _pattern = Pattern.compile(regex, FLAGS);
  }

  public void setPattern(Pattern pattern) {
    _pattern = pattern;
  }

  public static void setTextBasedSupport(boolean text_based) {
    IS_TEXT_BASED_PROTOCOL = text_based;
    if (text_based)
      PATTERN_TOKEN = Pattern.compile("\\\\Q(.*?)\\\\E", FLAGS);
    else
      // treat each byte as a single field initially
      PATTERN_TOKEN = Pattern.compile(".", FLAGS);
  }

  public static boolean hasTextBasedSupport() {
    return IS_TEXT_BASED_PROTOCOL;
  }

  // /**
  // * Generalizes the regular expression (TEXT-BASED PROTOCOL VERSION).
  // *
  // * @param over_generalized simplify parameter characterization
  // */
  // public boolean generalize(boolean over_generalized) {
  // if (over_generalized)
  // _pattern = Pattern.compile(".+", FLAGS);
  // else
  // return generalize();
  // return true;
  // }

  public static RegEx concat(RegEx r0, RegEx r1) {
    String concat = r0._pattern.pattern() + r1._pattern.pattern();
    concat = concat.replaceAll("\\\\E\\\\Q", "");
    return new RegEx(concat);
  }

  public String getPattern() {
    return _pattern.pattern();
  }

  public String toString() {
    return _pattern.pattern().replaceAll("\\\\E|\\\\Q", "");
  }

  public String toDot() {
    return DotGraph.sanitize(toString());
  }

  @Override
  public int hashCode() {
    return _pattern.pattern().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof RegEx)
      return _pattern.pattern().equals(((RegEx)obj)._pattern.pattern());
    else
      return super.equals(obj);
  }

  public static List<RegEx> tokenize(CharSequence message, int offset) {
    List<RegEx> tokens = new ArrayList<RegEx>(10);
    Matcher matcher = PATTERN_TOKEN.matcher(message);

    int i = offset, end = 0;
    int start = 0;
    while (matcher.find(i)) {
      start = matcher.start();
      end = matcher.end();
      if (i < start)
        tokens.add(new RegEx(message.subSequence(i, start)));
      tokens.add(new RegEx(message.subSequence(start, end)));
      i = end;
    }
    if (i != message.length())
      tokens.add(new RegEx(message.subSequence(i, message.length())));
    return tokens;
  }

  public boolean accepts(CharSequence data) {
    return match(data, 0) == data.length();
  }

  public int match(CharSequence data, int offset) {
    Matcher m = _pattern.matcher(data);

    if (m.find(offset)) {
      int start = m.start();
      int end = m.end();
      if (start == offset)
        return end - offset;
    }

    // if (m.find() && m.start() == offset) return m.end()-offset;
    return 0;
  }

  /**
   * Pattern.quote(String) has a problem with strings that include 0x0 bytes.
   */
  public static String quote(CharSequence data) {
    StringBuffer sb = new StringBuffer();
    boolean quoting = false;
    for (int i = 0; i < data.length(); i++) {
      char b = data.charAt(i);

      // printable (text)
      if (RegEx.IS_TEXT_BASED_PROTOCOL && ByteChars.isASCIIPrintable((byte)b)) {
        if (!quoting) {
          sb.append("\\Q"); // quote ascii character.
          quoting = true;
        }
        sb.append((char)b); // append character
      }

      // non-printable (hex)
      else {
        if (quoting) {
          sb.append("\\E"); // unquote ascii character
          quoting = false;
        }
        sb.append(escape((byte)b)); // append escaped character in hex unicode
      }
    }
    if (quoting) {
      sb.append("\\E");
    }
    return sb.toString();
  }

  public static String escape(byte b) {
    return "\\x" + String.format("%02X", b);
  }

  @Override
  public Object clone() {
    return new RegEx(_pattern.pattern());
  }

}
