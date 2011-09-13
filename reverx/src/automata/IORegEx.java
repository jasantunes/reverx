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

import java.util.List;

public class IORegEx implements MessageType, java.io.Serializable {
  private static final long serialVersionUID = RegEx.serialVersionUID;
  protected RegEx input, output;

  public IORegEx(List<RegEx> input, List<RegEx> output) {
    // Concatenate input regex.
    this.input = new RegEx("");
    for (RegEx r : input)
      this.input = RegEx.concat(this.input, r);
    // Concatenate output regex.
    this.output = new RegEx("");
    for (RegEx r : output)
      this.output = RegEx.concat(this.output, r);
  }

  public RegEx getInput() {
    return input;
  }

  public RegEx getOutput() {
    return output;
  }

  @Override
  public int hashCode() {
    return input.hashCode() ^ output.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof IORegEx) {
      IORegEx other = (IORegEx)o;
      return (input.equals(other.input) && output.equals(other.output));
    }
    return false;
  }

  @Override
  public String toString() {
    return input.toString() + "/" + output.toString();
  }

  public String toDot() {
    return input.toDot() + "/" + output.toDot();
  }

}
