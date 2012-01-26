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
import java.util.*;
import traces.*;
import utils.*;
import automata.*;

public class Language {
  protected Automaton<RegEx> automaton;

  public Language() {
    automaton = new Automaton<RegEx>();
  }

  public Automaton<RegEx> getAutomaton() {
    return automaton;
  }

  public static Automaton<RegEx> inferLanguage(boolean direction,
      Collection<List<Message>> messages, float t1, int t2) {
    Language l = new Language();
    l.inferFromTraces(direction, messages, t1, t2);
    return l.automaton;
  }

  public void inferFromTraces(boolean is_input, Collection<List<Message>> messages, float T1, int T2) {
    System.out.println("[ ] building automaton");
    // State.NEXT_ID = 0;

    /* Extract individual messages and add them to the automaton. */
    for (List<Message> session : messages) {
      System.out.println("> _____");
      for (Message message : session) {
        if (message.isInput() == is_input) {
          System.out.println("> " + message);
          addSequence(automaton, message);
        }
      }
    }

    int n = 0;
    // automaton.DRAW("lang" + (++n) + "-PTA", false);
    Operations.minimization(automaton);
    // automaton.DRAW("lang" + (++n) + "-PTA-minimized", false);

    /* Generalize and merge similar transitions. */
    System.out.println("[ ] generalizing automaton");
    int old_total = automaton.getAllStates().size();
    if (generalize(automaton, T2)) {
      Operations.determinization(automaton);
      Operations.minimization(automaton);
      // automaton.DRAW("lang" + (++n) + "-generalized-T2", false);
    }
    while (generalize(automaton, T1)) {
      Operations.determinization(automaton);
      Operations.minimization(automaton);
      // automaton.DRAW("lang" + (++n) + "-generalized-T1", false);
    }

    int new_total = automaton.getAllStates().size();
    System.out.println("[ ] \tgeneralized: " + old_total + " >  " + new_total + " states ("
        + (int)((1 - (new_total / ((float)old_total))) * 100) + "% smaler)");

    /* Concatenate linear transitions and states. */
    concatUniqueLinearStates(automaton);
    automaton.resetAllStates();
    // automaton.DRAW("lang" + (++n) + "-generalized", false);

  }

  /**
   * Creates and adds a new sequence by matching the largest substring of the
   * message. Problem with this version is that it can append sequences
   * splitting a field. Eg: of -> " " -> the -> ... and message:
   * "officials of other agencies..." would be appended to: of -> ficials ->
   * ..., splitting of-ficials
   */
  @Deprecated
  public static void addSequence(Automaton<RegEx> automaton, String message) {
    State<RegEx> state = automaton.getInitialState();
    int message_offset = 0;

    // Get common prefix.
    while (state.isFinal() == false) {
      boolean found = false;
      for (Transition<RegEx> t : state) {
        int match = t.getSymbol().match(message, message_offset);
        if (match > 0) {
          t.setFreq(t.getFreq() + 1);
          message_offset += match;
          state = t.getState();
          found = true;
          break;
        }
      }
      if (!found)
        break;
    }

    // Add a new path of symbols to remaining sequence.
    List<RegEx> sequence_of_symbols = RegEx.tokenize(message, message_offset);
    for (RegEx symbol : sequence_of_symbols) {
      State<RegEx> new_state = new State<RegEx>();
      automaton.getAllStates().add(new_state);
      Transition<RegEx> new_t = new Transition<RegEx>(symbol, new_state);
      new_t.setFreq(1);
      state.getTransitions().add(new_t);
      state = new_state;
    }
    state.setFinal(true);
  }

