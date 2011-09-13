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

package automata;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import traces.Message;
import utils.Utils;
import dot.DotGraph;

public class RegEx implements Symbol, MessageType, java.io.Serializable {
  protected static final long serialVersionUID = Symbol.serialVersionUID;
  private Pattern _pattern;
  private static final int FLAGS = Pattern.DOTALL | Pattern.MULTILINE;
  private static final Pattern PATTERN_QUOTE = Pattern.compile("\\\\Q(.*?)\\\\E", FLAGS);

  /* Protocol definitions. */
  public static boolean IS_TEXT_BASED_PROTOCOL = true;
  private static final Pattern PATTERN_TOKEN = (IS_TEXT_BASED_PROTOCOL) ?
  // text- or binary-based initial field division
  Pattern.compile(" |\\r\\n", FLAGS) // every space or CRLF
      : Pattern.compile(".", FLAGS); // every byte

  /* For binary-based protocols only. */
  private boolean is_generalized_BINARY = false;
  private int length_BINARY = -1;

  public RegEx(Pattern pattern) {
    _pattern = pattern;
  }

  public RegEx(String regex) {
    this(Pattern.compile(regex, FLAGS));
  }

  public RegEx(CharSequence data) {
    this(quote(data));
    length_BINARY = data.length();
  }

  /**
   * Generalizes the regular expression (TEXT-BASED PROTOCOL VERSION).
   */
  public boolean generalize() {
    String old_pattern = _pattern.pattern();
    StringBuffer new_regex = new StringBuffer(old_pattern.length() + 10);
    Matcher matcher = PATTERN_QUOTE.matcher(old_pattern);

    int i = 0, end = 0;
    int start = 0;
    while (matcher.find()) {
      start = matcher.start();
      end = matcher.end();

      if (i != start)
        new_regex.append(old_pattern.substring(i, start));

      // Replace particular QUOTED REGEX with general regex.
      String s = old_pattern.substring(start + 2, end - 2);
      s = getGeneralizedRegEx(s.getBytes());
      new_regex.append(s);
      i = end;
    }
    if (i != old_pattern.length())
      new_regex.append(old_pattern.substring(i, old_pattern.length()));

    String new_pattern = new_regex.toString();
    if (!new_pattern.equals(old_pattern)) {
      _pattern = Pattern.compile(new_pattern, FLAGS);
      return true;
    }
    return false;

  }

  /**
   * Generalizes the regular expression (BINARY PROTOCOL VERSION).
   */
  public boolean generalize_BINARY() {
    if (length_BINARY > 0 && is_generalized_BINARY == false) {
      _pattern = Pattern.compile(".{" + length_BINARY + "}+", FLAGS);
      is_generalized_BINARY = true;
      return true;
    }
    return false;
  }

  // //////////////////////////////////////////////////////////////////////////////
  // STATIC
  // //////////////////////////////////////////////////////////////////////////////
  public static boolean isASCIIPrintable(byte b) {
    return (b >= 32 && b <= 126);
  }

  /**
   * If byte is any of [\^$.|?*+(){}
   */
  public static boolean isSpecialCharacter(byte b) {
    switch (b) {
      case '[':
      case '\\':
      case '^':
      case '$':
      case '.':
      case '|':
      case '?':
      case '*':
      case '+':
      case '(':
      case ')':
      case '{':
      case '}':
        return true;
    }
    return false;
  }

  //
  public static boolean isASCIIAlpha(byte b) {
    return ((b >= 65 && b <= 90) || (b >= 97 && b <= 122));
  }

  public static boolean isASCIIDigit(byte b) {
    return (b >= 48 && b <= 57);
  }

  public static boolean isASCIIPrintable(byte[] data) {
    for (byte b : data)
      if (!isASCIIPrintable(b))
        return false;
    return true;
  }

  private static String getGeneralizedRegEx(byte[] data) {

    boolean alpha = false;
    boolean digit = false;
    boolean repeat = false;
    HashSet<Byte> other_chars = new HashSet<Byte>(data.length);

    for (byte b : data) {
      if (isASCIIAlpha(b))
        alpha = true;
      else if (isASCIIDigit(b))
        digit = true;
      else
        repeat |= !other_chars.add(b);
      // other_chars.add(b);
    }
    repeat |= (alpha || digit);

    StringBuffer sb = new StringBuffer(6 + 3 + 5 * other_chars.size() + 2);

    if (alpha || digit || other_chars.size() > 1)
      sb.append('[');

    if (alpha) {
      sb.append("A-Za-z");
      /* uncomment to be optimistic: alpha=alphanumeric */
      // digit = true;
    }
    if (digit)
      sb.append("0-9");
    for (byte b : other_chars) {
      if (isASCIIPrintable(b))
        sb.append("\\Q" + (char)b + "\\E");
      else
        sb.append(escape(b));
    }

    if (alpha || digit || other_chars.size() > 1)
      sb.append(']');

    if (repeat == true)
      sb.append('+');

    return sb.toString();
  }

