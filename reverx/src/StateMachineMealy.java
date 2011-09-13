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

public class StateMachineMealy extends StateMachineMoore implements java.io.Serializable {
  protected static final long serialVersionUID = StateMachineMoore.serialVersionUID;
  protected Automaton<RegEx> output_language;

  public StateMachineMealy(Automaton<RegEx> input_language, Automaton<RegEx> output_language) {
    super(input_language);
    this.output_language = output_language;
  }

  public StateMachineMealy(Language input_language, Language output_language) {
    super(input_language);
    this.output_language = output_language.getAutomaton();
  }

  public StateMachineMealy(Automaton<RegEx> input_language, Automaton<RegEx> output_language,
      Collection<List<Message>> sessions) throws UnknownMessageTypeException {
    this(input_language, output_language);
    inferFromTraces(sessions);
  }

  @Override
  public List<MessageType> convertSessionToSequenceOfMsgTypes(List<Message> session)
      throws UnknownMessageTypeException {
    List<MessageType> sequence = new ArrayList<MessageType>();
    List<LanguageMessageType> input = new ArrayList<LanguageMessageType>();
    List<LanguageMessageType> output = new ArrayList<LanguageMessageType>();
    boolean look_for_output = false;

    /* Process each message of a session. */
    for (Message m : session) {

      // Input message.
      if (m.isRequest()) {
        System.out.println("> " + m.toString());
        if (look_for_output) {
          // We have a complete <input,output> symbol.
          sequence.add(new IOLanguageMessageType(input, output));
          input = new ArrayList<LanguageMessageType>();
          output = new ArrayList<LanguageMessageType>();
          look_for_output = false;
        }
        List<Transition<RegEx>> path_in_language = RegEx.getPath(this.language, m);
        if (path_in_language == null)
          throw new UnknownMessageTypeException("Unknown message: " + m.toString());
        LanguageMessageType msg_type = new LanguageMessageType(path_in_language);
        input.add(msg_type);
      }

      // Output message.
      else {
        System.out.println("< " + m.toString());
        look_for_output = true;
        List<Transition<RegEx>> path_in_language = RegEx.getPath(this.output_language, m);
        if (path_in_language == null)
          throw new UnknownMessageTypeException("Unknown message: " + m.toString());
        LanguageMessageType msg_type = new LanguageMessageType(path_in_language);
        output.add(msg_type);

      }
    }

    /* Add last <input,output> symbol. */
    if (!input.isEmpty() || !output.isEmpty())
      sequence.add(new IOLanguageMessageType(input, output));

    return sequence;
  }

  @SuppressWarnings("unchecked")
  public static void main(String[] args) {

    /* Global option for printing textual protocols / binary. */
    // LanguageMessageType.setTextualProtocol(true);

    // main_debug(args); System.exit(0);
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
      int PROTOCOL_PORT = opt.getValueInteger();
      String INPUT_LANG = opt.getValueString();
      String OUTPUT_LANG = opt.getValueString();
      String OUTFILE = opt.getValueString();
      Automaton.DEBUG = true;

      /* Load inferred input languages. */
      Automaton<RegEx> input_language = Automaton.loadFromFile(INPUT_LANG);
      Automaton<RegEx> output_language = Automaton.loadFromFile(OUTPUT_LANG);

      /* Load sessions (extracted previously from traces). */
      Collection<List<Message>> sessions = null;
      TracesInterface traces = null;
      if (opt.getValueBoolean("--txt=")) {
        traces = new TextFile(opt.getValueString("--txt="));
        traces.open();
        sessions = traces.getSessions(!stateless);
        traces.close();
      } else if (opt.getValueBoolean("--pcap=")) {
        traces = new PcapFile(opt.getValueString("--pcap="), "port " + PROTOCOL_PORT,
            PROTOCOL_PORT, MSG_DELIMITER);
        traces.open();
        sessions = traces.getSessions(!stateless);
        traces.close();
      } else if (opt.getValueBoolean("--sessions=")) {
        sessions = (Collection<List<Message>>)utils.Utils.readFromFile(opt
            .getValueString("--sessions="));
      } else {
        throw new OptionsException(OptionsException.Types.MISSING_PARAMETER, "Missing traces file.");
      }

      /* Infer state machine of the protocol. */
      StateMachineMealy state_machine = new StateMachineMealy(input_language, output_language);
      state_machine.inferFromTraces(sessions);
      state_machine.automaton.drawAutomaton(OUTFILE, false);
      Utils.saveToFile(state_machine, OUTFILE);

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