  /**
   * Creates and adds a new sequence by tokenizing the message.
   */
  public static void addSequence(Automaton<RegEx> automaton, Message message) {
    State<RegEx> state = automaton.getInitialState();
    List<RegEx> sequence_of_symbols = RegEx.tokenize(message, 0);
    int offset = 0;

    // Get common prefix.
    while (state.isFinal() == false && offset < sequence_of_symbols.size()) {
      RegEx symbol = sequence_of_symbols.get(offset);
      boolean found = false;

      for (Transition<RegEx> t : state) {
        if (t.getSymbol().equals(symbol)) {
          t.setFreq(t.getFreq() + 1);
          state = t.getState();
          offset++;
          found = true;
          break; // go to next state
        }
      }

      // break if not found
      if (!found)
        break;
    }

    // Add remaining sequence of symbols to new path.
    while (offset < sequence_of_symbols.size()) {
      RegEx symbol = sequence_of_symbols.get(offset);
      State<RegEx> new_state = new State<RegEx>();
      automaton.getAllStates().add(new_state);
      Transition<RegEx> new_t = new Transition<RegEx>(symbol, new_state);
      new_t.setFreq(1);
      state.getTransitions().add(new_t);
      state = new_state;
      offset++;
    }
    state.setFinal(true);
  }

  private static int _get_paths_to_state(Collection<State<RegEx>> all_states,
      State<RegEx> dest_state) {
    int total_transitions = 0;
    for (State<RegEx> s : all_states)
      for (Transition<RegEx> t : s)
        if (t.getState() == dest_state)
          total_transitions++;
    return total_transitions;
  }

  /**
   * Concatenates linear transitions (and merges respective states).
   * 
   * @param s0 State we wish to check if it has any linear transitions.
   * @param all_states List of all states.
   * @param visited Set of visited states.
   * @return Returns the index of the state merged with s0, -1 if no merging was
   *         made.
   */
  private static void concatUniqueLinearTransitions(State<RegEx> s0,
      Collection<State<RegEx>> all_states, Set<State<RegEx>> visited) {

    /*
     * We are going to get the linear transitions t0 and t1, such that t0 is a
     * transition from s0 going to s1, which is the only transition in the whole
     * automaton going to s1. Additionally, t1 is the only transition in s1.
     */

    // Get t0 and s1 (dest_state of t0).
    for (Transition<RegEx> t0 : s0) {

      // Keep checking if there are no more transitions to s1, besides
      // t0.
      while (true) {
        State<RegEx> s1 = t0.getState();
        if (_get_paths_to_state(all_states, s1) == 1) {

          // Get t1, and t0's and t1's symbols (to concatenate).
          ArrayList<Transition<RegEx>> transitions_from_s1 = s1.getTransitions();
          if (transitions_from_s1.size() == 1) {
            Transition<RegEx> t1 = transitions_from_s1.get(0);
            RegEx symb0 = (RegEx)t0.getSymbol();
            RegEx symb1 = (RegEx)t1.getSymbol();

            // Update t0 with both symbols.
            t0.setSymbol(RegEx.concat(symb0, symb1));
            t0.setState(t1.getState());

            // Delete s1 from all_states.
            all_states.remove(s1);
            continue;
          }
        }

        break;
      }

      // Proceed through this path (depth-first).
      State<RegEx> child = t0.getState();
      if (visited.add(child))
        concatUniqueLinearTransitions(child, all_states, visited);

    }

  }

  /**
   * Concatenates every two transitions that are "linear". If S0 -A-> S1 -B-> S2
   * and no more transitions exist to S1 besides A and from S1 besides B, then
   * S0 -AB-> S2
   */
  public static void concatUniqueLinearStates(Automaton<RegEx> automaton) {
    State<RegEx> initial_state = automaton.getInitialState();
    ArrayList<State<RegEx>> all_states = automaton.getAllStates();
    int size = all_states.size();
    concatUniqueLinearTransitions(initial_state, all_states, new HashSet<State<RegEx>>(size));
  }

