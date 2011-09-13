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

import java.awt.Point;
import java.util.*;

public class Operations {

  // ////////////////////////////////////////////////////////////
  // DETERMINIZATION
  // ////////////////////////////////////////////////////////////
  /**
   * Object for holding a set of states that should be merged together because
   * they share the same symbol at the same state
   */
  @SuppressWarnings("rawtypes")
  private static class UnionStates extends HashSet<State> {
    private static final long serialVersionUID = 1L;
    int freq = 0;
    boolean is_final = false;

    @Override
    public boolean equals(Object arg0) {
      if (arg0 instanceof UnionStates)
        return equals((UnionStates)arg0);
      return super.equals(arg0);
    }

    @Override
    public int hashCode() {
      return this.size();
    }

    public boolean equals(UnionStates arg0) {
      return this.containsAll(arg0) && arg0.containsAll(this);
    }
  }

  public static <T extends Symbol> void determinization(Automaton<T> automaton) {
    HashMap<UnionStates, State<T>> new_states = new HashMap<UnionStates, State<T>>(
        automaton._all_states.size());

    // Set the initial state of the new automaton.
    State<T> new_initial_state = new State<T>();
    new_initial_state._id = 0;
    UnionStates initial_state = new UnionStates();
    initial_state.add(automaton._initial_state);
    new_states.put(initial_state, new_initial_state);
    automaton._initial_state = new_initial_state;

    // Recursive call.
    determinization_rec(initial_state, new_states);

    // Set all states of the new automaton.
    automaton._all_states = new ArrayList<State<T>>(new_states.values());
  }

  private static <T extends Symbol> void determinization_rec(UnionStates states,
      HashMap<UnionStates, State<T>> new_states) {
    HashMap<T, UnionStates> defined_symbols = new HashMap<T, UnionStates>();

    /* Search for states to merge. */
    for (State<T> state : states) {
      for (Transition<T> t : state) {
        T key = t._symbol;

        UnionStates value = defined_symbols.get(key);
        if (value == null) {
          value = new UnionStates();
          defined_symbols.put(key, value);
        }
        value.add(t._dest_state);
        value.freq += t._freq;
        if (t._dest_state._is_final)
          value.is_final = true;
      }
    }

    State<T> this_state = new_states.get(states);

    /* Merge states: for each different symbol create a new transition. */
    for (Map.Entry<T, UnionStates> entry : defined_symbols.entrySet()) {
      T key = entry.getKey();
      UnionStates states_to_merge = entry.getValue();

      // Get (previously merged) new state (or we create it below).
      State<T> new_state = new_states.get(states_to_merge);
      if (new_state == null) {
        new_state = new State<T>();
        new_state._is_final = states_to_merge.is_final;
        if (states_to_merge.size() == 1)
          new_state._id = states_to_merge.iterator().next()._id;
        new_states.put(states_to_merge, new_state);
        // Recursive call.
        determinization_rec(states_to_merge, new_states);
      }
      Transition<T> new_t = new Transition<T>(key, new_state);
      new_t._freq = states_to_merge.freq;
      this_state._transitions.add(new_t);
    }

  }

  // ////////////////////////////////////////////////////////////
  // MINIMIZATION
  // ////////////////////////////////////////////////////////////

  // private static class MergedState<T extends Symbol> extends
  // HashSet<State<T>> {
  // State<T> s;
  //
  // public MergedState(State<T> s) {
  // setMergedState(s);
  // }
  //
  // public State<T> getMergedState() {
  // return s;
  // }
  //
  // public void setMergedState(State<T> s) {
  // this.s = s;
  // }
  //
  // @Override
  // public int hashCode() {
  // return 0; // use equals()
  // }
  //
  // @Override
  // public boolean equals(Object obj) {
  // if (obj instanceof State) {
  // return this.contains(obj);
  // }
  // return super.equals(obj);
  // }
  // }

