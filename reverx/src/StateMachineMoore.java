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

public class StateMachineMoore extends Automaton<MessageType> implements java.io.Serializable {
  protected static final long serialVersionUID = 1L;
  protected Language language;

  protected static int STATS_TIMER_PTA;
  protected static int STATS_TIMER_GENERALIZE;
  protected static int STATS_TIMER_MINIMIZATION;
  protected static int STATS_STATES0;
  protected static int STATS_STATES1;
  protected static int STATS_PATHS0;
  protected static int STATS_PATHS1;
  protected static int STATS_MESSAGES = 0;

  public static Timer TIMER;

  // EXCEPTION
  public static class UnknownMessageTypeException extends Exception {
    int nth_message = 0; // which message was not accepted (in the traces)

    public UnknownMessageTypeException(String string, int nth_message) {
      super(string);
      this.nth_message = nth_message;
    }

    private static final long serialVersionUID = 1L;
  }

  public StateMachineMoore() {
    language = null;
  }

  public StateMachineMoore(Language l, Collection<List<Message>> sessions)
      throws UnknownMessageTypeException {
    super();
    language = l;
    infer(sessions);
  }

  protected void infer(Collection<List<Message>> sessions) throws UnknownMessageTypeException {
    System.out.println("[ ] building automaton");
    int session_id = 0;

    TIMER.restart();
    /* Build raw automaton. */
    for (List<Message> session : sessions) {
      List<MessageType> inferred = convertSessionToSequenceOfMsgTypes(session);
      System.out.println("[" + session_id + "] adding " + inferred);
      super.addSequence(inferred);
      session_id++;
    }

    STATS_TIMER_PTA = TIMER.getElapsedTime();
    System.out.println("[T] PTA:\t" + STATS_TIMER_PTA);

    /* STATS */
    STATS_STATES0 = _all_states.size();
    // STATS_PATHS0 = this.getListofPaths().size();
    TIMER.restart();
    /* STATS */

    System.out.println("[ ] merging automaton");
    // automaton.DRAW("statemachine1-PTA", false);
    TIMER.mark();
    Operations.minimization(this);
    STATS_TIMER_MINIMIZATION = TIMER.getElapsedTimeFromMark();
    // automaton.DRAW("statemachine2-PTA-minimized", false);

    /* Generalize automaton. */
    generalize(this);
    this.resetAllStates();
    STATS_TIMER_GENERALIZE = TIMER.getElapsedTime() - STATS_TIMER_MINIMIZATION;
    System.out.println("[T] Merge:\t" + TIMER.getElapsedTimeFromMark());

    /* STATS */
    STATS_STATES1 = _all_states.size();
    // STATS_PATHS1 = this.getListofPaths().size();
    TIMER.restart();
    /* STATS */

  }

  // ////////////////////////////////////////////////////////

  /**
   * Returns the number of message types in common.
   */
  private static int _msg_types_have_in_common(State<MessageType> s0, State<MessageType> s1) {

    // Message types defined in s0
    Set<MessageType> transitions_in_s0 = new HashSet<MessageType>(s0.getTransitions().size());
    for (Transition<MessageType> t : s0.getTransitions())
      transitions_in_s0.add(t.getSymbol());

    // Message types defined in s1
    Set<MessageType> transitions_in_s1 = new HashSet<MessageType>(s1.getTransitions().size());
    for (Transition<MessageType> t : s1.getTransitions())
      transitions_in_s1.add(t.getSymbol());

    // intersection
    transitions_in_s0.retainAll(transitions_in_s1);

    return transitions_in_s0.size();
  }

  private static HashSet<State<MessageType>> getPartition(
      HashSet<HashSet<State<MessageType>>> partitions, State<MessageType> state) {
    for (HashSet<State<MessageType>> partition : partitions)
      if (partition.contains(state))
        return partition;
    return null;
  }

