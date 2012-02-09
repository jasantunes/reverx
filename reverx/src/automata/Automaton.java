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

import java.io.IOException;
import java.util.*;
import utils.Timer;
import utils.Utils;
import dot.DotGraph;

public class Automaton<T extends Symbol> implements java.io.Serializable {
  /* DEBUG */
  public static String DEBUG_FILENAME = "automaton.fsm";
  public static boolean DEBUG = false;
  public static Timer TIMER = new Timer();

  protected static final long serialVersionUID = 1L;
  protected State<T> _initial_state;
  protected ArrayList<State<T>> _all_states;

  public void DRAW(String filename_with_desc, boolean show_freq) {
    if (DEBUG)
      TIMER.pause();
    try {
      this.drawAutomaton(filename_with_desc, show_freq);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }
    TIMER.resume();
  }

  public Automaton() {
    _all_states = new ArrayList<State<T>>();
    _initial_state = new State<T>();
    _all_states.add(_initial_state);
  }

  public State<T> getInitialState() {
    return _initial_state;
  }

  public ArrayList<State<T>> getAllStates() {
    return _all_states;
  }

  public void resetAllStates() {
    // Remove duplicates and sort.
    _all_states = new ArrayList<State<T>>(new HashSet<State<T>>(_all_states));
    Collections.sort(_all_states);

    // Reset ids.
    State.NEXT_ID = 0;
    for (State<T> s : _all_states)
      s._id = State.NEXT_ID++;
  }

  protected State<T> appendNewSymbol(State<T> state, T symbol) {
    State<T> new_state = new State<T>();
    _all_states.add(new_state);
    Transition<T> new_t = new Transition<T>(symbol, new_state);
    new_t.setFreq(1);
    state.getTransitions().add(new_t);
    return new_state;
  }

  public void addSequence(Collection<T> sequence) {
    State<T> state = _initial_state;
    Iterator<T> iterator = sequence.iterator();
    T symbol = null;

    // Get common prefix.
    boolean found_prefix = true;
    while (found_prefix && !state.isFinal() && iterator.hasNext()) {
      symbol = iterator.next();
      found_prefix = false;
      for (Transition<T> t : state) {
        if (t.getSymbol().equals(symbol)) {
          found_prefix = true;
          t.setFreq(t.getFreq() + 1);
          state = t.getState();
          symbol = null;
          break;
        }
      }
    }
    // Add last non-matching symbol and the remaining symbols of the sequence.
    if (symbol != null)
      state = appendNewSymbol(state, symbol);
    while (iterator.hasNext())
      state = appendNewSymbol(state, iterator.next());

    // Set last state as final state.
    state.setFinal(true);
  }

  /**
   * Return true if the automaton accepts the sequence, and optionally builds a
   * Stack with the first accepted path.
   */
  private static <T extends Symbol> boolean accepts(boolean only_final_state, State<T> state,
      List<T> sequence, int offset) {
    if (offset == sequence.size())
      return (!only_final_state || (state != null && state.isFinal()));

    T symbol = sequence.get(offset);
    for (Transition<T> t : state) {
      if (t.getSymbol().equals(symbol)) {
        if (accepts(only_final_state, t.getState(), sequence, offset + 1))
          return true;
      }
    }

    return false;
  }

  /**
   * Sets a collection of all the accepted paths for a given sequence.
   */
  @SuppressWarnings("unchecked")
  private static <T extends Symbol> void accepts(boolean only_final_state, State<T> state,
      List<T> sequence, Stack<Transition<T>> accepted,
      Collection<Collection<Transition<T>>> fully_accepted_paths, int offset) {
    if (offset == sequence.size()) {
      if (!only_final_state || (state != null && state.isFinal())) {
        // if the automaton accepted the whole sequence,
        // keep the accepted sequence and backtrack
        fully_accepted_paths.add((Collection<Transition<T>>)accepted.clone());
        return;
      }
    }

    T symbol = sequence.get(offset);
    for (Transition<T> t : state) {
      if (t.getSymbol().equals(symbol)) {
        accepted.push(t);
        accepts(only_final_state, t.getState(), sequence, accepted, fully_accepted_paths,
            offset + 1);
        accepted.pop();
      }
    }
  }

