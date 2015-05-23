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

package traces;

import java.io.*;
import java.util.*;

public class TextFile implements TracesInterface {
  private BufferedInputStream _input;
  private byte[] _buffer = new byte[1000000];
  private String _filename;

  public TextFile(String filename) {
    _filename = filename;
  }

  public void open() throws FileNotFoundException {
    _input = new BufferedInputStream(new FileInputStream(_filename));
  }

  public void close() {
    if (_input != null)
      try {
        _input.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
  }

  @Override
  public void finalize() throws Throwable {
    close();
  }

  public Message getNextPacket() {
    try {
      int i = 0;
      while (_input.available() > 0) {
        byte b = (byte)_input.read();
        _buffer[i++] = b;
        if (b == '\n')
          break;
      }

      if (i == 0)
        return null;

      byte[] arr = new byte[i];
      System.arraycopy(_buffer, 0, arr, 0, i);
      return new Message(arr, true);

    } catch (FileNotFoundException ex) {
      ex.printStackTrace();
    } catch (IOException ex) {
      ex.printStackTrace();
    }

    return null;

  }

  public boolean skip(int messages_to_skip) {
    while (messages_to_skip-- > 0) {
      if (getNextPacket() == null)
        return false;
    }
    return true;
  }

  public Collection<List<Message>> getSessions(boolean stateful_protocol) {
    return getSessions(stateful_protocol, -1);
  }

  /**
   * Extracts application sessions from the traces. A negative sample_size means
   * unrestrictive sample_size, which is the same as calling
   * getSessions(stateful_protocol,-1).
   */
  public Collection<List<Message>> getSessions(boolean stateful_protocol, int sample_size) {
    Collection<List<Message>> sessions = new ArrayList<List<Message>>();
    List<Message> last_session = new ArrayList<Message>();
    sessions.add(last_session);
    Message m = null;
    while (sample_size-- != 0 && (m = getNextPacket()) != null) {
      System.out.println("getting m " + sample_size);
      // If message has just a EOL, new session
      if (m.getByteArray().length == 1) {
        last_session = new ArrayList<Message>();
        sessions.add(last_session);
      } else
        last_session.add(m);
    }
    return sessions;
  }

}
