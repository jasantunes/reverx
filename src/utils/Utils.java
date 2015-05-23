/*******************************************************************************
 * Copyright 2011 Joao Antunes
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package utils;

import java.io.*;
import java.util.ArrayList;

/**
 * Static utility methods.
 */
public class Utils {

  public static final long serialVersionUID = 4;

  public static void saveToFile(Object o, String filename) throws IOException {
    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename));
    out.writeObject(o);
    out.close();
  }

  public static Object readFromFile(String filename) throws IOException, ClassNotFoundException {
    ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename));
    Object o = in.readObject();
    in.close();
    return o;
  }

  public static double[] trimArray(double[] data, int length) {
    double[] new_data = new double[length];
    System.arraycopy(data, 0, new_data, 0, length);
    return new_data;
  }

  /**
   * Returns a random value where min<=value<max.
   */
  public static int random(int min, int max) {
    int diff = max - min;
    int r = (int)(Math.random() * diff); // int between 0 and diff-1
    return r + min;
  }

  /**
   * Returns a random value where min<=value<max.
   */
  public static long random(long min, long max) {
    long diff = max - min;
    long r = (long)(Math.random() * diff); // int between 0 and diff-1
    return r + min;
  }

  public static void handleException(Exception e, String message) {
    e.printStackTrace();
    if (message != null)
      System.err.println(message);
    else
      System.err.println(e);
  }

  public static void handleException(Exception e) {
    handleException(e, null);
  }

  public static void handleExceptionAndExit(Exception e, String message) {
    handleException(e, message);
    System.exit(-1);
  }

  public static void handleExceptionAndExit(Exception e) {
    handleException(e, null);
    System.exit(-1);
  }

  public static void sleep(int milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException e) {
    }
  }

  public static void pause(String message) {
    System.err.println(message);
    pause();
  }

  public static void pause() {
    System.err.print("Press <ENTER> to continue");
    try {
      System.in.read();
    } catch (Exception e) {
    }
  }

  public static String getClassName(Object o) {
    return o.getClass().getSimpleName();
  }

  public static ArrayList<String> getFileContents(String filename) throws IOException {
    if ((filename == null) || (filename == ""))
      throw new IOException();

    String line;
    ArrayList<String> file = new ArrayList<String>();
    BufferedReader in = new BufferedReader(new FileReader(filename));

    if (!in.ready()) {
      in.close();
      throw new IOException();
    }
    while ((line = in.readLine()) != null)
      if (line.length() != 0)
        file.add(line);
    in.close();
    return file;
  }

  /**
   * Returns the value if min<value<max. Otherwise it returns either the closest
   * to value from min or max.
   */
  public static int inBetween(int min, int value, int max) {
    if (value < min)
      return min;
    else if (value > max)
      return max;
    else
      return value;
  }

  public static final String[] REPLACE_LIST = {
      "\t", "\\\\t", "\n", "\\\\n", "\r", "\\\\r",
  };

  /**
   * Changes a string with newlines (\n) and other special character to \\n and
   * equivalent representations. (see array of replacements String[]
   * REPLACE_LIST)
   */
  public static String fromJavaString(final String s) {
    String new_s = new String(s);
    for (int i = 0; i < REPLACE_LIST.length; i += 2)
      new_s = new_s.replaceAll(REPLACE_LIST[i], REPLACE_LIST[i + 1]);
    return new_s;
  }

  /**
   * Changes a string with \\n and other similar representation of special
   * characters to actual special characters, such as newlines (\n), tabs (\t),
   * and equivalent. (see array of replacements String[] REPLACE_LIST)
   */
  public static String toJavaString(final String s) {
    String new_s = new String(s);
    for (int i = 0; i < REPLACE_LIST.length; i += 2)
      new_s = new_s.replaceAll(REPLACE_LIST[i + 1], REPLACE_LIST[i]);
    return new_s;
  }

  /**
   * Execute a local Bash script.
   * 
   * @param args Argument list. The first argument is the actual script file,
   *          while the remaining elements correspond to the respective
   *          command-line parameters.
   * @return Returns BufferedReader to get the output of the script.
   */
  public static BufferedReader executeBashScript(String[] args) throws Exception {
    Runtime run = Runtime.getRuntime();
    Process pr = run.exec(args);
    pr.waitFor();
    return new BufferedReader(new InputStreamReader(pr.getInputStream()));
  }

}
