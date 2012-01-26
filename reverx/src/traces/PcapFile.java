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

package traces;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jnetpcap.*;
import org.jnetpcap.nio.JBuffer;
import org.jnetpcap.nio.JMemory;
import org.jnetpcap.packet.JHeader;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.protocol.JProtocol;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.network.Ip6;
import org.jnetpcap.protocol.tcpip.Tcp;
import org.jnetpcap.protocol.tcpip.Udp;
import utils.Convert;

public class PcapFile implements TracesInterface {
  protected Pcap pcap;
  protected String filename;
  protected String filter; // to filter messages

  // Filter server messages (address and/or port).
  protected int server_addr; // 0 = any
  protected int protocol_port; // 0 = any

  // default is to extract the payload of tcp or udp.
  protected boolean payload_ip = false;

  protected Connection last_connection;

  private Pattern PATTERN_TEXT_DELIMITER;
  protected Message _fragment;

  protected PcapHeader header = new PcapHeader(JMemory.Type.POINTER);
  protected JBuffer buffer = new JBuffer(JMemory.Type.POINTER);
  private int snaplen = 0;

  public static class Connection implements Comparable<Connection> {
    private int ip, port;

    public Connection(int ip, int port) {
      this.ip = ip;
      this.port = port;
    }

    @Override
    public int hashCode() {
      return ip ^ port;
    }

    @Override
    public boolean equals(Object obj) {
      Connection c = (Connection)obj;
      return ip == c.ip && port == c.port;
    }

    @Override
    public String toString() {
      return "<" + ip + ":" + port + ">";
    }

    @Override
    public int compareTo(Connection o) {
      int res = ip - o.ip;
      if (res == 0)
        res = port - o.port;
      return res;
    }
  }

  public void setPayloadIp(boolean payload_ip) {
    this.payload_ip = payload_ip;
  }

  /*
   * Set the maximum bytes of payload to extract. Useful to extract only
   * fixed-size headers.
   */
  public void setSnaplen(int length) {
    snaplen = length;
  }

  public PcapFile(String file, String expr, String server_addr, String delimiter_regex) {
    filename = file;
    filter = expr;

    // Extract IP address and port.
    if (server_addr != null) {
      String ip_part = getIpString(server_addr);
      String port_part = getPortString(server_addr);
      this.server_addr = ip_part.equals("*") ? 0 : getIpInt(ip_part);
      this.protocol_port = port_part.equals("*") ? 0 : new Integer(port_part);
    } else {
      this.server_addr = 0;
      this.protocol_port = 0;
    }

    last_connection = null;
    if (delimiter_regex != null)
      PATTERN_TEXT_DELIMITER = Pattern.compile(delimiter_regex, Pattern.DOTALL | Pattern.MULTILINE);
  }

  public void open() throws PcapClosedException {
    StringBuilder errbuf = new StringBuilder();
    pcap = Pcap.openOffline(filename, errbuf);
    if (pcap == null)
      throw new PcapClosedException("Error while opening device for capture: " + errbuf.toString());
    if (filter != null)
      setFilter(filter);
  }

  protected void setFilter(String expression) throws PcapClosedException {
    System.out.println("Setting filter " + expression);
    PcapBpfProgram program = new PcapBpfProgram();
    int optimize = 1; // 0 = false
    int netmask = 0xFFFFFF00; // 255.255.255.0

    if (pcap.compile(program, expression, optimize, netmask) != Pcap.OK)
      throw new PcapClosedException(pcap.getErr());

    if (pcap.setFilter(program) != Pcap.OK)
      throw new PcapClosedException(pcap.getErr());
  }

  // public boolean skip(int messages_to_skip) {
  // while (messages_to_skip-- > 0) {
  // if (_pcap.nextEx(_header, _buffer) != 1)
  // return false;
  // }
  // return true;
  // }

  public Message getNextFragment(Message m) {
    /* Check if message should be split. */
    if (PATTERN_TEXT_DELIMITER != null) {

      Matcher matcher = PATTERN_TEXT_DELIMITER.matcher(m);
      if (matcher.find()) {
        boolean is_input = m.isInput();
        byte[] data = m.getByteArray();
        int end = matcher.end();
        if (end < data.length) {

          // Keep remaining of the message in _fragment.
          byte[] remaining_data = new byte[data.length - end];
          System.arraycopy(data, end, remaining_data, 0, data.length - end);
          _fragment = new Message(remaining_data, is_input);

          // New message fragment.
          byte[] new_data = new byte[end];
          System.arraycopy(data, 0, new_data, 0, end);
          m = new Message(new_data, is_input);

        } else {
          _fragment = null;
        }
      }
    }

    return m;
  }

  private static boolean trimToSnaplen(Message m, int snaplen) {
    if (snaplen > 0 && m.len > snaplen) {
      byte[] trimmed_buf = new byte[snaplen];
      System.arraycopy(m.buf, 0, trimmed_buf, 0, snaplen);
      m.buf = trimmed_buf;
      m.len = snaplen;
      return true;
    }
    return false;
  }

  public Message getNextPacket() {
    Message m = null;

    /* Check if there is some old fragment. */
    if (_fragment != null) {
      m = getNextFragment(_fragment);
      if (m != null) {
        trimToSnaplen(m, snaplen);
        return m;
      }
    }

    /* Get next message. */
    do {

      if (pcap.nextEx(header, buffer) != 1)
        return null; // if null, there are no more packets.
      PcapPacket packet = new PcapPacket(header, buffer);

      // To support loopback interface: 14th byte is the type field, which can
      // be at offset 12 or 14.
      if (buffer.getByte(14) == 8)
        packet.scan(JProtocol.SLL_ID);
      else
        packet.scan(JProtocol.ETHERNET_ID);

      m = toMessage(packet);

    } while (m == null);

    /* Check if message should be split. */
    m = getNextFragment(m);
    trimToSnaplen(m, snaplen);
    return m;
  }

