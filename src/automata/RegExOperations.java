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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import traces.ByteChars;

public class RegExOperations {
  private static final int FLAGS = Pattern.DOTALL | Pattern.MULTILINE;

  public static interface RegularExpressionInterface {
    public BracketedExpression generalize();

    public byte[] getBytes();
  }

  private static class RegularExpression implements RegularExpressionInterface {
    protected byte data[];

    public RegularExpression(String text) {
      this.data = text.getBytes();
    }

    public RegularExpression() {
      this.data = new byte[0];
    }

    @Override
    public String toString() {
      return new String(data);
    }

    @Override
    public BracketedExpression generalize() {
      return BracketedExpression.generalize(data);
    }

    @Override
    public byte[] getBytes() {
      return data;
    }
  }

  // private static class Division implements RegularExpressionInterface {
  // private static final String PATTERN =
  // "\\\\Q \\\\E| |\\r\\n|\\\\x0D\\\\x0A";
  // private static final Pattern PATTERN_ = Pattern.compile("^" + PATTERN +
  // "$", FLAGS);
  // private RegularExpression division;
  //
  // public Division(String text) {
  // division = new RegularExpression(text);
  // }
  //
  // @Override
  // public BracketedExpression generalize() {
  // return division.generalize();
  // }
  //
  // @Override
  // public String toString() {
  // return division.toString();
  // }
  //
  // }

  private static class QuotedExpression extends RegularExpression {
    private static final String PATTERN = "\\\\Q(.*?)\\\\E";
    private static final Pattern PATTERN_ = Pattern.compile("^" + PATTERN + "$", FLAGS);

    public QuotedExpression(String text) {
      Matcher matcher = PATTERN_.matcher(text);
      if (matcher.find())
        data = matcher.group(1).getBytes();
      else
        data = new byte[0];
    }

    @Override
    public String toString() {
      return "\\Q" + new String(data) + "\\E";
    }
  }

  private static class EscapedChar extends RegularExpression {
    private static final String PATTERN = "\\\\x[0-9A-Fa-f]{2}";
    private static final Pattern PATTERN_ = Pattern.compile("^" + PATTERN + "$", FLAGS);

    public EscapedChar(String text) {
      data = new byte[1];
      // text = "\x99"
      data[0] = (byte)Integer.parseInt(text.substring(2), 16);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return escape(data[0]);
    }

    public static String escape(byte b) {
      return "\\x" + String.format("%02X", b);
    }
  }

  public static class BracketedExpression implements RegularExpressionInterface {
    private static final String PATTERN = "\\[(.*?)\\]([+]{0,1})";
    private static final Pattern PATTERN_ = Pattern.compile("^" + PATTERN + "$", FLAGS);
    private static final Pattern PATTERN_SEARCH_FOR_QUOTED = Pattern.compile(
        QuotedExpression.PATTERN, FLAGS);
    private static final Pattern PATTERN_SEARCH_FOR_ESCAPED = Pattern.compile(EscapedChar.PATTERN,
        FLAGS);
    boolean any_nonwhitespace_char = false;
    boolean alpha;
    boolean digit;
    Set<Byte> other_chars;
    // boolean zero;
    boolean many;

    private static boolean hasRange(StringBuffer regular_expression, String range) {
      int first_index = regular_expression.indexOf(range);
      if (first_index >= 0) {
        regular_expression.replace(first_index, range.length(), "");
        return true;
      }
      return false;
    }

    public BracketedExpression() {
      any_nonwhitespace_char = false;
      alpha = false;
      digit = false;
      other_chars = new HashSet<Byte>();
      // zero = false;
      many = false;
    }

    /**
     * True if alpha and if there are other characters (all printable).
     */
    private boolean isElligableForNonWhitespaceChars() {
      if (this.many && this.alpha && !other_chars.isEmpty()) {
        for (byte b : other_chars) {
          if (b == (char)' ' || !ByteChars.isASCIIPrintable(b))
            return false;
          return true;
        }
      }
      return false;
    }

    public static BracketedExpression generalize(byte data[]) {
      BracketedExpression re = new BracketedExpression();
      for (byte b : data) {
        if (ByteChars.isASCIIAlpha(b))
          re.alpha = true;
        else if (ByteChars.isASCIIDigit(b))
          re.digit = true;
        else
          re.other_chars.add(b);
      }
      if (data.length > 1) {
        // re.zero = false;
        re.many = true;
      }

      if (re.isElligableForNonWhitespaceChars())
        re.any_nonwhitespace_char = true;
      return re;
    }

    public void addByte(byte b) {
      if (ByteChars.isASCIIAlpha(b))
        alpha = true;
      else if (ByteChars.isASCIIDigit(b))
        digit = true;
      else
        other_chars.add(b);
    }