  public static RegEx concat(RegEx r0, RegEx r1) {
    String concat = r0._pattern.pattern() + r1._pattern.pattern();
    concat = concat.replaceAll("\\\\E\\\\Q", "");
    return new RegEx(concat);
  }

  private static ArrayList<String> tokenize_reg_ex(String reg_ex) {
    ArrayList<String> tokens = new ArrayList<String>();

    // Tokenize in blocks.
    Pattern p = Pattern.compile("(\\[.*?\\]|\\(.*?\\)|.|[A-Z0-9])[\\+\\*]", FLAGS);
    Matcher m = p.matcher(reg_ex);

    int last_end = 0;
    while (m.find()) {
      int start = m.start();
      int end = m.end();

      if (start != last_end) {
        String s = reg_ex.substring(last_end, start);
        System.out.println(">> " + s);
        tokens.add(s);
      }

      // Standard regular expression.
      String s = reg_ex.substring(start, end);
      // System.out.println("> " + s);
      tokens.add(s);

      last_end = end;
    }

    if (last_end != reg_ex.length()) {
      String s = reg_ex.substring(last_end, reg_ex.length());
      // System.out.println(">>> " + s);
      tokens.add(s);
    }

    return tokens;
  }

  public ArrayList<String> generateInstances() {
    ArrayList<String> tokens = new ArrayList<String>();

    // tokenize \E \Q
    String reg_ex = _pattern.pattern();
    Pattern p = Pattern.compile("\\\\Q.*?\\\\E", FLAGS);
    Matcher m = p.matcher(reg_ex);

    int last_end = 0;
    while (m.find()) {
      int start = m.start();
      int end = m.end();

      // standard regular expression
      if (start != last_end) {
        String s = reg_ex.substring(last_end, start);
        ArrayList<String> reg_ex_tokens = tokenize_reg_ex(s);
        tokens.addAll(reg_ex_tokens);
      }

      // quoted text
      String s = reg_ex.substring(start, end);
      tokens.add(s);

      last_end = end;
    }

    // standard regular expression
    if (last_end != reg_ex.length()) {
      String s = reg_ex.substring(last_end, reg_ex.length());
      ArrayList<String> reg_ex_tokens = tokenize_reg_ex(s);
      tokens.addAll(reg_ex_tokens);
    }

    /*
     * TODO: Merge serial regular expressions, such as "[A-Z]+ [A-Z]+", which is
     * transformed into "[A-Z ]+[A-Z]+", and then minimized into "[A-Z ]+".
     */

    /* Expand regular expressions in instances. */
    // ArrayList<ArrayList<String>> expanded is in the form of:
    // array of tokens where each token has all expansions for that token.
    ArrayList<ArrayList<String>> all_tokens_expansions = new ArrayList<ArrayList<String>>(tokens
        .size());
    for (String token : tokens) {
      ArrayList<String> token_expansion = new ArrayList<String>();

      Matcher match_quote = PATTERN_QUOTE.matcher(token);
      if (match_quote.find()) {
        token = match_quote.replaceAll("$1");
        token_expansion.add(token);
      } else {
        token_expansion.addAll(expandRegEx(token));
      }
      all_tokens_expansions.add(token_expansion);
    }

    /* Generate strings from ArrayList<ArrayList<String>> expanded. */
    ArrayList<StringBuilder> all_instances = new ArrayList<StringBuilder>();
    all_instances.add(new StringBuilder());
    for (ArrayList<String> token : all_tokens_expansions) {
      if (token.size() == 1) {
        String s = token.get(0);
        for (StringBuilder sb : all_instances)
          sb.append(s);
      } else {
        ArrayList<StringBuilder> all_instances_tmp = new ArrayList<StringBuilder>(all_instances
            .size()
            * token.size());
        for (StringBuilder sb : all_instances) {
          for (String s : token)
            all_instances_tmp.add(new StringBuilder(sb + s));
        }
        all_instances = all_instances_tmp;
      }
    }

    ArrayList<String> result = new ArrayList<String>(all_instances.size());
    for (StringBuilder sb : all_instances)
      result.add(sb.toString());

    return result;

  }

