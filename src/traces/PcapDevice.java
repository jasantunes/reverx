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

import java.io.IOException;
import java.util.*;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapClosedException;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;

public class PcapDevice extends PcapFile {
  // Workaround to save sessions when CTRL-C
  // (Runtime.getRuntime().addShutdownHook())
  protected AbstractMap<Connection, List<Message>> sessions = null;

  public PcapDevice(String device, String expr, String server_addr, String delimiter_regex) {
    super(device, expr, server_addr, delimiter_regex);
    sessions = new TreeMap<Connection, List<Message>>(); // sorted
  }

  /**
   * Open network device for listening.
   */
  @Override
  public void open() throws PcapClosedException {
    int snaplen = 64 * 1024; // capture all packets, no truncation
    int flags = Pcap.MODE_PROMISCUOUS; // capture all packets
    int timeout = 0; // in milliseconds, 0 = forever
    StringBuilder errbuf = new StringBuilder();
    pcap = Pcap.openLive(filename, snaplen, flags, timeout, errbuf);
    if (pcap == null)
      throw new PcapClosedException("Error while opening device for capture: " + errbuf.toString());

    if (filter != null)
      setFilter(filter);
  }

  /**
   * Use "Runtime.getRuntime().addShutdownHook(new Thread() { public void run()
   * { saveSessions("file"); } }" to catch CTRL-C in order to stop this
   * functions.
   * 
   * @see getSessionsUntilCtrlC()
   */
  @Deprecated
  public Collection<List<Message>> getSessions() {
    loop();
    return sessions.values();
  }

  /**
   * Stops reading packets from network device (breaks cycle in loop()).
   */
  public void breakLoop() {
    pcap.breakloop();
  }

  /**
   * Saves captured sessions (ArrayList<List<Message>>) to file.
   * 
   * @param filename
   * @throws IOException
   */
  public void saveSessions(String filename) throws IOException {
    ArrayList<List<Message>> to_save = new ArrayList<List<Message>>(sessions.values());
    utils.Utils.saveToFile(to_save, filename);
  }

  /*
   * getNextPacket() on live network devices has a bug. So we have to use the
   * loop method instead of the iterative getNextPacket().
   */
  public void loop() {
    PcapPacketHandler<AbstractMap<Connection, List<Message>>> handler = new PcapPacketHandler<AbstractMap<Connection, List<Message>>>() {
      public void nextPacket(PcapPacket packet, AbstractMap<Connection, List<Message>> sessions) {
        // Get message from packet.
        Message m = toMessage(packet);
        if (m == null)
          return;

        List<Message> session = sessions.get(last_connection);
        if (session == null) {
          session = new ArrayList<Message>(30);
          sessions.put(last_connection, session);
        }
        session.add(m);
        System.out.println("[ ] " + last_connection + ": " + session);
      }
    };

    pcap.loop(Pcap.LOOP_INFINATE, handler, sessions);

  }

}
