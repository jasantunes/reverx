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

public class StateMachineMealy extends StateMachineMoore implements java.io.Serializable {
  protected static final long serialVersionUID = StateMachineMoore.serialVersionUID;
  protected Language output_language;

  public StateMachineMealy(Language l_input, Language l_output, Collection<List<Message>> sessions)
      throws UnknownMessageTypeException {
    language = l_input;
    output_language = l_output;
    infer(sessions);
  }

  @Override
  public List<MessageType> convertSessionToSequenceOfMsgTypes(List<Message> session)
      throws UnknownMessageTypeException {
    List<MessageType> sequence = new ArrayList<MessageType>();
    List<LanguageMessageType> input = new ArrayList<LanguageMessageType>();
    List<LanguageMessageType> output = new ArrayList<LanguageMessageType>();
    int nth_message = 0;
    /* Process each message of a session. */
    boolean look_for_output = false;
    for (Message m : session) {
      STATS_MESSAGES++;

      /* Input message. */
      if (m.isInput()) {
        // System.out.println("> " + m.toString());
        if (look_for_output) {
          // We have a complete <input,output> symbol.
          sequence.add(new IOLanguageMessageType(input, output));
          input = new ArrayList<LanguageMessageType>();
          output = new ArrayList<LanguageMessageType>();
          look_for_output = false;
        }

        Collection<Transition<RegEx>> path_in_language = language.accepts(m);
        if (path_in_language != null) {
          LanguageMessageType msg_type = new LanguageMessageType(path_in_language);
          input.add(msg_type);
        } else
          throw new UnknownMessageTypeException(m.toString(), nth_message);
      }

      /* Output message. */
      else {
        // System.out.println("< " + m.toString());
        look_for_output = true;
        Collection<Transition<RegEx>> path_in_language = output_language.accepts(m);
        if (path_in_language != null) {
          LanguageMessageType msg_type = new LanguageMessageType(path_in_language);
          output.add(msg_type);
        } else
          throw new UnknownMessageTypeException(m.toString(), nth_message);
      }

      nth_message++;
    }

    /* Add last <input,output> symbol. */
    if (!input.isEmpty() || !output.isEmpty())
      sequence.add(new IOLanguageMessageType(input, output));

    return sequence;
  }

  // /////////////////////////////////////////////////////////////////////////////
  public static void printUsage(OptionsExtended options) {
    System.out
        .println("Usage: java StateMachineMealy [OPTIONS...] LANG1 LANG2 STATEMACHINE IP:PORT [\"expr\"]");
    System.out.println();
    System.out.println("Creates an automaton that represents the protocol state machine "
        + "from messages taken from traces (text or pcap file) that are recognized by "
        + "the AUTOMATON message formats.");
    System.out.println();
    System.out.println("LANG1\t\tinput language file (client)");
    System.out.println("LANG2\t\toutput language file (server)");
    System.out.println("IP:PORT\t\tfilter output messages by the address (*:* = ANY)");
    System.out.println("STATEMACHINE\t\t output file");
    System.out.println("expr\t\tfilter expression to extract messages from pcap file (optional)");
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
    TIMER = new Timer();

    /* Global option for printing textual protocols / binary. */
    // LanguageMessageType.setTextualProtocol(true);

    // main_debug(args); System.exit(0);
    OptionsExtended opt = new OptionsExtended();
    opt.setOption("--txt=", "-t", "FILE\ttext file with a packet payload in each line");
    opt.setOption("--pcap=", "-p", "FILE\tpacket capture file in tcpdump format");
    opt.setOption("--sessions=", null, "FILE\tSessions object file");
    opt.setOption("--max=", "-m", "NUMBER\tmaximum number of messages to process");
    opt.setOption("--delim=", "-d", "message delimiter (eg, \"\\r\\n\")");
    opt.setOption("--stateless=", "-s", "\tif the protocol is stateless");

    /* Check command-line parameters. */
    opt.parseArgs(args);
    try {

      /* Parse command-line parameters. */
      boolean stateless = opt.getValueBoolean("-s");
      int MAX = opt.getValueBoolean("-m") ? opt.getValueInteger("-m") : -1;

      // Check for message delimiter (for text-based protocols).
      String MSG_DELIMITER = opt.getValueString("--delim=");
      if (MSG_DELIMITER != null) {
        System.out.println("MSG_DELIMITER: " + MSG_DELIMITER);
        MSG_DELIMITER = Utils.toJavaString(MSG_DELIMITER);
        System.out.println("MSG_DELIMITER: " + MSG_DELIMITER);
      }
      String LANG1 = opt.getValueString();
      String LANG2 = opt.getValueString();
      String OUTFILE = opt.getValueString();
      // Optional
      String SERVER_ADDR = (opt.getTotalRemainingArgs() > 0) ? opt.getValueString() : null;
      String EXPRESSION = (opt.getTotalRemainingArgs() > 0) ? opt.getValueString() : null;
      Automaton.DEBUG = true;

      /* Load inferred input languages. */
      Language input_language = (Language)Utils.readFromFile(LANG1);
      Language output_language = (Language)Utils.readFromFile(LANG2);

      /* Load sessions (extracted previously from traces). */
      Collection<List<Message>> sessions = null;
      TracesInterface traces = null;

      // Packet capture files.
      if (opt.getValueBoolean("--pcap=")) {
        traces = new PcapFile(opt.getValueString("--pcap="), EXPRESSION, SERVER_ADDR, MSG_DELIMITER);
        traces.open();
        sessions = traces.getSessions(!stateless, MAX);
        traces.close();
      }

      // Cached sessions.
      else if (opt.getValueBoolean("--sessions=")) {
        sessions = (Collection<List<Message>>)utils.Utils.readFromFile(opt
            .getValueString("--sessions="));
        if (opt.getValueBoolean("-m"))
          sessions = Sessions.trim(sessions, MAX);

      }

      // Text files (DEBUG).
      else if (opt.getValueBoolean("--txt=")) {
        traces = new TextFile(opt.getValueString("--txt="));
        traces.open();
        sessions = traces.getSessions(!stateless, MAX);
        traces.close();
      }

      // ERROR!
      else {
        throw new OptionsException(OptionsException.Types.MISSING_PARAMETER, "Missing traces file.");
      }

      /* Infer state machine of the protocol. */
      TIMER.restart();
      StateMachineMealy state_machine = new StateMachineMealy(input_language, output_language,
          sessions);
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
