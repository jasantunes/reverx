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

public class Print {

  // The Color Code: <ESC>[{attr};{fg};{bg}m
  // {attr} needs to be one of the following:
  // 0 Reset All Attributes (return to normal mode)
  // 1 Bright (usually turns on BOLD)
  // 2 Dim
  // 3 Underline
  // 5 Blink
  // 7 Reverse
  // 8 Hidden
  // {fg} needs to be one of the following:
  // 30 Black
  // 31 Red
  // 32 Green
  // 33 Yellow
  // 34 Blue
  // 35 Magenta
  // 36 Cyan
  // 37 White
  //
  // {bg} needs to be one of the following:
  // 40 Black
  // 41 Red
  // 42 Green
  // 43 Yellow
  // 44 Blue
  // 45 Magenta
  // 46 Cyan
  // 47 White

  public static String red(String text) {
    return "\u001b[0;31m" + text + "\u001b[m";
  }

  public static String green(String text) {
    return "\u001b[0;32m" + text + "\u001b[m";
  }

  public static String blue(String text) {
    return "\u001b[0;34m" + text + "\u001b[m";
  }

  public static String yellow(String text) {
    return "\u001b[0;33m" + text + "\u001b[m";
  }

  public static String bold(String text) {
    return "\u001b[1m" + text + "\u001b[m";
  }

  public static String underline(String text) {
    return "\u001b[3m" + text + "\u001b[m";
  }

}
