import java.io.IOException;
import java.util.*;
import org.junit.Test;
import traces.Message;
import traces.PcapFile;
import automata.*;

public class StateMachineTest {

  public static ArrayList<MessageType> createSequence(String list_of_tokens) {
    String[] tokens = list_of_tokens.split(" ");
    ArrayList<MessageType> sequence = new ArrayList<MessageType>(tokens.length);
    for (int i = 0; i < tokens.length; i++)
      sequence.add(new RegEx(tokens[i]));
    return sequence;
  }

  @Test
  public void testReduce() {
    try {
      Automaton<MessageType> automaton = new Automaton<MessageType>();
      automaton.addSequence(createSequence("A B C D E"));
      automaton.addSequence(createSequence("A X C F"));
      Operations.minimization(automaton);
      automaton.drawAutomaton("test/reduce0", true);

      StateMachineMoore.reduce(automaton);
      automaton.drawAutomaton("test/reduce1", true);

    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println("DONE!");

  }

  @Test
  public void testInferFromTraces() {
    /* Parameters. */
    String traces_filename = "test/traces.pcap";
    String input_language_filename = "test/1-inlang";
    String output_language_filename = "test/2-outlang";
    String output_state_machine_filename = "test/3-statemachine";
    float T1 = 0.3f;
    int T2 = 30;

    try {
      /* Extract sessions. */
      PcapFile traces = new PcapFile(traces_filename, "port 21", "*:21", null);
      traces.open();
      System.out.println("[ ] sessions = traces.getSessions();");
      Collection<List<Message>> sessions = traces.getSessions(true);
      traces.close();

      /* Infer input language. */
      System.out.println("[ ] input_language.inferFromTraces(true, sessions, " + T1 + ", " + T2
          + ");");
      Language input_language = new Language(true, sessions, T1, T2);
      input_language.drawAutomaton(input_language_filename, false);

      /* Infer output language. */
      State.NEXT_ID = 0;
      System.out.println("[ ] output_language.inferFromTraces(false, sessions, " + T1 + ", " + T2
          + ");");
      Language output_language = new Language(false, sessions, T1, T2);
      output_language.drawAutomaton(output_language_filename, false);

      /* Infer state machine. */
      StateMachineMealy state_machine = new StateMachineMealy(input_language, output_language,
          sessions);
      state_machine.drawAutomaton(output_state_machine_filename, false);

    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    System.out.println("DONE!");
  }

  public static void main(String[] args) {
    StateMachineTest s = new StateMachineTest();
    s.testInferFromTraces();
  }

}
