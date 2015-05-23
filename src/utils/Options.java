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

import java.util.ArrayList;

public class Options {

  public static class Opt {
    String opt;
    String opt_short;
    String value;
    boolean is_set;

    Opt(String opt, String opt_short) {
      this.opt = opt;
      this.opt_short = (opt_short != null) ? opt_short : "";
      is_set = false;
    }

    public boolean equals(String option) {
      return (option != null && ((opt.compareTo(option) == 0) || opt_short.compareTo(option) == 0));
    }
  }

  private ArrayList<Opt> _options;
  private String[] _remaining_args;

  public Options() {
    _options = new ArrayList<Opt>();
    _remaining_args = new String[0];
  }

  public void setOption(String opt, String opt_short) {
    _options.add(new Opt(opt, opt_short));
  }

  public void setArgs(String[] args) {
    ArrayList<String> remaining = new ArrayList<String>();

    for (int i = 0; i < args.length; i++) {
      String arg = args[i];

      boolean found = false;
      for (Opt option : _options) {

        if (option.equals(arg)) {
          option.is_set = true;

          /* if args have "value" for that "option" */
          if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
            i++;
            option.value = args[i];
          }

          found = true;
          break;
        }
      }
      if (!found)
        remaining.add(arg);

    }
    _remaining_args = remaining.toArray(new String[remaining.size()]);
  }

  public String[] getRemainingArgs() {
    return _remaining_args;
  }

  public static String concatArgs(String[] args, int first, int last) {
    String expression = "";
    for (int i = first; i < last; i++)
      expression += args[i] + " ";
    return expression;
  }

  private Opt getOpt(String opt) {
    for (Opt option : _options)
      if (option.equals(opt))
        return option;
    return null;
  }

  public String getValueString(String opt) {
    Opt option = getOpt(opt);
    if (option != null)
      return option.value;
    else
      return null;
  }

  public int getValueInteger(String opt) throws OptionsException {
    try {
      return new Integer(getValueString(opt));
    } catch (Exception e) {
      throw new OptionsException(OptionsException.Types.INCORRECT_PARAMETER, opt
          + " not an integer");
    }
  }

  public float getValueFloat(String opt) throws OptionsException {
    try {
      return new Float(getValueString(opt));
    } catch (Exception e) {
      throw new OptionsException(OptionsException.Types.INCORRECT_PARAMETER, opt
          + " not an integer");
    }
  }

  public boolean getValueBoolean(String opt) {
    Opt option = getOpt(opt);
    if (option != null)
      return option.is_set;
    else
      return false;
  }

  public boolean stripArg(String arg) {
    String[] new_remaining_args = new String[_remaining_args.length - 1];
    boolean found = false;

    int i = 0;
    for (String s : _remaining_args) {
      if (!s.equals(arg))
        new_remaining_args[i++] = s;
      else
        found = true;
    }
    _remaining_args = new_remaining_args;
    return found;
  }

  @Override
  public String toString() {
    StringBuffer s = new StringBuffer();
    for (Opt o : _options) {
      s.append(o.opt + ((o.opt_short != null) ? " (" + o.opt_short + ")" : "") + "\n");
      s.append("\t" + ((o.is_set) ? "true (" + o.value + ")" : "false") + "\n");
    }

    s.append("Non parsed arguments:\n\t");
    s.append(getRemainingArgs());
    return new String(s);
  }

  // public static boolean equal(String string1, String string2) {
  // return (string1 != null && string2 != null && string1.compareTo(string2) ==
  // 0);
  // }

}
