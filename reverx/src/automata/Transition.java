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

public class Transition<T extends Symbol> implements Symbol, java.io.Serializable {
  private static final long serialVersionUID = Symbol.serialVersionUID;

  protected T _symbol;
  protected State<T> _dest_state;
  protected int _freq = 0;

  public Transition(T symbol, State<T> to_state) {
    _dest_state = to_state;
    _symbol = symbol;
  }

  public T getSymbol() {
    return _symbol;
  }

  public int getFreq() {
    return _freq;
  }

  public void setFreq(int freq) {
    _freq = freq;
  }

  public State<T> getState() {
    return _dest_state;
  }

  @Override
  public int hashCode() {
    return _symbol.hashCode() ^ _dest_state.hashCode();
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Transition) {
      Transition<T> t = (Transition<T>)obj;
      return (_dest_state == t._dest_state) && _symbol.equals(t._symbol);
    } else
      return _symbol.equals(obj);
  }

  @Override
  public String toString() {
    return toString(false);
  }

  public String toDot(boolean with_labels) {
    return _symbol.toDot() + (with_labels ? " (" + _freq + ")" : "");
  }

  public String toDot() {
    return _symbol.toDot();
  }

  public String toString(boolean with_labels) {
    return _symbol.toString() + (with_labels ? " (" + _freq + ")" : "");
  }

  public void setSymbol(T _symbol) {
    this._symbol = _symbol;
  }

  public void setState(State<T> dest_state) {
    this._dest_state = dest_state;
  }

}
