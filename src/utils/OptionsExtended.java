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

import java.util.*;

public class OptionsExtended {
  private static class Opt {
    String opt;
    String opt_short;
    String value;
    String description;

    Opt(String opt, String opt_short, String description) {
      this.opt = opt;
      this.opt_short = opt_short;
      this.description = description;
    }

    public boolean equals(String option) {
      return ((opt != null && (option.equals(opt) || option.startsWith(opt))) || (opt_short != null && option
          .equals(opt_short)));
    }

  }

  private ArrayList<Opt> _options;
  private Queue<String> _remaining_args;

  public OptionsExtended() {
    _options = new ArrayList<Opt>();
    _remaining_args = new LinkedList<String>();
  }

  /**
   * @param opt String option with the format --opt=param
   * @param opt_short String option with the format -opt_short param
   * @param mandatory True if this option is mandatory or optional otherwise.
   * @param args Number of arguments that follow this option (usually 0 or 1).
   */
  public void setOption(String opt, String opt_short, String description) {
    _options.add(new Opt(opt, opt_short, description));
  }

  private Opt getOption(String arg) {
    for (Opt option : _options)
      if (option.equals(arg))
        return option;
    return null;
  }

  public void parseArgs(String[] args) {
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];

      Opt option = getOption(arg);
      if (option != null) {
        // get 'value'
        {
          // --opt=value
          if (!arg.equals(option.opt) && arg.startsWith(option.opt))
            option.value = arg.substring(option.opt.length());

          // --opt value OR -opt_short value -> get next argument
          else if ((arg.equals(option.opt) || arg.equals(option.opt_short))
              && (i + 1 < args.length && getOption(args[i + 1]) == null))
            option.value = args[++i];
          else
            option.value = "";

        }
      } else
        _remaining_args.add(arg);

    }
  }

  public String[] getRemainingArgs() {
    return _remaining_args.toArray(new String[_remaining_args.size()]);
  }

  public int getTotalRemainingArgs() {
    return _remaining_args.size();
  }

  public String removeArg() throws OptionsException {
    if (_remaining_args.isEmpty())
      throw new OptionsException(OptionsException.Types.MISSING_PARAMETER);
    else
      return _remaining_args.remove();
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

  private static int str2int(String value) throws OptionsException {
    try {
      return new Integer(value);
    } catch (Exception e) {
      throw new OptionsException(OptionsException.Types.INCORRECT_PARAMETER, "'" + value
          + "' not an integer");
    }
  }

  private static float str2float(String value) throws OptionsException {
    try {
      return new Float(value);
    } catch (Exception e) {
      throw new OptionsException(OptionsException.Types.INCORRECT_PARAMETER, "'" + value
          + "' not a float");
    }
  }

  public String getValueString(String opt) {
    Opt option = getOpt(opt);
    if (option != null)
      return option.value;
    else
      return null;
  }

  public String getValueString() throws OptionsException {
    return removeArg();
  }

  public int getValueInteger() throws OptionsException {
    return str2int(removeArg());
  }

  public float getValueFloat() throws OptionsException {
    return str2float(removeArg());
  }

  public int getValueInteger(String opt) throws OptionsException {
    return str2int(getValueString(opt));
  }

  public float getValueFloat(String opt) throws OptionsException {
    return str2float(getValueString(opt));
  }

  public boolean getValueBoolean(String opt) {
    Opt option = getOpt(opt);
    return (option != null && option.value != null);
  }

  @Override
  public String toString() {
    StringBuffer s = new StringBuffer(256);
    s.append("Parsed arguments:\n");

    for (Opt o : _options) {
      String value = (o.value == null) ? "False" : (o.value.length() == 0 ? "True" : o.value);
      s.append("\t" + o.opt + "\t" + value + "\n");
    }

    s.append("Non parsed arguments:\n");
    int i = 0;
    for (String arg : _remaining_args)
      s.append("\targs[" + (i++) + "]=\t" + arg + "\n");
    return new String(s);
  }

  public String getUsageOptions() {
    StringBuffer s = new StringBuffer(100 * _options.size());
    for (Opt option : _options) {
      String line = null;
      if (option.opt_short != null && option.opt != null)
        line = "  " + option.opt_short + ", " + option.opt;
      else if (option.opt_short == null && option.opt != null)
        line = "  " + option.opt;
      else if (option.opt_short != null && option.opt == null)
        line = "  " + option.opt_short;

      // if (line.length() > 8)
      // line += "\n\t\t";
      // else
      // line += "\t";

      s.append(line + option.description + "\n");
    }

    return new String(s);
  }

  // public static boolean equal(String string1, String string2) {
  // return (string1 != null && string2 != null && string1.compareTo(string2)
  // == 0);
  // }

}
