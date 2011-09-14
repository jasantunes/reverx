
package automata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.Test;

public class OperationsTest {

  /**
   * From book's example.
   */
  public static Automaton<RegEx> createBooksAutomaton() {
    State.NEXT_ID = 0;
    Automaton<RegEx> automaton = new Automaton<RegEx>();

    // States.
    State<RegEx> q0 = automaton._initial_state;
    State<RegEx> q1 = new State<RegEx>();
    State<RegEx> q2 = new State<RegEx>();
    State<RegEx> q3 = new State<RegEx>();
    State<RegEx> q4 = new State<RegEx>();
    State<RegEx> q5 = new State<RegEx>();
    automaton._all_states.add(q1);
    automaton._all_states.add(q2);
    automaton._all_states.add(q3);
    automaton._all_states.add(q4);
    automaton._all_states.add(q5);
    q0._is_final = true;
    q4._is_final = true;
    q5._is_final = true;

    // Transitions from q0.
    q0._transitions.add(new Transition<RegEx>(new RegEx("a"), q2));
    q0._transitions.add(new Transition<RegEx>(new RegEx("b"), q1));

    // Transitions from q1.
    q1._transitions.add(new Transition<RegEx>(new RegEx("a"), q1));
    q1._transitions.add(new Transition<RegEx>(new RegEx("b"), q0));

    // Transitions from q2.
    q2._transitions.add(new Transition<RegEx>(new RegEx("a"), q4));
    q2._transitions.add(new Transition<RegEx>(new RegEx("b"), q5));

    // Transitions from q3.
    q3._transitions.add(new Transition<RegEx>(new RegEx("a"), q5));
    q3._transitions.add(new Transition<RegEx>(new RegEx("b"), q4));

    // Transitions from q4.
    q4._transitions.add(new Transition<RegEx>(new RegEx("a"), q3));
    q4._transitions.add(new Transition<RegEx>(new RegEx("b"), q2));

    // Transitions from q5.
    q5._transitions.add(new Transition<RegEx>(new RegEx("a"), q2));
    q5._transitions.add(new Transition<RegEx>(new RegEx("b"), q3));

    return automaton;

  }

  public static ArrayList<RegEx> createSequence(String list_of_tokens) {
    String[] tokens = list_of_tokens.split(" ");
    ArrayList<RegEx> sequence = new ArrayList<RegEx>(tokens.length);
    for (int i = 0; i < tokens.length; i++)
      sequence.add(new RegEx(tokens[i]));
    return sequence;
  }

  @SuppressWarnings("all")
  @Test
  public void testMerge() {
    try {
      Automaton<RegEx> automaton = new Automaton<RegEx>();
      automaton.addSequence(createSequence("A B C X Y"));
      automaton.addSequence(createSequence("A D C F"));
      automaton.drawAutomaton("test/testMerge0", false);

      Transition<RegEx> A = automaton._initial_state.getTransition(new RegEx("A"));
      Transition<RegEx> B = A._dest_state.getTransition(new RegEx("B"));
      Transition<RegEx> C = B._dest_state.getTransition(new RegEx("C"));
      Transition<RegEx> D = A._dest_state.getTransition(new RegEx("D"));
      Transition<RegEx> C2 = D._dest_state.getTransition(new RegEx("C"));
      Transition<RegEx> F = C2._dest_state.getTransition(new RegEx("F"));

      // Make F point to B and merge D and B.
      automaton._all_states.remove(F._dest_state);
      F._dest_state = B._dest_state;
      automaton.drawAutomaton("test/testMerge1", true);
      Operations.merge(automaton, D._dest_state, B._dest_state,
          new HashMap<State<RegEx>, State<RegEx>>());
      automaton.drawAutomaton("test/testMerge2", true);

      System.out.println("DONE!");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testMinimization() {
    Automaton<RegEx> automaton = createBooksAutomaton();
    automaton.DRAW("test/example", false);
    Operations.minimization(automaton);
    automaton.DRAW("test/example-minimized", false);
  }
}
