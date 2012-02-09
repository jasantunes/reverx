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
import utils.Timer;
import automata.*;
import automata.RegExOperations.RegularExpressionInterface;

public class Language extends Automaton<RegEx> implements java.io.Serializable {
  protected static final long serialVersionUID = 1L;
  public static Timer TIMER;

  private static int STATS_TIMER_PTA;
  private static int STATS_TIMER_GENERALIZE;
  private static int STATS_TIMER_MINIMIZATION;
  private static int STATS_STATES0;
  private static int STATS_STATES1;
  private static int STATS_PATHS0;
  private static int STATS_PATHS1;
  private static int STATS_MESSAGES = 0;

  public Language(boolean is_input, Collection<List<Message>> messages, float T1, int T2) {
    System.out.println("[ ] building automaton");
    // State.NEXT_ID = 0;

    TIMER.restart();
    /* Extract individual messages and add them to the automaton. */
    for (List<Message> session : messages) {
      // System.out.println("> _____");
      for (Message m : session) {
        if (m.isInput() == is_input) {
          STATS_MESSAGES++;
          // System.out.println("> " + m);
          this.addSequence(m);
        }
      }
    }

    int n = 0;

    STATS_TIMER_PTA = TIMER.getElapsedTime();
    // automaton.DRAW("lang" + (++n) + "-PTA", false);
    System.out.println("[T] PTA:\t" + STATS_TIMER_PTA);

    /* STATS */
    TIMER.pause();
    STATS_STATES0 = _all_states.size();
    STATS_PATHS0 = this.getListofPaths().size();
    TIMER.resume();
    /* STATS */

    /* Generalize and merge similar transitions. */
    System.out.println("[ ] generalizing automaton");

    TIMER.mark();
    Operations.minimization(this);
    STATS_TIMER_MINIMIZATION = TIMER.getElapsedTimeFromMark();
    int old_total = _all_states.size();
    if (generalizeI(T2)) {
      TIMER.mark();
      Operations.determinization(this);
      Operations.minimization(this);
      STATS_TIMER_MINIMIZATION += TIMER.getElapsedTimeFromMark();
      // this.DRAW("lang" + (++n) + "-generalized-T2", false);
    }
    while (generalizeII(T1)) {
      TIMER.mark();
      Operations.determinization(this);
      Operations.minimization(this);
      STATS_TIMER_MINIMIZATION += TIMER.getElapsedTimeFromMark();
      // this.DRAW("lang" + (++n) + "-generalized-T1", false);
    }

    int new_total = _all_states.size();
    System.out.println("[ ] \tgeneralized: " + old_total + " >  " + new_total + " states ("
        + (int)((1 - (new_total / ((float)old_total))) * 100) + "% smaler)");

    /* Concatenate linear transitions and states. */
    if (RegEx.hasTextBasedSupport())
      concatUniqueLinearStates();
    this.resetAllStates();

    STATS_TIMER_GENERALIZE = TIMER.getElapsedTime() - STATS_TIMER_PTA - STATS_TIMER_MINIMIZATION;
    System.out.println("[T] Generalization:\t" + STATS_TIMER_GENERALIZE);

    /* STATS */
    STATS_STATES1 = _all_states.size();
    STATS_PATHS1 = this.getListofPaths().size();
    /* STATS */
  }

  /**
   * Creates and adds a new sequence by matching the largest substring of the
   * message. Problem with this version is that it can append sequences
   * splitting a field. Eg: of -> " " -> the -> ... and message:
   * "officials of other agencies..." would be appended to: of -> ficials ->
   * ..., splitting of-ficials
   */
  public void addSequence(Message message) {
    super.addSequence(RegEx.tokenize(message, 0));
  }