  /**
   * Merges each set in list_of_sets into one state. Returns true if it has
   * performed changes.
   */
  private static boolean mergeEachSet(Automaton<MessageType> automaton,
      Collection<HashSet<State<MessageType>>> all_sets) {
    boolean result = false;

    HashMap<State<MessageType>, State<MessageType>> merged_mapping = new HashMap<State<MessageType>, State<MessageType>>();

    // System.out.println("[ ] merging states...");
    for (HashSet<State<MessageType>> set_to_merge : all_sets) {
      if (set_to_merge.size() <= 1)
        continue;
      result = true;

      // System.out.println("\tmerging " + partition);
      State<MessageType> merged = null;

      for (State<MessageType> state : set_to_merge) {
        /* Merge states. */
        if (merged == null) // first iteration
          merged = state;
        // else if (state == null) // TODO: I think it never happens
        // ; // merged = merged
        else {
          if (merged != state)
            Operations.merge(automaton, merged, state, merged_mapping);
        }

        /* Map 'state' to 'merged'. */
        // merged_mapping.put(state, merged);

      }
    }
    return result;
  }

  public static void generalize(Automaton<MessageType> automaton) {
    ArrayList<State<MessageType>> _all_states = automaton.getAllStates();
    int n = 2;

    /* Initially set all states as non-final. */
    for (State<MessageType> s : _all_states)
      s.setFinal(false);

    boolean dirty = true;
    int i = 0;
    while (dirty == true) {
      dirty = false;
      System.out.println("[ ] Reduce x " + (++i) + " times");

      /* Merge all dest_state that come from the same symbol. */
      if (generalizeI(automaton)) {
        dirty = true;
        System.out.println("\ttrue");
        TIMER.mark();
        Operations.determinization(automaton);
        Operations.minimization(automaton);
        STATS_TIMER_MINIMIZATION += TIMER.getElapsedTimeFromMark();
        // automaton.DRAW("statemachine" + (++n) + "-reduceI", false);
      }

      /*
       * Merge all states that share at least one identical transition and have
       * no causal relation.
       */
      while (generalizeII(automaton)) {
        dirty = true;
        System.out.println("\ttrue");
        TIMER.mark();
        Operations.determinization(automaton);
        Operations.minimization(automaton);
        STATS_TIMER_MINIMIZATION += TIMER.getElapsedTimeFromMark();
        // automaton.DRAW("statemachine" + (++n) + "-reduceII", false);
      }

    }

    /* Find final states. */
    for (State<MessageType> s : _all_states)
      if (s.getTransitions().isEmpty())
        s.setFinal(true);

  }

  /**
   * Merges cyclic states that are considered similar. Their similarity is
   * related to the number of minimal changes that two states have to suffer to
   * be identical.
   */
  private static boolean generalizeII(Automaton<MessageType> automaton) {
    System.out.println("[ ] reduceII()");
    ArrayList<State<MessageType>> _all_states = automaton.getAllStates();
    HashSet<HashSet<State<MessageType>>> partitions = new HashSet<HashSet<State<MessageType>>>();

    /* Compare each pair of partitions: containing s0 and s1. */
    for (int i = 0; i < _all_states.size() - 1; i++) {
      State<MessageType> s0 = _all_states.get(i);

      // Get partition with s0.
      HashSet<State<MessageType>> partition_with_s0 = getPartition(partitions, s0);
      if (partition_with_s0 == null) {
        partition_with_s0 = new HashSet<State<MessageType>>();
        partition_with_s0.add(s0);
        partitions.add(partition_with_s0);
      }

      for (int j = i + 1; j < _all_states.size(); j++) {
        State<MessageType> s1 = _all_states.get(j);

        /* Don't merge if there is a causal relation. */
        // break if either 1) S0->S1 and no S1->S0
        // 2) S1->S0 and no S0->S1
        boolean s0_to_s1 = !s0.getAcceptedTransitions(s1).isEmpty();
        boolean s1_to_s0 = !s0.getAcceptedTransitions(s1).isEmpty();
        if (s0_to_s1 != s1_to_s0)
          // the same as (s0_to_s1 && !s1_to_s0) || (s1_to_s0 && !s0_to_s1)
          continue;

        /* If there are transitions in common, merge two partitions. */
        if (_msg_types_have_in_common(s0, s1) > 0) {
          // Get partition with s1 and merge it with partition with s0.
          HashSet<State<MessageType>> partition_with_s1 = getPartition(partitions, s1);
          if (partition_with_s1 != null && partition_with_s0 != partition_with_s1) {
            partition_with_s0.addAll(partition_with_s1);
            partition_with_s1.clear(); // remove doesn't work
            System.out.println("merging two partitions");
          } else
            partition_with_s0.add(s1);
        }

      }

    }

    /* Merge all states of each partition. */
    boolean changed = mergeEachSet(automaton, partitions);
    return changed;
  }