  public boolean accepts(List<T> sequence) {
    return accepts(true, _initial_state, sequence, 0);
  }

  public Collection<Collection<Transition<T>>> acceptsAllPaths(List<T> sequence) {
    Collection<Collection<Transition<T>>> accepted_paths = new ArrayList<Collection<Transition<T>>>();
    accepts(true, _initial_state, sequence, new Stack<Transition<T>>(), accepted_paths, 0);
    return accepted_paths;
  }

  public boolean acceptsPrefix(List<T> prefix) {
    return accepts(false, _initial_state, prefix, 0);
  }

  public Collection<Collection<Transition<T>>> acceptsPrefixAllPaths(List<T> sequence) {
    Collection<Collection<Transition<T>>> accepted_paths = new ArrayList<Collection<Transition<T>>>();
    accepts(false, _initial_state, sequence, new Stack<Transition<T>>(), accepted_paths, 0);
    return accepted_paths;
  }

  // ////////////////////////////////////////////////////////////////////
  // DRAWING + OUTPUT
  // ////////////////////////////////////////////////////////////////////

  private static <T extends Symbol> int getMaxChars(int total_nodes) {
    int MAX_CHARS = (int)2000 / total_nodes;
    if (MAX_CHARS < 10)
      MAX_CHARS = 10;
    else if (MAX_CHARS > 150)
      MAX_CHARS = 150;
    return MAX_CHARS;
  }

  /*
   * Draw Finite State<T> Machine with Graphviz http://en.youxu.info/?p=32
   */
  public void drawAutomaton(String filename, boolean with_labels) throws IOException {
    System.out.println("[ ] saving graph to " + filename + ".gif...");
    DotGraph p = new DotGraph(filename);
    p.addln("rankdir=LR;");

    /* initial state */
    p.addln("null [shape=plaintext label=\"\"];");
    p.addln("null -> " + _initial_state.toString());

    /* remaining nodes */
    for (State<T> state : _all_states) {
      for (String line : state.toDot(with_labels))
        p.addln(line);
    }

    p.writeGraphToFile(getMaxChars(_all_states.size()));
    System.out.println();
  }

  /**
   * Remake of Automaton.drawAutomaton() to support red and yellow labels.
   */
  public void drawAutomaton(String filename, Collection<Transition<T>> red_transitions,
      Collection<Transition<T>> yellow_transitions) throws IOException {
    System.out.println("saving graph to " + filename + ".gif...");
    DotGraph p = new DotGraph(filename);
    p.addln("rankdir=LR;");

    /* initial state */
    p.addln("null [shape=plaintext label=\"\"];");
    p.addln("null -> " + _initial_state.toString());

    /* remaining nodes */
    for (State<T> state : _all_states) {
      for (String line : state.toDot(red_transitions, yellow_transitions))
        p.addln(line);
    }

    p.writeGraphToFile(getMaxChars(_all_states.size()));
    System.out.println();
  }

  private void getListofPaths_rec(List<T> current_path, State<T> state, List<List<T>> paths) {
    if (state.isFinal())
      paths.add(current_path);
    for (Transition<T> t : state._transitions) {
      List<T> subpath = new ArrayList<T>(current_path);
      subpath.add(t._symbol);
      getListofPaths_rec(subpath, t._dest_state, paths);
    }
  }

  public List<List<T>> getListofPaths() {
    List<List<T>> paths = new ArrayList<List<T>>();
    getListofPaths_rec(new ArrayList<T>(), _initial_state, paths);
    return paths;

  }