  /**
   * Object for cell. By default a new object is a cell marked as false (not
   * equivalent).
   */
  private static class Cell {
    // List of other cells (x,y) that are dependent on this cell.
    ArrayList<Point> list = null; // minimize memory
  }

  /**
   * NxN / 2 table. To minimize memory usage, we delay the initialization of
   * cells. Because the minimization algorithm only looks for non-marked cells,
   * only non-marked cells are initialized. When a cell is marked, it is set to
   * null.
   */
  private static class DistinctStatesTable<T extends Symbol> {
    Cell[][] _table;
    ArrayList<State<T>> _all_states;

    // Coordinates (x,y) are translated to (x, y-x-1).
    DistinctStatesTable(ArrayList<State<T>> all_states) {
      _all_states = all_states;
      int size = _all_states.size();
      _table = new Cell[size - 1][];
      for (int row = 0; row < size - 1; row++) {
        _table[row] = new Cell[size - row - 1];
        State<T> s0 = _all_states.get(row);

        for (int col = 0; col < _table[row].length; col++) {
          State<T> s1 = _all_states.get(row + col + 1);
          // Delay cell creation. Only create cells for non-marked cells.
          if (s0._is_final != s1._is_final)
            // Mark trivially not equivalent states (final states != non final).
            _table[row][col] = null;
          else {
            // marked = false
            _table[row][col] = new Cell();
          }
        }
      }
    }

    @Override
    public String toString() {
      StringBuffer s = new StringBuffer();
      // Cols on left, rows on bottom.
      int size = _table.length;
      for (int row = 1; row <= size; row++) {
        s.append("\t" + _all_states.get(row));
        for (int col = 0; col < row; col++) {
          if (get(row, col) == null)
            s.append("   _");
          else
            s.append("   X");
        }
        s.append("\n");
      }
      s.append("\t   ");
      for (int col = 0; col <= size - 1; col++)
        s.append(" " + _all_states.get(col) + " ");
      return s.toString();
    }

    /**
     * Returns the element at specified row and col. May return null to indicate
     * that the cell was marked as not equivalent.
     */
    Cell get(int x, int y) {
      // Coordinates (x,y) are translated to (x, y-x-1).
      if (x < y)
        return _table[x][y - x - 1];
      else
        return _table[y][x - y - 1];
    }

    /**
     * Returns true if cell at (x,y) was marked as not equivalent (null).
     */
    boolean is_marked(int x, int y) {
      return get(x, y) == null;
    }

    /**
     * Mark cell (x,y) as not equivalent.
     */
    void mark_cell(int x, int y) {
      // Coordinates (x,y) are translated to (x, y-x-1).
      if (x < y)
        _table[x][y - x - 1] = null;
      else
        _table[y][x - y - 1] = null;
    }

    /**
     * Mark cell (x,y) as not equivalent and also mark all other cells in its
     * list as not equivalent.
     */
    void mark_and_propagate(int row, int col) {
      Cell cell = get(row, col);
      if (cell != null) {
        mark_cell(row, col);
        if (cell.list != null) {
          for (Point p : cell.list)
            mark_and_propagate(p.x, p.y);
        }
      }
    }

    /**
     * Adds cell (x1,y1) to the list of cell (x0,y0).
     */
    void add_to_list(int x0, int y0, int x1, int y1) {
      // From the algorithm, this cell is not marked (non-null).
      Cell cell = get(x0, y0);
      if (cell.list == null)
        cell.list = new ArrayList<Point>(1);

      Point p = new Point(x1, y1);
      if (!cell.list.contains(p))
        cell.list.add(p);
    }

  }