  private static ArrayList<HashSet<State<MessageType>>> getAllSets(
      Collection<HashSet<State<MessageType>>> collection_of_sets) {
    ArrayList<HashSet<State<MessageType>>> partitions = new ArrayList<HashSet<State<MessageType>>>(
        collection_of_sets.size());

    // add each set as a new partition or merge with existing one
    for (HashSet<State<MessageType>> set : collection_of_sets) {

      // check if any elem of set already exists in any partition
      HashSet<State<MessageType>> existing_partition = null;
      for (HashSet<State<MessageType>> partition : partitions) {
        for (State<MessageType> s : set)
          if (partition.contains(s)) {
            // merge with existing partition
            existing_partition = partition;
            break;
          }
        if (existing_partition != null)
          break;
      }

      // create a new partition
      if (existing_partition == null) {
        existing_partition = new HashSet<State<MessageType>>();
        partitions.add(existing_partition);
      }

      // add set to new or existing partition
      existing_partition.addAll(set);
    }

    return partitions;
  }

  /**
   * Merge all "similar" dest_states, ie, states that are destination of the
   * same symbol. In this resulting state machine each state represents the
   * state of the protocol that accepts a given message format.
   */
  private static boolean generalizeI(Automaton<MessageType> automaton) {
    System.out.println("[ ] reduceI()");
    ArrayList<State<MessageType>> _all_states = automaton.getAllStates();
    HashMap<MessageType, HashSet<State<MessageType>>> to_merge = new HashMap<MessageType, HashSet<State<MessageType>>>();

    // Get sets of states that accept the same symbol/message_format.
    for (State<MessageType> state : _all_states) {
      for (Transition<MessageType> t : state) {
        MessageType symbol = t.getSymbol();
        HashSet<State<MessageType>> set = to_merge.get(symbol);
        if (set == null) {
          set = new HashSet<State<MessageType>>();
          to_merge.put(symbol, set);
        }
        set.add(t.getState());
      }
    }

    ArrayList<HashSet<State<MessageType>>> sets = getAllSets(to_merge.values());
    boolean changed = mergeEachSet(automaton, sets);
    return changed;
  }

  public List<MessageType> convertSessionToSequenceOfMsgTypes(List<Message> session)
      throws UnknownMessageTypeException {
    int nth_message = 0;
    List<MessageType> sequence = new ArrayList<MessageType>();
    for (Message m : session) {
      if (m.isInput()) { // Only process input messages.
        STATS_MESSAGES++;
        Collection<Transition<RegEx>> path_in_language = language.accepts(m);
        if (path_in_language != null) {
          MessageType msg_type = new LanguageMessageType(path_in_language);
          sequence.add(msg_type);
        } else
          throw new UnknownMessageTypeException(m.toString(), nth_message);
      }
      nth_message++;
    }
    return sequence;
  }