  private void add_graph_dot_nodes(ArrayList<String> dot_output, State<T> from, State<T> to,
      String from_label) {
    String from_toString = from.toString();
    String to_toString = to.toString();
    String[] dot_to = null;

    // FROM
    if (from._transitions.size() > 1) {
      for (Transition<T> t : from) {
        if (t._dest_state == to) {
          from_toString += t._dest_state.toString();
          break;
        }
      }
    }

    // add FROM label
    {
      from_label = DotGraph.sanitize(from_label);
      String dot_line = from_toString + " [label=\"" + from_label + "\"];";

      for (int i = 0; i < dot_output.size(); i++) {
        String line = dot_output.get(i);
        if (line.startsWith(from_toString + " [label=")) {
          dot_output.remove(i);
          dot_line = line.replaceAll("\\[label=\"(.*)\"]", "\\[label=\"$1|" + from_label + "\"\\]");
          break;
        }
      }

      dot_output.add(dot_line);

    }

    // TO + TRANSITIONS
    if (to._transitions.size() > 1) {
      dot_to = new String[to._transitions.size()];
      for (int i = 0; i < dot_to.length; i++) {
        dot_to[i] = to_toString + to._transitions.get(i)._dest_state.toString();
        String transition = from_toString + " -> " + dot_to[i] + ";";
        if (!dot_output.contains(transition))
          dot_output.add(transition);
      }
    }

    else if (to._transitions.size() == 1) {
      dot_to = new String[1];
      dot_to[0] = to_toString;
      String transition = from_toString + " -> " + dot_to[0] + ";";
      if (!dot_output.contains(transition))
        dot_output.add(transition);
    }

  }

  /*
   * Draw Finite State<T> Machine with Graphviz http://en.youxu.info/?p=32
   */
  public void drawDirectedGraph(String filename) throws IOException {
    System.out.println("[ ] saving graph to " + filename + ".gif...");
    DotGraph p = new DotGraph(filename);
    ArrayList<String> lines = new ArrayList<String>(_all_states.size());

    for (State<T> s : _all_states)
      for (Transition<T> t : s)
        add_graph_dot_nodes(lines, s, t._dest_state, t._symbol.toString());

    for (String s : lines)
      p.addln(s);

    p.writeGraphToFile(getMaxChars(_all_states.size()));
  }

  // //////////////////////////////////////////////////////////////////////////////
  // MISC.
  // //////////////////////////////////////////////////////////////////////////////

  public void saveToFile(String filename) throws IOException {
    Utils.saveToFile(this, filename);
  }

  public static <T extends Symbol> Automaton<T> loadFromFile(String filename) throws IOException,
      ClassNotFoundException {
    @SuppressWarnings("unchecked")
    Automaton<T> automaton = (Automaton<T>)Utils.readFromFile(filename);
    // Update static variables.
    int last_id = automaton._initial_state.getId();
    for (State<T> s : automaton._all_states)
      if (s.getId() > last_id)
        last_id = s.getId();
    State.NEXT_ID = last_id + 1;
    return automaton;
  }

  // public static <T extends Symbol><T extends SymbolInterface> int
  // compareAutomata(String
  // bash_script,
  // Automaton<T> arg0, Automaton<T> arg1) throws Exception {
  // ArrayList<ArrayList<Transition<T>>> all_paths = null;
  //
  // // Create temp file.
  // File temp0 = File.createTempFile("file0", ".tmp");
  // File temp1 = File.createTempFile("file1", ".tmp");
  //
  // // Write to temp file
  // all_paths = arg0.getAllPaths();
  // BufferedWriter out = new BufferedWriter(new FileWriter(temp0));
  // for (ArrayList<Transition<T>> path : all_paths) {
  // String s = Automaton.pathToString(path);
  // out.write(s);
  // }
  // out.close();
  //
  // // Write to temp file
  // all_paths = arg1.getAllPaths();
  // out = new BufferedWriter(new FileWriter(temp1));
  // for (ArrayList<Transition<T>> path : all_paths) {
  // String s = Automaton.pathToString(path);
  // out.write(s);
  // }
  // out.close();
  //
  // // execute script that compares two files (sort + diff)
  // Runtime run = Runtime.getRuntime();
  // Process pr = run.exec(new String[] {
  // bash_script, temp0.getAbsolutePath(), temp1.getAbsolutePath()
  // });
  // pr.waitFor();
  // BufferedReader buf = new BufferedReader(new
  // InputStreamReader(pr.getInputStream()));
  // String output = buf.readLine();
  // int diffs = new Integer(output);
  //
  // // clean up
  // temp0.delete();
  // temp1.delete();
  //
  // return diffs;
  // }

  // private void readObject(ObjectInputStream ois) throws IOException,
  // ClassNotFoundException {
  // System.out.println("AUTOMATON");
  // String regex = (String)ois.readObject();
  // }
  //
  // private void writeObject(ObjectOutputStream oos) throws IOException {
  // System.out.println("AUTOMATON");
  // oos.defaultWriteObject();
  // }

}
