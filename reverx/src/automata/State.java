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

public class State<T extends Symbol> implements Iterable<Transition<T>>, java.io.Serializable,
    Comparable<State<T>> {
  private static final long serialVersionUID = Symbol.serialVersionUID;

  public static int NEXT_ID = 0;

  protected boolean _is_final;
  protected ArrayList<Transition<T>> _transitions;
  protected int _id;

  public State() {
    _is_final = false;
    _transitions = new ArrayList<Transition<T>>(1);
    _id = NEXT_ID++;
  }

  public void setFinal(boolean is_final) {
    _is_final = is_final;
  }

  public boolean isFinal() {
    return _is_final;
  }

  public int getId() {
    return _id;
  }

  public void setId(int id) {
    _id = id;
  }

  public ArrayList<Transition<T>> getTransitions() {
    return _transitions;
  }

  public int getSumFreq() {
    int sum_freq = 0;
    for (Transition<T> t : _transitions)
      sum_freq += t._freq;
    return sum_freq;
  }

  protected Transition<T> getTransition(Symbol symbol) {
    for (Transition<T> t : _transitions)
      if (t._symbol.equals(symbol))
        return t;
    return null;
  }

  // //////////////////////////////////////////////////////////////////////////////

  @Override
  public String toString() {
    return "S" + _id;
  }

  public ArrayList<Transition<T>> getAcceptedTransitions(State<T> dest_state) {
    ArrayList<Transition<T>> transitions = new ArrayList<Transition<T>>(1);
    for (Transition<T> t : _transitions)
      if (dest_state == t._dest_state)
        transitions.add(t);
    return transitions;
  }

  private String get_transitions_symbols(Collection<Transition<T>> transitions, boolean with_labels) {
    StringBuilder s = new StringBuilder();
    Iterator<Transition<T>> iter = transitions.iterator();
    while (iter.hasNext()) {
      Transition<T> t = iter.next();
      s.append(t.toDot(with_labels));
      if (iter.hasNext())
        s.append("|");
    }

    return s.toString();

  }

  protected Collection<String> toDot(boolean with_labels) {
    ArrayList<String> dot_source = new ArrayList<String>(_transitions.size());
    HashSet<State<T>> possible_states = new HashSet<State<T>>();
    String from = this.toString();

    // Get all possible dest_states of state 'from'.
    for (Transition<T> t : _transitions)
      possible_states.add(t._dest_state);

    // Get all transitions.
    for (State<T> dest_state : possible_states) {
      String to = dest_state.toString();
      if (dest_state._is_final)
        dot_source.add(to + " [shape=doublecircle];");
      Collection<Transition<T>> remaining = getAcceptedTransitions(dest_state);
      String symbols = get_transitions_symbols(remaining, with_labels);
      dot_source.add(from + " -> " + to + " [label=\"" + symbols + "\"];");
    }

    return dot_source;
  }

  /**
   * Version of toDot() but with support for Red and Yellow labels. Red:
   * color=red,style=bold Yellow: color=orange,style=bold
   */
  public Collection<String> toDot(Collection<Transition<T>> red_transitions,
      Collection<Transition<T>> yellow_transitions) {
    ArrayList<String> dot_source = new ArrayList<String>(_transitions.size());
    Set<State<T>> possible_states = new HashSet<State<T>>();
    String from = this.toString();

    // Get all possible dest_states from this instance.
    for (Transition<T> t : _transitions)
      possible_states.add(t._dest_state);

    // Get all transitions that come from this instance to dest_state.
    for (State<T> dest_state : possible_states) {
      String to = dest_state.toString();
      if (dest_state._is_final)
        dot_source.add(to + " [shape=doublecircle];");

      // Get red and yellow transitions and remaining transitions.
      Collection<Transition<T>> remaining = getAcceptedTransitions(dest_state);
      Collection<Transition<T>> red = new ArrayList<Transition<T>>(remaining);
      red.retainAll(red_transitions);
      Collection<Transition<T>> yellow = new ArrayList<Transition<T>>(remaining);
      yellow.retainAll(yellow_transitions);
      remaining.removeAll(red);
      remaining.removeAll(yellow);

      String symbols = null;

      // Print red symbols.
      if (!red.isEmpty()) {
        symbols = get_transitions_symbols(red, false);
        dot_source.add(from + " -> " + to + " [color=red,style=dotted,label=\"" + symbols + "\"];");
      }

      // Print yellow symbols.
      if (!yellow.isEmpty()) {
        symbols = get_transitions_symbols(yellow, false);
        dot_source
            .add(from + " -> " + to + " [color=orange,style=bold,label=\"" + symbols + "\"];");
      }

      // Print remaining symbols.
      if (!remaining.isEmpty()) {
        symbols = get_transitions_symbols(remaining, false);
        dot_source.add(from + " -> " + to + " [label=\"" + symbols + "\"];");
      }
    }

    return dot_source;
  }

  /**
   * Checks if o is child of this at a particular level.
   */
  // private boolean isChild(State<T> o) {
  // for (Transition<T> t : _transitions) {
  // if (t._dest_state == o)
  // return true;
  // else if (isChild(t._dest_state))
  // return true;
  // }
  // return false;
  // }

  public int compareTo(State<T> o) {
    return this._id - o._id;
    // boolean is_child_of = this.isChild(o);
    // boolean is_parent_of = o.isChild(this);
    // if (is_child_of == is_parent_of)
    // return 0;
    // else if (is_child_of)
    // return -1;
    // else
    // return 1;
  }

  @Override
  public Iterator<Transition<T>> iterator() {
    return _transitions.iterator();
  }

}
