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

package bioinformatics;

import automata.Symbol;

public class Cell {
  public enum DIR {
    DIAG, HORIZ, VERT
  }

  protected int value = 0;
  // Cell from which the value was calculated from (to backtrack alignment).
  protected Cell next_ref;
  protected DIR dir;

  protected Symbol _symbol;

  public Cell() {
    this(0, null, null, null);
  }

  public Cell(int value, DIR dir, Cell from, Symbol node) {
    this.value = value;
    this.dir = dir;
    this.next_ref = from;
    this._symbol = node;
  }

  // public static Cell[] mergeBestCells(Cell[] scores0, Cell[] scores1) {
  // for (int i = 0; i < scores0.length; i++)
  // scores0[i] = (scores1[i].globalSimilarity() >
  // scores0[i].globalSimilarity()) ? scores1[i]
  // : scores0[i];
  // return scores0;
  // }

  @Override
  public String toString() {
    return new Float(value).toString();
  }

  public int Size() {
    int size = 1;
    Cell curr = this;
    while ((curr = curr.next_ref) != null)
      size++;
    return size;

  }

  public float globalSimilarity() {
    return value / (Size() - 1);
  }

}