  private static boolean generalize(Automaton<RegEx> automaton, int MIN_TRANSITIONS) {
    System.out.println("[ ] generalizing states with DIFFERENT_TRANSITIONS >= " + MIN_TRANSITIONS);
    ArrayList<State<RegEx>> new_states = new ArrayList<State<RegEx>>();
    boolean dirty = false;

    for (State<RegEx> s : automaton.getAllStates()) {
      int total_transitions = s.getTransitions().size();
      if (total_transitions <= 1)
        continue;

      System.out.print("[ ] \t" + s + "\tDIFFERENT_TRANSITIONS = " + total_transitions);
      // generalize
      if (total_transitions >= MIN_TRANSITIONS) {
        System.out.println("\tGEN!");
        for (Transition<RegEx> t : s) {

          if (RegEx.hasBinarySupport())
            // simplify parameter characterization
            dirty |= ((RegEx)t.getSymbol()).generalize(false);
          else
            dirty |= ((RegEx)t.getSymbol()).generalize_BINARY();
        }
      } else
        System.out.println();

    }// for ALL states

    if (dirty)
      automaton.getAllStates().addAll(new_states);
    return dirty;
  }

  private static boolean generalize(Automaton<RegEx> automaton,
      float MIN_RATIO_TRANSITIONS_OVER_TOTAL_FREQ) {
    System.out.println("[ ] generalizing states with RATIO_DIFFERENT_TRANSITIONS > "
        + Convert.toDecimalString(MIN_RATIO_TRANSITIONS_OVER_TOTAL_FREQ, 2) + "...");
    boolean dirty = false;

    for (State<RegEx> s : automaton.getAllStates()) {
      int total_transitions = s.getTransitions().size();
      if (total_transitions <= 1)
        continue;

      int sum_freq = 0;
      for (Transition<RegEx> t : s)
        sum_freq += t.getFreq();

      float ratio_different_transitions = (float)total_transitions / (float)sum_freq;
      System.out.print("[ ] \t" + s + "\tRATIO : " + total_transitions + " / " + sum_freq + "\t= "
          + String.format("%.2f", ratio_different_transitions));
      if (ratio_different_transitions >= MIN_RATIO_TRANSITIONS_OVER_TOTAL_FREQ) {
        System.out.println("\tGEN!");
        for (Transition<RegEx> t : s) {
          if (RegEx.hasBinarySupport())
            // simplify parameter characterization
            dirty |= ((RegEx)t.getSymbol()).generalize(false);
          else
            dirty |= ((RegEx)t.getSymbol()).generalize_BINARY();
        }

      } else
        System.out.println();

    }
    return dirty;
  }

  // //////////////////////////////////////////////////////////////////

  private static void printUsage(OptionsExtended options) {
    System.out.println("Usage: java Language [OPTIONS...] T1 T2 LANGUAGE [\"expression\"]");
    System.out.println();
    System.out.println("Creates an automaton that represents the protocol state machine "
        + "from messages taken from traces (text or pcap file) that are recognized by "
        + "the AUTOMATON message formats.");
    System.out.println();
    System.out.println("Options:");
    System.out.println(options.getUsageOptions());
    System.out.println("Report bugs to <jantunes@di.fc.ul.pt>.");
    // expressions:
    // http (doesn't filter non-http or continuation...): tcp dst port 3128
    // and (((ip[2:2] - ((ip[0]&0xf)<<2)) - ((tcp[12]&0xf0)>>2)) != 0)
    // ftp: dst port 21
    // pop: dst port 110
    // msn: dst port 1863
  }

