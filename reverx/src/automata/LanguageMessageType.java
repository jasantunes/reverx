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

import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanguageMessageType implements MessageType, java.io.Serializable {
  protected static final long serialVersionUID = Symbol.serialVersionUID;

  // Regular expression to extract the relevant part each transition (field).
  // use for first textual field
  public static Pattern PATTERN_FIELD = null;
  public static final Pattern PATTERN_TEXTUAL_FIELD = Pattern.compile("\\w+");

  // Path of transitions in language automaton that accept a type of message.
  private Collection<Transition<RegEx>> _path_in_language;

  public static void setTextualProtocol(boolean is_textual) {
    if (is_textual)
      LanguageMessageType.PATTERN_FIELD = LanguageMessageType.PATTERN_TEXTUAL_FIELD;
    else
      LanguageMessageType.PATTERN_FIELD = null;
  }

  public LanguageMessageType(Collection<Transition<RegEx>> message_type) {
    _path_in_language = message_type;

    // get string from regexs
    // StringBuffer sb = new StringBuffer();
    // for (Transition<RegEx> t : _path_in_language)
    // sb.append(t.getSymbol().toString());
    // _message_type_tostring = sb.toString();
  }

  public Collection<Transition<RegEx>> getPathInLanguage() {
    return _path_in_language;
  }

  @Override
  public int hashCode() {
    return _path_in_language.hashCode();
  }

  @Override
  public boolean equals(Object obj) {

    if (obj instanceof LanguageMessageType) {
      LanguageMessageType other = (LanguageMessageType)obj;

      // We don't use plain equals because we have a list of transitions. We
      // have
      // to override this to compare only the symbols of the transitions.
      Iterator<Transition<RegEx>> iter0 = this._path_in_language.iterator();
      Iterator<Transition<RegEx>> iter1 = other._path_in_language.iterator();
      while (iter0.hasNext() && iter1.hasNext()) {
        Symbol s0 = ((Transition<RegEx>)iter0.next())._symbol;
        Symbol s1 = ((Transition<RegEx>)iter1.next())._symbol;
        if (!s0.equals(s1))
          return false;
      }
      return iter0.hasNext() == iter1.hasNext();
    }

    return super.equals(obj);
  }

  @Override
  public String toString() {
    // return _message_type_tostring;
    StringBuffer sb = new StringBuffer();
    for (Transition<RegEx> t : _path_in_language)
      sb.append(t.getSymbol().toString());
    return sb.toString();
  }

  /**
   * Applies the PATTERN_FIELD to show only the relevant part each of the
   * Transition<RegEx> that compose the input and output part of this message
   * type.
   */
  private static String sanitize(String string) {
    if (PATTERN_FIELD != null) {
      Matcher m = PATTERN_FIELD.matcher(string);
      if (m.find())
        string = m.group();
    }
    return string;
  }

  public String toDot() {
    StringBuffer sb = new StringBuffer();
    for (Transition<RegEx> t : _path_in_language)
      sb.append(t.getSymbol().toDot());

    return sanitize(sb.toString());
  }

  // public boolean accepts(ByteChars message) {
  // int offset = 0;
  // int match = 0;
  // Iterator<Transition<RegEx>> iter = _path_in_language.iterator();
  // while (iter.hasNext()) {
  // match = iter.next()._symbol.match(message, offset);
  // if (match == 0)
  // return false;
  // offset += match;
  // }
  // return (iter.hasNext() == false);
  // }

}