  private int transitions_to_state(State<RegEx> dest_state) {
    int total_transitions = 0;
    for (State<RegEx> s : _all_states)
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
  private void concatUniqueLinearTransitions(State<RegEx> s0, Set<State<RegEx>> visited) {
    if (!visited.add(s0))
      return;

    /*
     * We are going to get the linear transitions t0 and t1, such that t0 is a
     * transition from s0 going to s1, which is the only transition in the whole
     * automaton going to s1. Additionally, t1 is the only transition in s1.
     */

    for (Transition<RegEx> t0 : s0) {

      // concatenating t0+t1 where:
      // (s0) -t0-> (s1) -t1-> ...

      // Ignore immutable transitions.
      if (isImmutable(t0))
        continue;

      // There must be only one transition to s1, and that is t0.
      State<RegEx> s1 = t0.getState();
      int transitions_to_s1 = transitions_to_state(s1);
      if (transitions_to_s1 != 1)
        continue;

      // There must be only one transition leaving s1, and that is t1.
      ArrayList<Transition<RegEx>> transitions_from_s1 = s1.getTransitions();
      if (transitions_from_s1.size() != 1)
        continue;

      // Concatenate t0 + t1.
      Transition<RegEx> t1 = transitions_from_s1.get(0);
      RegEx symb0 = (RegEx)t0.getSymbol();
      RegEx symb1 = (RegEx)t1.getSymbol();

      // Update t0 with a concatenated symbol.
      t0.setSymbol(RegEx.concat(symb0, symb1));
      t0.setState(t1.getState());

      // Delete s1 from all_states.
      _all_states.remove(s1);

      // Proceed through this path (depth-first).
      concatUniqueLinearTransitions(t0.getState(), visited);

    }

  }

  /**
   * Concatenates every two transitions that are "linear". If S0 -A-> S1 -B-> S2
   * and no more transitions exist to S1 besides A and from S1 besides B, then
   * S0 -AB-> S2
   */
  public void concatUniqueLinearStates() {
    concatUniqueLinearTransitions(_initial_state, new HashSet<State<RegEx>>(_all_states.size()));
  }

  /**
   * Splits the transition's symbol at the first regular expression token. This
   * is required to merge transitions (by separating their first token and then
   * merging it).
   */
  private RegularExpressionInterface prepareTransitionForMerge(Transition<RegEx> t,
      Collection<State<RegEx>> new_states) {
    RegEx re = (RegEx)t.getSymbol();

    RegularExpressionInterface first_token = null;
    List<RegularExpressionInterface> re_tokens = RegExOperations.process(re.getPattern());
    if (re_tokens.size() > 1) {
      // Replace current symbol with first token.
      first_token = re_tokens.get(0);
      re.setPattern(first_token.toString());

      // Create a new transition for the remaining tokens.
      RegEx remaining = new RegEx(RegExOperations.toPattern(re_tokens, 1));
      Transition<RegEx> next = new Transition<RegEx>(remaining, t.getState());

      // Replace t's next state to an intermediate one.
      State<RegEx> intermediate_state = new State<RegEx>();
      intermediate_state.getTransitions().add(next);
      new_states.add(intermediate_state);
      t.setState(intermediate_state);

    } else {
      // Only one token, so no need to create an intermediate state.
      first_token = re_tokens.get(0);
      re.setPattern(first_token.toString());
    }

    return first_token;
  }

  private boolean generalizeState(State<RegEx> s, Collection<State<RegEx>> new_states) {
    System.out.print("\tGEN!");
    boolean dirty = false;

    // Get all symbols first, then generalize them all to the same unifying
    // symbol.
    List<RegularExpressionInterface> to_generalize = new ArrayList<RegularExpressionInterface>(s
        .getTransitions().size());
    for (Transition<RegEx> t : s) {
      if (!isImmutable(t)) {
        // if (RegEx.hasTextBasedSupport()) {
        RegularExpressionInterface symbol = prepareTransitionForMerge(t, new_states);
        to_generalize.add(symbol);
        dirty = true;
        // } else
        // dirty |= ((RegEx)t.getSymbol()).generalize_BINARY();
      }
    }

    // Get unifying symbol.
    RegExOperations.BracketedExpression unifying_symbol = new RegExOperations.BracketedExpression();
    for (RegularExpressionInterface re : to_generalize)
      unifying_symbol = RegExOperations.merge(unifying_symbol, re);
    String unifying_regular_expression = unifying_symbol.toString();

    // Set all transitions to the new unifying symbol.
    for (Transition<RegEx> t : s) {
      if (!isImmutable(t))
        ((RegEx)t.getSymbol()).setPattern(unifying_regular_expression);
    }

    return dirty;
  }

  private boolean generalizeI(int MIN_TRANSITIONS) {
    System.out.println("[ ] generalizing states with DIFFERENT_TRANSITIONS >= " + MIN_TRANSITIONS);
    ArrayList<State<RegEx>> new_states = new ArrayList<State<RegEx>>();
    boolean dirty = false;

    for (State<RegEx> s : _all_states) {
      int total_transitions = s.getTransitions().size();

      /* Check for eligible transitions */
      for (Transition<RegEx> t : s) {
        if (isImmutable(t))
          total_transitions--;
      }
      if (total_transitions <= 1)
        continue;

      /* generalize */
      System.out.print("[ ] \t" + s + "\tDIFFERENT_TRANSITIONS = " + total_transitions);
      if (total_transitions >= MIN_TRANSITIONS) {
        dirty |= generalizeState(s, new_states);
      }
      System.out.println();

    }// for ALL states

    if (dirty)
      _all_states.addAll(new_states);
    return dirty;
  }

  /**
   * Determines if a particular transition is immutable, i.e., if it cannot be
   * concatenated nor generalized with other transitions. A transition is
   * immutable if its symbol (RegEx) contains a delimiter or if it goes to a
   * final state.
   */
  private static boolean isImmutable(Transition<RegEx> t) {
    if (t.getState().isFinal())
      return true;
    RegEx re = (RegEx)t.getSymbol();
    // TODO: this delimiters are hardcoded... for now.
    if (re.accepts(" ") || re.accepts("\r\n"))
      return true;
    return false;
  }

  private boolean generalizeII(float MIN_RATIO_TRANSITIONS_OVER_TOTAL_FREQ) {
    System.out.println("[ ] generalizing states with RATIO_DIFFERENT_TRANSITIONS > "
        + Convert.toDecimalString(MIN_RATIO_TRANSITIONS_OVER_TOTAL_FREQ, 2) + "...");
    ArrayList<State<RegEx>> new_states = new ArrayList<State<RegEx>>();
    boolean dirty = false;

    for (State<RegEx> s : _all_states) {
      int total_transitions = s.getTransitions().size();
      int sum_freq = 0;

      // Check for eligible transitions.
      for (Transition<RegEx> t : s) {
        if (!isImmutable(t))
          sum_freq += t.getFreq();
        else
          total_transitions--;
      }

      if (total_transitions <= 1)
        continue;

      float ratio_different_transitions = (float)total_transitions / (float)sum_freq;
      System.out.print("[ ] \t" + s + "\tRATIO : " + total_transitions + " / " + sum_freq + "\t= "
          + String.format("%.2f", ratio_different_transitions));
      if (ratio_different_transitions >= MIN_RATIO_TRANSITIONS_OVER_TOTAL_FREQ)
        dirty |= generalizeState(s, new_states);

      System.out.println();

    }

    if (dirty)
      _all_states.addAll(new_states);

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
    TIMER = new Timer();

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
    opt.setOption("--output=", null, "\t\tinfer output messages (from sessions only)");

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
        RegEx.setTextBasedSupport(false);
      boolean stateless = opt.getValueBoolean("-s");

      int MAX = opt.getValueBoolean("-m") ? opt.getValueInteger("-m") : -1;

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
        if (opt.getValueBoolean("-m"))
          sessions = Sessions.trim(sessions, MAX);
      }

      else {
        throw new OptionsException(OptionsException.Types.MISSING_PARAMETER, "Missing traces file.");
      }

      State.NEXT_ID = 0;
      Automaton.DEBUG_FILENAME = LANGUAGE;
      TIMER.restart();
      Language language = new Language(!opt.getValueBoolean("--output="), sessions, T1, T2);
      System.out.println("[T] TOTAL TIME:\t" + TIMER.getElapsedTime());
      language.resetAllStates();
      language.saveToFile(LANGUAGE);
      language.drawAutomaton(LANGUAGE, false);

      /* DEBUG */
      // __checkLanguage__(language, !opt.getValueBoolean("--output="),
      // sessions,
      // "331 Anonymous login ok");

      System.out.println("# Printing all paths");
      List<List<RegEx>> all_paths = language.getListofPaths();
      for (List<RegEx> path : all_paths) {
        for (RegEx symbol : path)
          System.out.print(symbol);
        System.out.println();
      }
      System.out.println("[ ] inferred msgs formats:\t" + all_paths.size());

      System.out.println("[ ] DONE!");

      /* STATISTICS */
      // PcapFile.printStatistics(sessions);
      System.out.print("[S]\t" + STATS_MESSAGES);
      // times: PTA, GENERALIZE, MINIMIZATION
      System.out.print("\t" + STATS_TIMER_PTA + "\t" + STATS_TIMER_GENERALIZE + "\t"
          + STATS_TIMER_MINIMIZATION);
      // states after PTA and after generalization
      System.out.print("\t" + STATS_STATES0 + "\t" + STATS_STATES1);
      // inferred msg types after PTA and after generalization
      System.out.print("\t" + STATS_PATHS0 + "\t" + STATS_PATHS1);
      System.out.println();

    } catch (OptionsException e_options) {
      /* print usage and quit */
      printUsage(opt);
      System.err.println("[!] " + e_options.getMessage());
      System.exit(1);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Check if all traces are accepted.
   */
  public static void __checkLanguage__(Language l, boolean test_input,
      Collection<List<Message>> sessions, String begin_message_stop_at) throws Exception {
    for (List<Message> session : sessions) {
      for (Message m : session) {

        if (begin_message_stop_at != null && m.toString().startsWith(begin_message_stop_at))
          System.out.println("HERE");

        if (m.isInput() == test_input && l.accepts(m) == null)
          throw new Exception("X " + m);
      }
    }

  }

  /**
   * @deprecated In Language class, use accepts(CharSequence message) instead.
   */
  @Override
  @Deprecated
  public boolean accepts(List<RegEx> sequence) {
    return false;
  }

  /**
   * @deprecated In Language class, use accepts(CharSequence message) instead.
   */
  @Override
  @Deprecated
  public Collection<Collection<Transition<RegEx>>> acceptsAllPaths(List<RegEx> sequence) {
    return null;
  }

  /**
   * @deprecated In Language class, use accepts(CharSequence message) instead.
   */
  @Override
  @Deprecated
  public boolean acceptsPrefix(List<RegEx> prefix) {
    return false;
  }

  /**
   * @deprecated In Language class, use acceptsAllPaths(CharSequence message)
   *             instead.
   */
  @Override
  @Deprecated
  public Collection<Collection<Transition<RegEx>>> acceptsPrefixAllPaths(List<RegEx> sequence) {
    return null;
  }

  /**
   * Returns a path accepting the message.
   */
  public Collection<Transition<RegEx>> accepts(CharSequence message) {
    Stack<Transition<RegEx>> curr_path = new Stack<Transition<RegEx>>();
    if (accepts(_initial_state, message, curr_path, 0))
      return curr_path;
    else
      return null;
  }

  /**
   * Overrides automaton.accepts()
   */
  public static boolean accepts(State<RegEx> state, CharSequence message,
      Stack<Transition<RegEx>> curr_path, int offset) {
    if (offset == message.length())
      return (state != null && state.isFinal());

    for (Transition<RegEx> t : state) {
      int match = t.getSymbol().match(message, offset);
      if (match > 0) {
        if (curr_path != null)
          curr_path.push(t);
        if (accepts(t.getState(), message, curr_path, offset + match))
          return true;
        if (curr_path != null)
          curr_path.pop();
      }
    }
    return false;
  }

}
