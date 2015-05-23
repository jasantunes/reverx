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



import java.util.GregorianCalendar;

public class Timer {
  private GregorianCalendar _start;
  private long _pause;
  private long _time_idle;

  private int _mark;

  public Timer() {
    restart();
  }

  public void restart() {
    _start = new GregorianCalendar();
    _pause = _time_idle = 0;
    _mark = 0;
  }

  public boolean isPaused() {
    return (_pause != 0);
  }

  public void pause() {
    _pause = new GregorianCalendar().getTimeInMillis();
  }

  public void resume() {
    _time_idle += new GregorianCalendar().getTimeInMillis() - _pause;
    _pause = 0;
  }

  public void mark() {
    _mark = getElapsedTime();
  }

  public int getElapsedTimeFromMark() {
    return getElapsedTime() - _mark;
  }

  public int getElapsedTime() {
    if (_pause != 0)
      return (int)(_pause - _start.getTimeInMillis() - _time_idle);
    else
      return (int)(new GregorianCalendar().getTimeInMillis() - _start.getTimeInMillis() - _time_idle);
  }

  /**
   * Returns the total amount of time estimated.
   * 
   * @param completed Ratio completion.
   * @return Estimated time in milliseconds.
   */
  public int calculateETA(float completed) {
    if (completed <= 0 || completed >= 1)
      return 0;
    int elapsed = getElapsedTime();
    return (int)(elapsed / completed - elapsed);
  }

  @Override
  public String toString() {
    return toString(getElapsedTime());
  }

  public static String toString(int milliseconds) {
    int seconds = milliseconds / 1000;
    int hours = seconds / 3600;
    seconds = seconds % 3600;
    int minutes = seconds / 60;
    seconds = seconds % 60;

    if (hours > 0)
      return hours + "h " + minutes + "m";
    else if (seconds > 0)
      return minutes + "m " + seconds + "s";
    else
      return milliseconds + "ms";
  }

  public static String toStringFuzzy(int milliseconds) {
    int seconds = milliseconds / 1000;
    int hours = seconds / 3600;
    seconds = seconds % 3600;
    int minutes = seconds / 60;
    seconds = seconds % 60;

    if (hours > 0) {
      float min = ((int)((minutes / 60.0f) * 10)) / 10.0f;
      return (hours + min) + " hour(s)";
    } else if (minutes > 0) {
      if (seconds > 30)
        minutes++;
      return minutes + " min(s)";
    } else
      return seconds + " sec(s)";
  }

  // public String getEstimate(int completed, int total) {
  // return toString(calculateETA(completed, total));
  // }

}
