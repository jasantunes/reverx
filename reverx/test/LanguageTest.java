import java.util.Collection;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import traces.*;
import automata.*;

public class LanguageTest {

  private Automaton<RegEx> automaton = null;

  private static Transition<RegEx> addTransition(State<RegEx> s, String text, State<RegEx> dest) {
    Transition<RegEx> t = new Transition<RegEx>(new RegEx(new ByteChars(text.getBytes())), dest);
    s.getTransitions().add(t);
    return t;
  }

  private static Transition<RegEx> addTransition(State<RegEx> s, Transition<RegEx> t) {
    s.getTransitions().add(t);
    return t;
  }

  private State<RegEx> createState(int id) {
    State.NEXT_ID = id;
    State<RegEx> s = new State<RegEx>();
    automaton.getAllStates().add(s);
    return s;
  }

  @Before
  public void setUp() throws Exception {
    automaton = new Automaton<RegEx>();

    State<RegEx> s0 = automaton.getInitialState();

    State<RegEx> s780 = createState(780);
    State<RegEx> s781 = createState(781);
    State<RegEx> s782 = createState(782);
    State<RegEx> s783 = createState(783);
    State<RegEx> s537 = createState(537);

    State<RegEx> s525 = createState(525);
    State<RegEx> s526 = createState(526);
    State<RegEx> s527 = createState(527);
    State<RegEx> s528 = createState(528);
    State<RegEx> s529 = createState(529);
    State<RegEx> s530 = createState(530);

    State<RegEx> s1027 = createState(1027);
    State<RegEx> s1028 = createState(1028);
    State<RegEx> s580 = createState(580);
    State<RegEx> s1 = createState(1);

    addTransition(s0, "path", s1027);
    addTransition(s0, "user2", s780);
    addTransition(s0, "user3", s525);

    addTransition(s1027, "user1", s1028);
    addTransition(s1028, "@", s526);

    Transition<RegEx> right = addTransition(s780, ">", s580);
    addTransition(s580, " CRLF", s1);

    addTransition(s525, "@", s526);
    addTransition(s526, "server", s527);
    addTransition(s527, ".", s528);
    Transition<RegEx> lasige = addTransition(s528, "lasige", s529);
    addTransition(s529, ".", s530);
    Transition<RegEx> pt = addTransition(s530, "pt", s537);

    addTransition(s780, "@", s781);
    addTransition(s781, "server", s782);
    addTransition(s782, ".", s783);
    addTransition(s783, pt);
    addTransition(s537, right);

    addTransition(s783, lasige);

    automaton.DRAW("test/LanguageTest0", false);

  }

  @Test
  public void concatUniqueLinearTransitionsTest() {
    Language.concatUniqueLinearStates(automaton);
    automaton.DRAW("test/LanguageTest1", false);
  }

  @Test
  public void inferFromTracesTest() {
    /* Parameters: input */
    String traces_filename = "test/traces.pcap";
    float T1 = 0.5f;
    int T2 = 30;
    /* Parameters: output */
    String input_lang = "test/in.lang";
    String output_lang = "test/out.lang";

    try {
      /* Extract sessions. */
      PcapFile traces = new PcapFile(traces_filename, "port 21", "*:21", null);
      traces.open();
      Collection<List<Message>> sessions = traces.getSessions(true);
      traces.close();

      /* Infer input language. */
      Language input_language = new Language(true, sessions, T1, T2);
      input_language.drawAutomaton(input_lang, false);

      /* Infer output language. */
      // State.NEXT_ID = 0;
      Language output_language = new Language(false, sessions, T1, T2);
      output_language.drawAutomaton(output_lang, false);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