  // /////////////////////////////////////////////////////////////////////////////
  public static void printUsage(OptionsExtended options) {
    System.out
        .println("Usage: java StateMachineMoore [OPTIONS...] LANGUAGE STATEMACHINE \"expr1\" \"expr2\"");
    System.out.println();
    System.out.println("Creates an automaton that represents the protocol state machine "
        + "from messages taken from traces (text or pcap file) that are recognized by "
        + "the AUTOMATON message formats.");
    System.out.println();
    System.out.println("LANGUAGE\t\tlanguage file");
    System.out.println("STATEMACHINE\t\t output file");
    System.out.println("expr\t\tfilter expression to extract messages from pcap file");
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
    opt.setOption("--stateless=", "-s", "\tIf the server/protocol is stateless.");
    opt.setOption("--txt=", "-t", "FILE\tText file with a packet payload in each line");
    opt.setOption("--pcap=", "-p", "FILE\tPacket capture file in tcpdump format");
    opt.setOption("--sessions=", null, "FILE\tSessions object file");
    opt.setOption("--max=", "-m", "NUMBER\tMaximum number of messages to process");
    opt.setOption("--delim=", "-d", "Message delimiter (eg, \"\\r\\n\")");

    /* Check command-line parameters. */
    opt.parseArgs(args);
    try {

      /* Parse command-line parameters. */
      boolean stateless = opt.getValueBoolean("-s");
      // Check for message delimiter (for text-based protocols).
      String MSG_DELIMITER = opt.getValueString("--delim=");
      if (MSG_DELIMITER != null) {
        System.out.println("MSG_DELIMITER: " + MSG_DELIMITER);
        MSG_DELIMITER = Utils.toJavaString(MSG_DELIMITER);
        System.out.println("MSG_DELIMITER: " + MSG_DELIMITER);
      }
      String LANGUAGE = opt.getValueString();
      String OUTFILE = opt.getValueString();
      // Optional expression
      String EXPRESSION = (opt.getTotalRemainingArgs() > 0) ? opt.getValueString() : null;
      Automaton.DEBUG = true;

      int MAX = opt.getValueBoolean("-m") ? opt.getValueInteger("-m") : -1;

      /* Load inferred input languages. */
      Automaton<RegEx> lang = Automaton.loadFromFile(LANGUAGE);
      Language input_language = (Language)lang;
      State.NEXT_ID = 0;

      /* Load sessions (extracted previously from traces). */
      Collection<List<Message>> sessions = null;
      TracesInterface traces = null;
      if (opt.getValueBoolean("--txt=")) {
        traces = new TextFile(opt.getValueString("--txt="));
        traces.open();
        sessions = traces.getSessions(!stateless, MAX);
        traces.close();
      } else if (opt.getValueBoolean("--pcap=")) {
        traces = new PcapFile(opt.getValueString("--pcap="), EXPRESSION, null, MSG_DELIMITER);
        traces.open();
        sessions = traces.getSessions(!stateless, MAX);
        traces.close();
      } else if (opt.getValueBoolean("--sessions=")) {
        sessions = (Collection<List<Message>>)utils.Utils.readFromFile(opt
            .getValueString("--sessions="));
        if (opt.getValueBoolean("-m"))
          sessions = Sessions.trim(sessions, MAX);

      } else {
        throw new OptionsException(OptionsException.Types.MISSING_PARAMETER, "Missing traces file.");
      }

      /* Infer state machine of the protocol. */
      TIMER.restart();
      StateMachineMoore state_machine = new StateMachineMoore(input_language, sessions);
      System.out.println("[T] TOTAL TIME:\t" + TIMER.getElapsedTime());
      state_machine.drawAutomaton(OUTFILE, false);
      Utils.saveToFile(state_machine, OUTFILE);

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

      /* Checking (original). */
      for (List<Message> session : sessions) {
        try {
          List<MessageType> inferred = state_machine.convertSessionToSequenceOfMsgTypes(session);
          if (!state_machine.acceptsPrefix(inferred)) {
            System.err.println("X message unrecognized: " + inferred);
            System.err.println("ERROR: !state_machine.accepts(inferred)");
            utils.Utils.sleep(10000);
            System.exit(1);
          }// else System.out.print(".");
        } catch (UnknownMessageTypeException e1) {
          System.err.println("X session not accepted: " + e1 + session.get(e1.nth_message));
          System.err.println("ERROR: UnknownMessageTypeException!");
          utils.Utils.sleep(10000);
          e1.printStackTrace();
          System.exit(1);
        }
      }
      System.out.println("All ok!");

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