    public BracketedExpression(String text) {
      other_chars = new HashSet<Byte>();
      Matcher matcher = PATTERN_.matcher(text);
      if (matcher.find()) {
        StringBuffer content = new StringBuffer(matcher.group(1));
        String qty_str = matcher.group(2);
        if (qty_str.equals("+")) {
          // zero = false;
          many = true;
          // } else if (qty_str.equals("*")) {
          // zero = true;
          // many = true;
        } else {
          // zero = false;
          many = false;
        }

        alpha = hasRange(content, "A-Z");
        alpha |= hasRange(content, "a-z");
        digit = hasRange(content, "0-9");

        /* strip all quoted expressions */
        matcher = PATTERN_SEARCH_FOR_QUOTED.matcher(content);
        StringBuffer remaining = new StringBuffer(content.length());
        int i = 0, start = 0, end = 0;
        while (matcher.find()) {
          start = matcher.start();
          end = matcher.end();
          if (i != start) // before match
            remaining.append(content.substring(i, start));
          // match
          byte data[] = new QuotedExpression(content.substring(start, end)).data;
          for (byte b : data)
            addByte(b);
          i = end;
        }
        if (i != content.length()) {
          // after last match
          remaining.append(content.substring(i, content.length()));
        }

        /* strip escaped chars */
        content = remaining;
        matcher = PATTERN_SEARCH_FOR_ESCAPED.matcher(content);
        remaining = new StringBuffer(content.length());
        i = 0;
        start = 0;
        end = 0;
        while (matcher.find()) {
          start = matcher.start();
          end = matcher.end();
          if (i != start) // before match
            remaining.append(content.substring(i, start));
          // match
          addByte(new EscapedChar(content.substring(start, end)).data[0]);
          i = end;
        }
        if (i != content.length()) {
          // after last match
          remaining.append(content.substring(i, content.length()));
        }

        /* convert remaining chars to byte array */
        for (byte b : remaining.toString().getBytes()) {
          addByte(b);
        }

        /* check if we can simplify this by generalizing to any text char */
        if (isElligableForNonWhitespaceChars())
          any_nonwhitespace_char = true;
      }
    }

    @Override
    public String toString() {
      if (any_nonwhitespace_char)
        return "\\S+";

      if (many && alpha && digit && other_chars.isEmpty())
        return "\\w+";

      StringBuffer sb = new StringBuffer(12 + 5 * other_chars.size());
      sb.append('[');

      if (alpha) {
        sb.append("A-Za-z");
        /* uncomment to generalize more (alpha->alphanumeric) */
        // digit = true;
      }
      if (digit)
        sb.append("0-9");

      for (byte b : other_chars) {
        if (ByteChars.isASCIIPrintable(b)) {
          if (ByteChars.isASCIIAlpha(b) | ByteChars.isASCIIDigit(b))
            sb.append((char)b);
          else
            sb.append("\\Q" + (char)b + "\\E");
        } else
          sb.append(EscapedChar.escape(b));
      }

      sb.append("]");
      if (this.many) {
        // if (this.zero)
        // sb.append("*");
        // else
        sb.append("+");
      }
      return sb.toString();

    }

    /**
     * Merges two regular expressions.
     */
    public static BracketedExpression merge(BracketedExpression re0, BracketedExpression re1) {
      BracketedExpression merged = new BracketedExpression();
      merged.any_nonwhitespace_char = re0.any_nonwhitespace_char || re1.any_nonwhitespace_char;
      merged.alpha = re0.alpha || re1.alpha;
      merged.digit = re0.digit || re1.digit;
      merged.other_chars.addAll(re0.other_chars);
      merged.other_chars.addAll(re1.other_chars);
      // merged.zero = re0.zero || re1.zero;
      merged.many = re0.many || re1.many;
      return merged;
    }

    @Override
    public BracketedExpression generalize() {
      /* check if we can simplify this by generalizing to any text char */
      if (!any_nonwhitespace_char && isElligableForNonWhitespaceChars())
        any_nonwhitespace_char = true;
      return this;
    }

    /**
     * Because BracketedExpression is a generalization, we return null!
     */
    @Override
    public byte[] getBytes() {
      return null;
    }

  }

  public static class CharacterClassExpression extends BracketedExpression {
    private static final String PATTERN = "\\\\([Sw]\\+)";
    private static final Pattern PATTERN_ = Pattern.compile("^" + PATTERN + "$", FLAGS);

