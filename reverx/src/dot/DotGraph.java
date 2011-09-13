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

package dot;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import traces.ByteChars;

public class DotGraph extends GraphViz {

  private String _filename;

  public DotGraph(String filename) {
    _filename = filename;
    addln(start_graph());
    // addln("rankdir=LR;");
  }

  public void writeGraphToFile(int MAX_CHARS) throws java.io.IOException {
    super.addln(end_graph());
    super.writeGraphToFile(getDotSource().getBytes(), new File(_filename + ".dot"));
    super.writeGraphToFile(getGraph(truncateLabels(getDotSource(), MAX_CHARS)), new File(_filename
        + ".gif"));
  }

  private String truncateLabels(String dot_source, int max_chars) {
    // Used to extract the label (supports quoted text).
    Pattern p = Pattern.compile("label=\".*?[^\\\\]\"");
    Matcher m = p.matcher(dot_source);
    StringBuffer sb = new StringBuffer();

    while (m.find()) {
      int begin_index = m.start();
      int end_index = begin_index + 6 + max_chars - 2;

      // Don't split escaped characters (eg. \", \\, etc.)
      while (end_index < m.end() && dot_source.charAt(end_index - 1) == '\\') {
        end_index++;
      }

      // Truncate if too long (> max_chars).
      if (end_index < m.end()) {
        String subst = dot_source.substring(begin_index, end_index) + "...\"";
        // replace accidental references to groups
        subst = subst.replaceAll("\\$", "\\\\\\$");
        subst = subst.replaceAll("\\\"", "\\\\\"");
        m.appendReplacement(sb, subst);
      }
    }
    m.appendTail(sb);
    return sb.toString();
  }

  public static String sanitize(String s) {
    ByteChars b = new ByteChars(s.getBytes());
    String sanitized = b.toString();
    String[] replace_list = {
        // sanitize backslash and quotes
        "\\\\", "\\\\\\\\", "\"", "\\\\\"", "'", "\\\\\'"
    // "\r\n", "<CRLF>", "\\\\0015\\\\0012", "<CRLF>",
    // "\r", "<LF>", "\\\\0012", "<LF>",
    // "\n", "<CR>", "\\\\0015", "<CR>",
    // "^ ", "<SP>", " $", "<SP>",
    };
    for (int i = 0; i < replace_list.length; i += 2)
      sanitized = sanitized.replaceAll(replace_list[i], replace_list[i + 1]);
    return sanitized;
  }

  public static void main(String[] args) {
    Pattern p = Pattern.compile("label=\".*?[^\\\\]\"");
    Matcher m = p.matcher("label=\"text com \\\"aspas\\\"\"");

    while (m.find()) {
      System.out.println(m.group());
    }

    String s = "ola $1 pah";
    System.out.println("[1] " + s);
    s = s.replaceAll("\\$", "\\\\\\$");
    System.out.println("[2] " + s);

  }
}