  /**
   * Minimization algorithm.
   */
  public static <T extends Symbol> void minimization(Automaton<T> automaton) {
    int total_states = automaton._all_states.size();
    System.out.println("[ ] minimizing automaton");
    System.out.println("[ ] \tcreating table for " + automaton._all_states.size() + " states.");

    DistinctStatesTable<T> table = new DistinctStatesTable<T>(automaton._all_states);

    int last_percent = -1;
    System.out.print("[ ] \tanalyzing every pair of states: [");

    // Go through rows.
    for (int row = 0; row < total_states - 1; row++) {
      float temp = (float)row / (float)(total_states - 1);
      int completed = 1 + (int)(temp * 100);
      if (completed % 5 == 0 && completed > last_percent) {
        last_percent = completed;
        if (completed % 5 == 0)
          System.out.print("=");
        if (completed % 100 == 0)
          System.out.print("]");
        else if (completed % 25 == 0)
          System.out.print("|");
      }

      State<T> q0 = automaton._all_states.get(row);

      // Go through columns.
      for (int col = row + 1; col < total_states; col++) {
        State<T> q1 = automaton._all_states.get(col);

        /* Process cells that were yet not marked. */
        if (table.is_marked(row, col) == false) {

          /*
           * Equivalent state must define identical transitions for the entire
           * alphabet.
           */
          for (int i = 0; i < 2; i++) {
            for (Transition<T> t : (i == 0) ? q0._transitions : q1._transitions) {
              // Ignore symbols processed in i==0.
              if (i == 1 && q0.getTransition(t._symbol) != null)
                continue;

              /*
               * Get transitions for current symbol and mark <q0,q1> as not
               * equiv if one of the states don't define it.
               */
              Transition<T> t0 = (i == 0) ? t : q0.getTransition(t._symbol);
              Transition<T> t1 = (i == 1) ? t : q1.getTransition(t._symbol);
              if (t0 == null || t1 == null) {
                if (t0 != t1)
                  table.mark_and_propagate(row, col);
                continue;
              }

              /*
               * If transitions go to different states p0 and p1, check pair
               * <p0,p1>.
               */
              State<T> p0 = t0._dest_state;
              State<T> p1 = t1._dest_state;
              if (p0 != p1) {
                int p0_index = automaton._all_states.indexOf(p0);
                int p1_index = automaton._all_states.indexOf(p1);

                /*
                 * If <p0,p1> is not marked, then <q0,q1> is added to its list.
                 * Else, mark <q0,q1> because they are not equiv, and also mark
                 * its entire list of pairs.
                 */
                if (table.is_marked(p0_index, p1_index) == false)
                  table.add_to_list(p0_index, p1_index, row, col);
                else
                  table.mark_and_propagate(row, col);

              }
            }
          } // for (Symbol a : all_symbols)
        } // if
      }
    }// for
    System.out.println();

    System.out.println("[ ] \tmerging equivalent states");
    /* Unification of the equiv states. */
    HashMap<State<T>, State<T>> merged_mapping = new HashMap<State<T>, State<T>>();
    LinkedList<State<T>> pairs_to_merge = new LinkedList<State<T>>();
    for (int row = 0; row < total_states - 1; row++) {
      // get merged state
      State<T> q0 = automaton._all_states.get(row);
      q0 = Operations.getMergedState(merged_mapping, q0);

      for (int col = row + 1; col < total_states; col++) {
        // get merged state
        State<T> q1 = automaton._all_states.get(col);
        q1 = Operations.getMergedState(merged_mapping, q1);

        // Ignore already merged states.
        if (q0 == q1)
          continue;

        // If cell was not marked, merge states and replace all instances.
        if (table.is_marked(row, col) == false) {
          pairs_to_merge.add(q0);
          pairs_to_merge.add(q1);
          merge(automaton, pairs_to_merge, merged_mapping);
        }
      }
    }

    // Remove duplicates.
    automaton._all_states = new ArrayList<State<T>>(new HashSet<State<T>>(automaton._all_states));
    System.out.println("[ ] \tminimized: " + total_states + " >  " + automaton._all_states.size()
        + " states (" + (int)((1 - (automaton._all_states.size() / ((float)total_states))) * 100)
        + "% smaler)");
  }

  // ////////////////////////////////////////////////////////////
  // MERGE STATES (recursively and without concurrent modification code)
  // ////////////////////////////////////////////////////////////