    public CharacterClassExpression(String text) {
      Matcher matcher = PATTERN_.matcher(text);
      if (matcher.find()) {
        String char_class = matcher.group(1);

        // w: A word character: [a-zA-Z_0-9]
        if (char_class.equals("w")) {
          alpha = true;
          digit = true;
          any_nonwhitespace_char = false;
          many = true;
        }

        // S: non-whitespace character
        // any except: [ \t\n\x0B\f\r]
        else {
          alpha = true;
          digit = true;
          any_nonwhitespace_char = true;
          many = true;
        }

      }
    }

  }

  /*********************************************************************************/

  private static final Pattern PATTERN_MATCH = Pattern.compile(
  // Division.PATTERN + "|" +
      QuotedExpression.PATTERN + "|" + EscapedChar.PATTERN + "|" + BracketedExpression.PATTERN
          + "|" + CharacterClassExpression.PATTERN, FLAGS);

  public static List<RegularExpressionInterface> process(String regular_expression) {
    Matcher matcher = PATTERN_MATCH.matcher(regular_expression);
    ArrayList<RegularExpressionInterface> tokens = new ArrayList<RegularExpressionInterface>();

    int i = 0, end = 0;
    int start = 0;
    RegularExpressionInterface re = null;
    while (matcher.find()) {
      start = matcher.start();
      end = matcher.end();

      if (i != start)
        tokens.add(new RegularExpression(regular_expression.substring(i, start)));

      /* See what type of token is this. */
      String substr = regular_expression.substring(start, end);

      // if (Division.PATTERN_.matcher(substr).matches()) {
      // System.out.println("SEP: " + substr);
      // re = new Division(substr);
      // }

      if (QuotedExpression.PATTERN_.matcher(substr).matches()) {
        // System.out.println("\\Q: " + substr);
        re = new QuotedExpression(substr);
      }

      else if (BracketedExpression.PATTERN_.matcher(substr).matches()) {
        // System.out.println("[: " + substr);
        re = new BracketedExpression(substr);
      }

      else if (CharacterClassExpression.PATTERN_.matcher(substr).matches()) {
        // System.out.println("[: " + substr);
        re = new CharacterClassExpression(substr);
      }

      else if (EscapedChar.PATTERN_.matcher(substr).matches()) {
        // System.out.println("\\x: " + substr);
        re = new EscapedChar(substr);
      }

      // anything else
      else {
        // System.out.println("regular: " + substr);
        re = new RegularExpression(substr);
      }

      tokens.add(re);
      i = end;
    }
    if (i != regular_expression.length())
      tokens
          .add(new RegularExpression(regular_expression.substring(i, regular_expression.length())));

    return tokens;
  }

  public static String toPattern(List<RegularExpressionInterface> tokens, int start) {
    return toPattern(tokens, start, tokens.size());
  }

  public static String toPattern(List<RegularExpressionInterface> tokens, int start, int end) {
    StringBuffer sb = new StringBuffer();
    while (start < end)
      sb.append(tokens.get(start++).toString());
    return sb.toString();
  }

  /**
   * Merges two regular expressions.
   */
  public static BracketedExpression merge(RegularExpressionInterface re0,
      RegularExpressionInterface re1) {
    // System.out.println("Generalizing " + re0 + " and " + re1);

    // Types of res:
    // - normal / quoted expression / escaped char
    // - bracketed with +/*
    //
    // 1. if re0 and re1 is bracketed, merge both
    // 2. else generalize re0 and re1 and call this func again.
    if (re0 instanceof BracketedExpression && re1 instanceof BracketedExpression) {
      return BracketedExpression.merge((BracketedExpression)re0, (BracketedExpression)re1);
    } else {
      BracketedExpression r0 = re0.generalize();
      BracketedExpression r1 = re1.generalize();
      return merge(r0, r1);
    }
  }

  public static void main(String[] args) {
    // String regex =
    // "[0-9]AA\\Q(\\EA[A-Z\\x10ZZ\\QLALA\\E]+B\\x65BB \\QABC\\E \\x09";
    String regex = "PORT \r\n";
    List<RegularExpressionInterface> tokens = process(regex);
    System.out.println("REGEX: " + regex);
    System.out.println("TOKENS: " + tokens);

    System.out.println("NEW TOKENS: " + toPattern(tokens, 1, tokens.size()));

    for (int i = 0; i < tokens.size() - 1; i++) {
      for (int j = i + 1; j < tokens.size(); j++) {
        System.out.println("-----------------------------------");
        BracketedExpression generalized = merge(tokens.get(i), tokens.get(j));
        System.out.println("generalized = " + generalized);
      }

    }

    String re = "\\S+ LA";
    Pattern p = Pattern.compile(re, FLAGS);
    Matcher m = p.matcher("     LA");
    if (m.find()) {
      System.out.println("HERE");
    } else {
      System.out.println("NOT HERE");
    }

  }

}
