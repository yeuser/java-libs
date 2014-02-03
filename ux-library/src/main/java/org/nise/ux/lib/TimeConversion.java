package org.nise.ux.lib;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.samanpr.jalalicalendar.JalaliCalendar;
import com.samanpr.jalalicalendar.JalaliCalendar.YearMonthDate;

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

  public static String getDateByDelta(long datetime) {
    long time = System.currentTimeMillis() - datetime;
    JalaliCalendar now_cal = new JalaliCalendar();
    JalaliCalendar pub_cal = getJalaliCalendar(datetime);
    if (time > 0) {
      time = (time + 499) / 1000;
      if (time <= 60) {
        return time + " ثانیه پیش";
      }
      time = (time + 29) / 60;
      if (time <= 60) {
        return time + " دقیقه پیش";
      }
      Calendar c1 = Calendar.getInstance(); // today
      c1.add(Calendar.DAY_OF_YEAR, -1); // yesterday
      if (c1.get(Calendar.YEAR) == pub_cal.get(Calendar.YEAR) && //
          c1.get(Calendar.DAY_OF_YEAR) == pub_cal.get(Calendar.DAY_OF_YEAR)) {
        return "دیروز " + JalaliCalendar2HourString(pub_cal);
      }
      time = (time + 29) / 60;
      if (now_cal.get(Calendar.YEAR) != pub_cal.get(Calendar.YEAR)) {
        return JalaliCalendar2FullString(pub_cal);
      }
      if (now_cal.get(Calendar.DAY_OF_YEAR) == pub_cal.get(Calendar.DAY_OF_YEAR)) {
        return "امروز " + time + " ساعت پیش";
      }
    } else {
      time = (-time + 499) / 1000;
      if (time <= 60) {
        return time + " ثانیه بعد";
      }
      time = (time + 29) / 60;
      if (time <= 60) {
        return time + " دقیقه بعد";
      }
      Calendar c1 = Calendar.getInstance(); // today
      c1.add(Calendar.DAY_OF_YEAR, +1); // tomorrow
      if (c1.get(Calendar.YEAR) == pub_cal.get(Calendar.YEAR) && //
          c1.get(Calendar.DAY_OF_YEAR) == pub_cal.get(Calendar.DAY_OF_YEAR)) {
        return "فردا " + JalaliCalendar2HourString(pub_cal);
      }
      time = (time + 29) / 60;
      if (now_cal.get(Calendar.YEAR) != pub_cal.get(Calendar.YEAR)) {
        return JalaliCalendar2FullString(pub_cal);
      }
      if (now_cal.get(Calendar.DAY_OF_YEAR) == pub_cal.get(Calendar.DAY_OF_YEAR)) {
        return "امروز " + time + " ساعت بعد";
      }
    }
    if (now_cal.get(Calendar.DATE) == pub_cal.get(Calendar.MONTH) && //
        now_cal.get(Calendar.WEEK_OF_MONTH) == pub_cal.get(Calendar.WEEK_OF_MONTH)) {
      switch (pub_cal.get(Calendar.DAY_OF_WEEK)) {
        case Calendar.SUNDAY:
          return "یکشنبه این هفته";
        case Calendar.MONDAY:
          return "دوشنبه این هفته";
        case Calendar.TUESDAY:
          return "سه‌شنبه این هفته";
        case Calendar.WEDNESDAY:
          return "چهارشنبه این هفته";
        case Calendar.THURSDAY:
          return "پنجشنبه این هفته";
        case Calendar.FRIDAY:
          return "جمعه این هفته";
        case Calendar.SATURDAY:
          return "شنبه این هفته";
      }
    }
    String date = JalaliCalendar2FullString(pub_cal);
    if (now_cal.get(Calendar.MONTH) != pub_cal.get(Calendar.MONTH) || //
        now_cal.get(Calendar.WEEK_OF_MONTH) != pub_cal.get(Calendar.WEEK_OF_MONTH)) {
      switch (pub_cal.get(Calendar.DAY_OF_WEEK)) {
        case Calendar.SUNDAY:
          return "یکشنبه " + date;
        case Calendar.MONDAY:
          return "دوشنبه " + date;
        case Calendar.TUESDAY:
          return "سه‌شنبه " + date;
        case Calendar.WEDNESDAY:
          return "چهارشنبه " + date;
        case Calendar.THURSDAY:
          return "پنجشنبه " + date;
        case Calendar.FRIDAY:
          return "جمعه " + date;
        case Calendar.SATURDAY:
          return "شنبه " + date;
      }
    }
    return date;
  }

  public static JalaliCalendar convertGregorian2Jalali(GregorianCalendar gregorianCalendar) {
    YearMonthDate now_jalali = JalaliCalendar.gregorianToJalali(new YearMonthDate(gregorianCalendar.get(Calendar.YEAR), gregorianCalendar.get(Calendar.MONTH), gregorianCalendar.get(Calendar.DATE)));
    JalaliCalendar cal = new JalaliCalendar();
    cal.set(now_jalali.getYear(), now_jalali.getMonth(), now_jalali.getDate(), gregorianCalendar.get(Calendar.HOUR_OF_DAY), gregorianCalendar.get(Calendar.MINUTE), gregorianCalendar.get(Calendar.SECOND));
    return cal;
  }

  public static JalaliCalendar getJalaliCalendar(long timeInMillis) {
    TimeZone timeZone = TimeZone.getTimeZone("Iran");
    GregorianCalendar cal = new GregorianCalendar(timeZone);
    cal.setTimeInMillis(timeInMillis);
    return convertGregorian2Jalali(cal);
  }

  public static String JalaliCalendar2FullString(JalaliCalendar jalaliCalendar) {
    return JalaliCalendar2DateString(jalaliCalendar) + " " + JalaliCalendar2HourString(jalaliCalendar);
  }

  public static String JalaliCalendar2DateString(JalaliCalendar jalaliCalendar) {
    int year = jalaliCalendar.get(Calendar.YEAR);
    int month = jalaliCalendar.get(Calendar.MONTH);
    int day = jalaliCalendar.get(Calendar.DAY_OF_MONTH);
    switch (month) {
      case JalaliCalendar.FARVARDIN:
        return day + " فروردین " + year;
      case JalaliCalendar.ORDIBEHESHT:
        return day + " اردیبهشت " + year;
      case JalaliCalendar.KHORDAD:
        return day + " خرداد " + year;
      case JalaliCalendar.TIR:
        return day + " تیر " + year;
      case JalaliCalendar.MORDAD:
        return day + " مرداد " + year;
      case JalaliCalendar.SHAHRIVAR:
        return day + " شهریور " + year;
      case JalaliCalendar.MEHR:
        return day + " مهر " + year;
      case JalaliCalendar.ABAN:
        return day + " آبان " + year;
      case JalaliCalendar.AZAR:
        return day + " آذر " + year;
      case JalaliCalendar.DEY:
        return day + " دی " + year;
      case JalaliCalendar.BAHMAN:
        return day + " بهمن " + year;
      case JalaliCalendar.ESFAND:
        return day + " اسفند " + year;
    }
    return day + " " + month + " " + year;
  }

  public static String JalaliCalendar2HourString(JalaliCalendar jalaliCalendar) {
    int hour = jalaliCalendar.get(Calendar.HOUR_OF_DAY);
    int minute = jalaliCalendar.get(Calendar.MINUTE);
    // int second = jalaliCalendar.get(Calendar.SECOND);
    String hh = "00" + hour;
    hh = hh.substring(hh.length() - 2);
    String mm = "00" + minute;
    mm = mm.substring(mm.length() - 2);
    return "ساعت " + hh + ":" + mm;
  }
}