  private static ArrayList<String> expandRegEx(String reg_ex) {
    ArrayList<String> expanded = new ArrayList<String>();
    String quantifier = "";

    boolean regex_any_char = false;
    ArrayList<Character> regex_square_brackets = null;

    // .*
    if (reg_ex.matches("\\.[+*]{0,1}")) {
      quantifier = reg_ex.replaceAll("\\.([+*]{0,1})", "$1");
      regex_any_char = true;
    }

    // [A-Z]+
    else if (reg_ex.matches("\\[.*?\\].*")) {

      // Accepted chars.
      regex_square_brackets = new ArrayList<Character>(128);

      String inside = reg_ex.replaceAll("\\[(.*?)\\].*", "$1");

      char last_c = 0;
      boolean interval = false;
      for (char c : inside.toCharArray()) {
        if (c == '-')
          interval = true;
        else {
          regex_square_brackets.add(c);
          if (interval) {
            for (int i = last_c + 1; i < c; i++)
              regex_square_brackets.add((char)i);
            interval = false;
          }
          last_c = c;
        }
      }

      // Get quantifier: +, * or nothing
      quantifier = reg_ex.replaceAll("\\[.*?\\]([+*]{0,1})", "$1");

    } // [A-Z]+

    // no matching reg ex special
    else {
      expanded.add(reg_ex);
      return expanded;
    }

    /* Expand into strings */
    int min = 0, max = 0;
    if (quantifier.equals("")) {
      min = 1;
      max = 1;
    } else if (quantifier.equals("+")) {
      min = 1;
      max = 10;
    } else if (quantifier.equals("*")) {
      min = 0;
      max = 10;
    }

    // min=0. Add empty string.
    if (min == 0) {
      expanded.add("");
    }

    // min=0 or min=1. Add random string with minimum length.
    if (min >= 0) {
      if (regex_any_char) {
        int i = Utils.random('A', 'Z' + 1);
        expanded.add(new Character((char)i).toString());
      } else if (regex_square_brackets != null) {
        int i = Utils.random(0, regex_square_brackets.size());
        Character c = regex_square_brackets.get(i);
        expanded.add(c.toString());
      }

    }

    if (max > 1) {
      StringBuilder sb = new StringBuilder(max);
      if (regex_any_char) {
        for (int j = 0; j < max; j++) {
          int i = Utils.random('A', 'Z' + 1);
          sb.append((char)i);
        }
      } else if (regex_square_brackets != null) {
        for (int j = 0; j < max; j++) {
          int i = Utils.random(0, regex_square_brackets.size());
          Character c = regex_square_brackets.get(i);
          sb.append(c);
        }
      }
      expanded.add(sb.toString());
    }

    return expanded;
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

  public static RegEx getConcatenatedPath(Automaton<RegEx> language, Message message) {
    List<Transition<RegEx>> path = getPath(language, message);
    RegEx concat = new RegEx("");
    for (Transition<RegEx> t : path)
      concat = RegEx.concat(concat, t.getSymbol());
    return concat;
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

  public static List<Transition<RegEx>> getPath(Automaton<RegEx> automaton, CharSequence message) {
    LinkedList<Transition<RegEx>> path = new LinkedList<Transition<RegEx>>();
    if (_get_path(message, 0, automaton._initial_state, path))
      return path;
    else
      return null;
  }

  // TODO a function like this was implemented in accepts() (used in
  // checkForLanguageViolations().
  private static boolean _get_path(CharSequence message, int message_offset,
      State<RegEx> from_state, LinkedList<Transition<RegEx>> output_path) {
    if (message.length() == message_offset)
      return true; // found a path (end of recursion)

    for (Transition<RegEx> t : from_state) {
      RegEx regex = (RegEx)t._symbol;
      int match = regex.match(message, message_offset);

      // If we match at least one symbol, we keep searching from this path.
      if (match > 0) {
        output_path.addLast(t);
        if (_get_path(message, message_offset + match, t._dest_state, output_path))
          return true; // found a path (backtrack recursion)
        else
          // Undo 'output_path.add(t);'
          output_path.removeLast();
      }
    }
    return false;
  }

  /**
   * Pattern.quote(String) has a problem with strings that include 0x0 bytes.
   */
  public static String quote(CharSequence data) {
    StringBuffer sb = new StringBuffer();
    boolean quoting = false;
    for (int i = 0; i < data.length(); i++) {
      char b = data.charAt(i);
      if (isASCIIPrintable((byte)b)) {
        if (!quoting) {
          sb.append("\\Q"); // quote ascii character.
          quoting = true;
        }
        sb.append((char)b); // append character
      } else {
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

}