  @SuppressWarnings("unchecked")
  public static void main(String[] args) {
    // main_debug(args); System.exit(0);

    OptionsExtended opt = new OptionsExtended();
    opt.setOption("--txt=", "-t", "FILE\ttext file with a packet payload in each line");
    opt.setOption("--pcap=", "-p", "FILE\tpacket capture file in tcpdump format");
    opt.setOption("--sessions=", null, "FILE\tsessions object file");
    opt.setOption("--max=", "-m", "NUMBER\tmaximum number of messages to process");
    opt.setOption("--delim=", null, "STRING\tdelimiter characters (text-based protocols only)");
    opt.setOption("--stateless=", "-s", "\tif the server/protocol is stateless");
    opt.setOption("--binary=", "-b", "\t\tbinary-based protocols");
    opt.setOption("--ip=", "--ip", "\t\tIP payload instead of TCP/UDP");
    opt.setOption("--snaplen=", null,
        "BYTES\tmaximum bytes to extract from payload (useful to extract headers)");

    Automaton.DEBUG = true;

    /* Check command line parameters. */
    opt.parseArgs(args);
    try {

      /* Parse command-line arguments. */
      float T1 = opt.getValueFloat();
      int T2 = opt.getValueInteger();
      String LANGUAGE = opt.getValueString();
      // Optional expression
      String EXPRESSION = (opt.getTotalRemainingArgs() > 0) ? opt.getValueString() : null;

      if (opt.getValueBoolean("-b"))
        RegEx.setBinarySupport();
      boolean stateless = opt.getValueBoolean("-s");
      int MAX = -1;
      if (opt.getValueBoolean("-m"))
        MAX = opt.getValueInteger("-m");

      String MSG_DELIMITER = opt.getValueString("--delim=");
      // Check for message delimiter (for text-based protocols).
      if (MSG_DELIMITER != null) {
        System.out.println("MSG_DELIMITER: " + MSG_DELIMITER);
        MSG_DELIMITER = Utils.toJavaString(MSG_DELIMITER);
        System.out.println("MSG_DELIMITER: " + MSG_DELIMITER);
      }

      /* Extract sessions (and save them) or just load them. */
      Collection<List<Message>> sessions = null;
      if (opt.getValueBoolean("--txt=") || opt.getValueBoolean("--pcap=")) {
        TracesInterface traces = null;
        String file = "";
        if (opt.getValueBoolean("--txt=")) {
          file = opt.getValueString("--txt=");
          traces = new TextFile(file);
        } else {
          file = opt.getValueString("--pcap=");
          traces = new PcapFile(opt.getValueString("--pcap="), EXPRESSION, null, MSG_DELIMITER);
          if (opt.getValueBoolean("--ip"))
            ((PcapFile)traces).setPayloadIp(true);
          if (opt.getValueBoolean("--snaplen="))
            ((PcapFile)traces).setSnaplen(opt.getValueInteger("--snaplen="));
        }

        // Get sessions from traces and save to .sessions file.
        traces.open();
        sessions = traces.getSessions(!stateless, MAX);
        traces.close();
        System.out.println("[ ] saving sessions to " + file + ".sessions");
        utils.Utils.saveToFile(new ArrayList<List<Message>>(sessions), file + ".sessions");
      }

      else if (opt.getValueBoolean("--sessions=")) {
        sessions = (Collection<List<Message>>)utils.Utils.readFromFile(opt
            .getValueString("--sessions="));
      }

      else {
        throw new OptionsException(OptionsException.Types.MISSING_PARAMETER, "Missing traces file.");
      }

      /* DEBUG: Reduce number of sessions/messages. */
      // int MAX_SESSIONS = 20;
      // Collection<List<Message>> temp = new
      // ArrayList<List<Message>>(MAX_SESSIONS);
      // int i = 0;
      // for (List<Message> session : sessions)
      // if (i++ >= MAX_SESSIONS)
      // break;
      // else
      // temp.add(session);
      // sessions = temp;

      System.out.println("# raw sessions:\t" + sessions.size());
      State.NEXT_ID = 0;
      Automaton.DEBUG_FILENAME = LANGUAGE;
      Language language = new Language();
      language.inferFromTraces(true, sessions, T1, T2);
      language.automaton.resetAllStates();
      language.automaton.saveToFile(LANGUAGE);
      language.automaton.drawAutomaton(LANGUAGE, false);

      System.out.println("# Printing all paths");
      ArrayList<ArrayList<RegEx>> all_paths = language.automaton.getListofPaths();
      for (ArrayList<RegEx> path : all_paths) {
        for (RegEx symbol : path)
          System.out.print(symbol);
        System.out.println();
      }

      System.out.println("[ ] DONE!");

    } catch (OptionsException e_options) {
      /* print usage and quit */
      printUsage(opt);
      System.err.println("[!] " + e_options.getMessage());
      System.exit(1);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