  /**
   * Returns a Message with the contents of the packet payload. The payload can
   * be either TCP/UDP or IP.
   */
  public Message toMessage(PcapPacket packet) {
    // System.out.println("PcapFile.toMessage()");
    last_connection = null;
    // packet.scan(JProtocol.ETHERNET_ID); // use this outside pcap.loop()

    boolean is_input = true;
    JHeader header = null;
    Ip4 ip4_header = new Ip4();
    Ip6 ip6_header = new Ip6();
    int length = 0, offset = 0;

    int src_ip = 0, dst_ip = 0, src_port = 0, dst_port = 0;

    /* Get IP addresses. */
    if (packet.hasHeader(ip4_header)) {
      src_ip = ip4_header.sourceToInt();
      dst_ip = ip4_header.destinationToInt();
      header = ip4_header;
    } else if (packet.hasHeader(ip6_header)) {
      // get last 4 bytes.
      src_ip = Convert.toInteger(ip6_header.source(), 12, 4);
      dst_ip = Convert.toInteger(ip6_header.destination(), 12, 4);
      header = ip6_header;
    } else
      return null;

    // Check type of payload to extract.
    if (payload_ip == false) {
      Tcp tcp_header = new Tcp();
      Udp udp_header = new Udp();

      // Get TCP/UDP header.
      if (packet.hasHeader(tcp_header)) {
        src_port = tcp_header.source();
        dst_port = tcp_header.destination();
        header = tcp_header;
      } else if (packet.hasHeader(udp_header)) {
        src_port = udp_header.source();
        dst_port = udp_header.destination();
        header = udp_header;
      } else
        return null;

    }

    // Check direction of message (input or output).
    is_input = (protocol_port == 0 || protocol_port == dst_port)
        && (server_addr == 0 || server_addr == dst_ip);
    last_connection = (is_input) ? new Connection(src_ip, src_port) : new Connection(dst_ip,
        dst_port);

    /* Get payload. */
    offset = header.getPayloadOffset();
    length = header.getPayloadLength();

    // Ignore empty packet (eg, TCP handshake).
    if (length == 0)
      return null; // return getNextPacket();
    else
      return new Message(packet.getByteArray(offset, length), is_input);
  }

  @Override
  public void finalize() {
    close();
  }

  public void close() {
    if (pcap != null)
      pcap.close();
  }

  /**
   * Extracts application sessions from the traces. In stateful protocols each
   * session is a sequence of pairs (request,response) terminating at the last
   * message of the connection, whereas in stateless protocols each session
   * corresponds to a single pair of (request,response). For instance, in the
   * DNS protocol (stateless), the client may send several requests, however, it
   * will correspond to several sessions.
   */
  public Collection<List<Message>> getSessions(boolean is_stateful_protocol) {
    return getSessions(is_stateful_protocol, -1);
  }

  /**
   * Extracts application sessions from the traces. In stateful protocols each
   * session is a sequence of pairs (request,response) terminating at the last
   * message of the connection, whereas in stateless protocols each session
   * corresponds to a single pair of (request,response). For instance, in the
   * DNS protocol (stateless), the client may send several requests, however, it
   * will correspond to several sessions. A negative sample_size means
   * unrestrictive sample_size, which is the same as calling
   * getSessions(stateful_protocol,-1).
   */
  public Collection<List<Message>> getSessions(boolean is_stateful_protocol, int sample_size) {
    if (is_stateful_protocol) {
      // Use LinkedHashMap to maintain the same order.
      LinkedHashMap<Connection, List<Message>> sessions = new LinkedHashMap<Connection, List<Message>>();
      Message m = null;
      while ((m = getNextPacket()) != null && sample_size-- != 0) {
        List<Message> session = sessions.get(last_connection);
        if (session == null) {
          session = new ArrayList<Message>();
          sessions.put(last_connection, session);
        }
        session.add(m);
      }
      return sessions.values();
    } else {
      // eg, DNS. Split sessions at each single pair of (req,resp).
      ArrayList<List<Message>> sessions = new ArrayList<List<Message>>();
      List<Message> session = new ArrayList<Message>();
      Message m = null;
      boolean expecting_response = false;
      while (sample_size-- != 0 && (m = getNextPacket()) != null) {

        // Got a request but we were looking for a response, it's a new session.
        if (m.isInput() && expecting_response) {
          sessions.add(session);
          session = new ArrayList<Message>();
          expecting_response = false;
        }
        // Got a response, we should expect more responses.
        else if (m.isInput() == false)
          expecting_response = true;

        session.add(m);
      }
      if (!session.isEmpty())
        sessions.add(session);
      return sessions;
    }
  }

  public static int getIpInt(String address) {
    String host = address.substring(0);
    InetAddress addr;
    try {
      addr = InetAddress.getByName(host);
      if (addr != null)
        return Convert.toInt32(addr.getAddress());
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
    return 0;
  }

  public static String getIpString(String address) {
    int separator = address.indexOf(':');
    return address.substring(0, separator);
  }

  public static String getPortString(String address) {
    int separator = address.indexOf(':');
    return address.substring(separator + 1);
  }

}
