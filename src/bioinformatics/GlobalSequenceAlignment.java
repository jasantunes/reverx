/*****************************************************************************
 * [Simplified BSD License]
 *
 * Copyright 2011 Joao Antunes. All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED BY JOAO ANTUNES ''AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL JOAO ANTUNES OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of Joao Antunes.
 *****************************************************************************/

package bioinformatics;

import java.util.ArrayList;
import automata.Symbol;

public class GlobalSequenceAlignment {
  public static final int INDEL_PENALTY = 0; // gap penalty (<=0)

  public static class Alignment {

    public ArrayList<Symbol> seq0, seq1;
    private float similarity;

    @Override
    public String toString() {
      return "\t" + seq0 + "\n\t" + seq1;
    }

  }

  public static class SymbolByte implements Symbol {
    private byte symbol;

    public SymbolByte(byte symbol) {
      this.symbol = symbol;
    }

    @Override
    public int hashCode() {
      return (int)symbol;
    }

    @Override
    public boolean equals(Object obj) {
      return (this.symbol == ((SymbolByte)obj).symbol);
    }

    public static SymbolByte[] toArray(CharSequence sequence) {
      SymbolByte[] seq = new SymbolByte[sequence.length()];
      for (int i = 0; i < seq.length; i++)
        seq[i] = new SymbolByte((byte)sequence.charAt(i));
      return seq;
    }

    @Override
    public String toString() {
      return "" + (char)symbol;
    }

    public String toDot() {
      return toString();
    }

    @Override
    public Object clone() {
      return new SymbolByte(this.symbol);
    }

  }

  public static Alignment getAlignment(Symbol[] sequence, Symbol[] new_sequence) {
    Cell[][] matrix = align(sequence, new_sequence);
    return GlobalSequenceAlignment._get_alignment(matrix[0][0], new_sequence);
  }

  public static Cell[] calculateReverseRow(Symbol node, Symbol[] new_sequence, Cell[] last_row) {
    Cell[] new_row = new Cell[new_sequence.length + 1];
    new_row[new_row.length - 1] = new Cell(0, Cell.DIR.VERT, last_row[last_row.length - 1], node);

    int i = new_row.length - 1; // from last position

    while (i-- > 0) {
      int diag = last_row[i + 1].value + (node.equals(new_sequence[i]) ? 1 : 0);
      int left = new_row[i + 1].value + INDEL_PENALTY;
      int up = last_row[i].value + INDEL_PENALTY;

      if (diag >= left && diag >= up)
        new_row[i] = new Cell(diag, Cell.DIR.DIAG, last_row[i + 1], node);

      else if (left >= diag && left >= up)
        new_row[i] = new Cell(left, Cell.DIR.HORIZ, new_row[i + 1], new_sequence[i]);

      else
        new_row[i] = new Cell(up, Cell.DIR.VERT, last_row[i], node);

    }

    return new_row;

  }

  private static Cell[][] align(Symbol[] sequence, Symbol[] new_sequence) {
    Cell[] scores = new Cell[new_sequence.length + 1];
    for (int i = 0; i < scores.length; i++)
      scores[i] = new Cell();
    Cell[][] matrix = new Cell[sequence.length + 1][];
    matrix[sequence.length] = scores;

    // from last
    for (int i = sequence.length - 1; i >= 0; i--) {
      scores = GlobalSequenceAlignment.calculateReverseRow(sequence[i], new_sequence, scores);
      matrix[i] = scores;
    }

    return matrix;
  }

  private static Alignment _get_alignment(Cell path, Symbol[] new_sequence) {
    Alignment alignment = new Alignment();
    alignment.similarity = path.globalSimilarity();
    alignment.seq0 = new ArrayList<Symbol>(new_sequence.length);
    alignment.seq1 = new ArrayList<Symbol>(new_sequence.length);

    // Note: matrix calculation was reversed (optimized),
    // hence the first cell corresponds to the first node
    int i = 0;
    for (; path != null; path = path.next_ref) {

      // automaton: x (aligned)
      // sequence: y (aligned)
      if (path.dir == Cell.DIR.DIAG) {
        alignment.seq0.add(path._symbol);
        alignment.seq1.add(new_sequence[i++]);
      }
      // automaton: x
      // sequence: - (gap)
      else if (path.dir == Cell.DIR.VERT) {
        alignment.seq0.add(path._symbol);
        alignment.seq1.add(null);
      }
      // automaton: - (gap)
      // sequence: y
      else if (path.dir == Cell.DIR.HORIZ) {
        alignment.seq0.add(null);
        alignment.seq1.add(new_sequence[i++]);
      }
    }

    while (i < new_sequence.length) {
      alignment.seq0.add(null);
      alignment.seq1.add(new_sequence[i++]);
    }
    // alignment.similarity /= alignment.seq0.size();
    return alignment;
  }

  public static float getSimilarityScore(Cell[][] matrix) {
    return matrix[0][0].globalSimilarity();
    // float num = matrix[0][0].value;
    // float den = Math.max(matrix.length, matrix[0].length)-1;
    // // return (float) matrix[0][0].value/(Math.max(matrix.length,
    // matrix[0].length)-1);
    // return num/den;
  }

  public static void main(String[] args) {
    try {
      String col = "250 User clark logged in.";
      String line = "250 User peter logged in.";

      SymbolByte[] sequence = SymbolByte.toArray(col);
      SymbolByte[] new_sequence = SymbolByte.toArray(line);
      Cell[][] matrix;
      matrix = GlobalSequenceAlignment.align(sequence, new_sequence);

      // GlobalSequenceAlignment.getAlignment(scores[0], sequence);
      Alignment alignment = GlobalSequenceAlignment._get_alignment(matrix[0][0], new_sequence);
      // String[] alignment =
      // GlobalSequenceAlignment.getAlignments(matrix, nodes, sequence);

      System.out.println("A: " + GlobalSequenceAlignment.getSimilarityScore(matrix));
      System.out.println("B: " + alignment.similarity);
      System.out.println(alignment.seq0);
      System.out.println(alignment.seq1);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
