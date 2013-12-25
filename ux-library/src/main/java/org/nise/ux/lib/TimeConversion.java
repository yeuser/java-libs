package org.nise.ux.lib;

public class TimeConversion {
  /**
   * یک شی از کلاس‫ TimeConversion ایجاد می‌کند
   * 
   * @return
   */
  // public static TimeConversion Construct() {
  // return new TimeConversion();
  // }
  /**
   * ثانیه را به میلی ثانیه تبدیل می‌کند
   * 
   * @param time
   * @return
   */
  public static long secondToMiliSecond(int time) {
    return time * 1000;
  }

  /**
   * دقیقه را به میلی ثانیه تبدیل می‌کند
   * 
   * @param time
   * @return
   */
  public static long minuteToMiliSecond(int time) {
    return secondToMiliSecond(time * 60);
  }

  /**
   * ساعت را به میلی ثانیه تبدیل می‌کند
   * 
   * @param time
   * @return
   */
  public static long hourToMiliSecond(int time) {
    return minuteToMiliSecond(time * 60);
  }

  /**
   * روز را به میلی ثانیه تبدیل می‌کند
   * 
   * @param time
   * @return
   */
  public static long dayToMiliSecond(int time) {
    return hourToMiliSecond(time * 24);
  }

  /**
   * هفته را به میلی ثانیه تبدیل می‌کند
   * 
   * @param time
   * @return
   */
  public static long weekToMiliSecond(int time) {
    return dayToMiliSecond(time * 7);
  }

  /**
   * ماه را به میلی ثانیه تبدیل می‌کند
   * 
   * @param time
   * @return
   */
  public static long monthToMiliSecond(int time) {
    return dayToMiliSecond(time * 30);
  }

  /**
   * سال را به میلی ثانیه تبدیل می‌کند
   * 
   * @param time
   * @return
   */
  public static long YearToMiliSecond(int time) {
    return monthToMiliSecond(time * 12);
  }

  /**
   * این تابع، زمان به میلی ثانیه و الگوی خروجی زمان را به صورت رشته از ورودی می‌گیرد و بر اساس الگو، خروجی را به صورت رشته باز می‌گرداند.
   * 
   * @param time
   * @param pattern
   * @return
   */
  public static String reverseTimeCovertor(Long time, String pattern) {
    String[] patterStrings = pattern.split(":");
    String resultString = "";
    for (int i = 0; i < patterStrings.length; i++) {
      if (patterStrings[i].equals("HH")) {
        patterStrings[i] = String.valueOf((time / 1000) / 3600);
      } else if (patterStrings[i].equals("MM")) {
        patterStrings[i] = String.valueOf(((time / 1000) % 3600) / 60);
      } else if (patterStrings[i].equals("ss")) {
        patterStrings[i] = String.valueOf(((time / 1000) % 3600) % 60);
      } else if (patterStrings[i].equals("mm")) {
        patterStrings[i] = String.valueOf(((time / 1000) < 1) ? time : 0);
      }
    }
    for (int i = 0; i < patterStrings.length; i++) {
      resultString += patterStrings[i] + ((i < patterStrings.length - 1) ? ":" : "");
    }
    return resultString;
  }
}