  /**
   * Merges two sates and removes any duplicates from automaton._all_states.
   * 
   * @param state0 Destination state.
   * @param state1 Source state.
   */
  public static <T extends Symbol> void merge(Automaton<T> automaton, State<T> state0,
      State<T> state1, HashMap<State<T>, State<T>> merged_mapping) {
    LinkedList<State<T>> pairs_to_merge = new LinkedList<State<T>>();
    pairs_to_merge.add(state0);
    pairs_to_merge.add(state1);
    merge(automaton, pairs_to_merge, merged_mapping);
    automaton._all_states = new ArrayList<State<T>>(new HashSet<State<T>>(automaton._all_states));
  }

  @SuppressWarnings("rawtypes")
  public static boolean samePairOfStates(State s0, State s1, State t0, State t1) {
    return (s0 == t0 && s1 == t1) || (s1 == t0 && s0 == t1);
  }

  /**
   * Search for possible already merged state.
   */
  public static <T extends Symbol> State<T> getMergedState(
      HashMap<State<T>, State<T>> merged_mapping, State<T> state) {
    // eg, state -> state' -> ... -> null
    State<T> result = state, temp = null;
    while ((temp = merged_mapping.get(result)) != null && temp != result)
      result = temp;
    return result;
  }

  /**
   * Merges two states (and their respective transitions) without recursive
   * calls. <b>NOTE:</b> it also replaces old states of the _initial_state and
   * _all_states fields with the merged states, thus creating duplicates.
   */
  public static <T extends Symbol> void merge(Automaton<T> automaton,
      LinkedList<State<T>> pairs_to_merge, HashMap<State<T>, State<T>> merged_mapping) {
    // Workaround to void recurrence.
    while (!pairs_to_merge.isEmpty()) {
      /* Getting pair of states to merge. */
      State<T> state0 = pairs_to_merge.removeFirst();
      State<T> state1 = pairs_to_merge.removeFirst();

      /* Search for possible already merged state. */
      state0 = getMergedState(merged_mapping, state0);
      state1 = getMergedState(merged_mapping, state1);

      if (state0 == state1)
        continue;

      /* Updating members. */
      state0._is_final = (state0._is_final || state1._is_final);
      state0._id = (state0._id < state1._id) ? state0._id : state1._id;

      /* Add all transitions from state1 to state0 (recursively). */
      for (Transition<T> t1 : state1) {
        // If symbol is already defined (t0), merge dest states,
        // otherwise just add the transition.
        Transition<T> t0 = state0.getTransition(t1._symbol);
        if (t0 != null) {
          t0._freq += t1._freq;
          if (t0._dest_state != t1._dest_state && t0._dest_state != null && t1._dest_state != null
              && !samePairOfStates(state0, state1, t0._dest_state, t1._dest_state)) {
            pairs_to_merge.add(t0._dest_state);
            pairs_to_merge.add(t1._dest_state);
          }
        } else
          state0._transitions.add(t1);
      }

      /* Replace all instances. */
      if (automaton._initial_state == state1)
        automaton._initial_state = state0;
      replace_all(automaton._all_states, state1, state0, true);
      replace_all(pairs_to_merge, state1, state0, false); // remove any state1
      merged_mapping.put(state1, state0);
    }

  }

  private static <T extends Symbol> void replace_all(List<State<T>> list, State<T> search_for,
      State<T> replace_with, boolean replace_in_transitions) {
    Set<State<T>> visited = new HashSet<State<T>>();
    /* Replace all instances of state1 with state0. */
    ListIterator<State<T>> iter = list.listIterator();
    while (iter.hasNext()) {
      State<T> s = iter.next();
      // Replace in all states.
      if (s == search_for)
        iter.set(replace_with);
      else {
        if (replace_in_transitions && visited.add(s)) {
          // Replace dest state in transitions.
          for (Transition<T> t : s) {
            if (t._dest_state == search_for)
              t._dest_state = replace_with;
          }
        }
      }
    }
  }

}
