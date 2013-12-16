package org.nise.ux.lib;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class StringNormalizer {
  private static HashMap<String, String> chars        = new HashMap<String, String>();
  public static final String             farsi        = "ابپتثجچحخدذرزژسشصضطظعغفقکگلمنوهی";
  public static final String             number       = "0-9";
  public static final String             english      = "a-zA-Z";
  public static final String             german       = "äöüÄÖÜß";
  private static String                  extra        = "\\s";
  private static char[][]                char2replace = {
                                                      //
      { '۰', '0' },//
      { '۱', '1' },//
      { '۲', '2' },//
      { '۳', '3' },//
      { '۴', '4' },//
      { '۵', '5' },//
      { '۶', '6' },//
      { '۷', '7' },//
      { '۸', '8' },//
      { '۹', '9' },//
      { '٠', '0' },//
      { '١', '1' },//
      { '٢', '2' },//
      { '٣', '3' },//
      { '٤', '4' },//
      { '٥', '5' },//
      { '٦', '6' },//
      { '٧', '7' },//
      { '٨', '8' },//
      { '٩', '9' },//
      { 'آ', 'ا' },//
      { 'أ', 'ا' },//
      { 'ؤ', 'و' },//
      { 'إ', 'ا' },//
      { 'ئ', 'ی' },//
      { 'ا', 'ا' },//
      { 'ب', 'ب' },//
      { 'ة', 'ه' },//
      { 'ت', 'ت' },//
      { 'ث', 'ث' },//
      { 'ج', 'ج' },//
      { 'ح', 'ح' },//
      { 'خ', 'خ' },//
      { 'د', 'د' },//
      { 'ذ', 'ذ' },//
      { 'ر', 'ر' },//
      { 'ز', 'ز' },//
      { 'س', 'س' },//
      { 'ش', 'ش' },//
      { 'ص', 'ص' },//
      { 'ض', 'ض' },//
      { 'ط', 'ط' },//
      { 'ظ', 'ظ' },//
      { 'ع', 'ع' },//
      { 'غ', 'غ' },//
      { 'ف', 'ف' },//
      { 'ق', 'ق' },//
      { 'ك', 'ک' },//
      { 'ل', 'ل' },//
      { 'م', 'م' },//
      { 'ن', 'ن' },//
      { 'ه', 'ه' },//
      { 'و', 'و' },//
      { 'ى', 'ی' },//
      { 'ي', 'ی' },//
      { 'ٮ', 'ب' },//
      { 'ٯ', 'ق' },//
      { '\u0671', 'ا' },//
      { '\u0672', 'ا' },//
      { '\u0673', 'ا' },//
      { '\u0675', 'ا' },//
      { '\u0676', 'و' },//
      { '\u0677', 'و' },//
      { 'ٶ', 'و' },//
      { 'ٷ', 'و' },//
      { 'ٸ', 'ی' },//
      { 'ٹ', 'ث' },//
      { 'ٺ', 'ث' },//
      { 'ٻ', 'ب' },//
      { 'ټ', 'ت' },//
      { 'ٽ', 'ت' },//
      { 'پ', 'پ' },//
      { 'ٿ', 'ت' },//
      { 'ڀ', 'ب' },//
      { 'ځ', 'ح' },//
      { 'ڂ', 'خ' },//
      { 'ڃ', 'ج' },//
      { 'ڄ', 'ج' },//
      { 'څ', 'خ' },//
      { 'چ', 'چ' },//
      { 'ڇ', 'چ' },//
      { 'ڈ', 'ذ' },//
      { 'ډ', 'د' },//
      { 'ڊ', 'د' },//
      { 'ڋ', 'ذ' },//
      { 'ڌ', 'ذ' },//
      { 'ڍ', 'د' },//
      { 'ڎ', 'ڎ' },//
      { 'ڏ', 'ذ' },//
      { 'ڐ', 'ذ' },//
      { 'ڑ', 'ر' },//
      { 'ڒ', 'ر' },//
      { 'ړ', 'ر' },//
      { 'ڔ', 'ر' },//
      { 'ڕ', 'ر' },//
      { 'ږ', 'ر' },//
      { 'ڗ', 'ز' },//
      { 'ژ', 'ژ' },//
      { 'ڙ', 'ژ' },//
      { 'ښ', 'س' },//
      { 'ڛ', 'س' },//
      { 'ڜ', 'ش' },//
      { 'ڝ', 'ص' },//
      { 'ڞ', 'ض' },//
      { 'ڟ', 'ظ' },//
      { 'ڠ', 'غ' },//
      { 'ڡ', 'ف' },//
      { 'ڢ', 'ف' },//
      { 'ڣ', 'ف' },//
      { 'ڤ', 'ق' },//
      { 'ڥ', 'ف' },//
      { 'ڦ', 'ف' },//
      { 'ڧ', 'ق' },//
      { 'ڨ', 'ق' },//
      { 'ک', 'ک' },//
      { 'ڪ', 'ک' },//
      { 'ګ', 'ک' },//
      { 'ڬ', 'ک' },//
      { 'ڭ', 'ک' },//
      { 'ڮ', 'ک' },//
      { 'گ', 'گ' },//
      { 'ڰ', 'گ' },//
      { 'ڱ', 'گ' },//
      { 'ڲ', 'گ' },//
      { 'ڳ', 'گ' },//
      { 'ڴ', 'گ' },//
      { 'ڵ', 'ل' },//
      { 'ڶ', 'ل' },//
      { 'ڷ', 'ل' },//
      { 'ڸ', 'ل' },//
      { 'ڹ', 'ن' },//
      { 'ں', 'ن' },//
      { 'ڻ', 'ن' },//
      { 'ڼ', 'ن' },//
      { 'ڽ', 'ن' },//
      { 'ھ', 'ه' },//
      { 'ڿ', 'چ' },//
      { 'ۀ', 'ه' },//
      { 'ہ', 'ه' },//
      { 'ۂ', 'ه' },//
      { 'ۃ', 'ه' },//
      { 'ۄ', 'و' },//
      { 'ۅ', 'و' },//
      { 'ۆ', 'و' },//
      { 'ۇ', 'و' },//
      { 'ۈ', 'و' },//
      { 'ۉ', 'و' },//
      { 'ۊ', 'و' },//
      { 'ۋ', 'و' },//
      { 'ی', 'ی' },//
      { 'ۍ', 'ی' },//
      { 'ێ', 'ی' },//
      { 'ۏ', 'و' },//
      { 'ې', 'ی' },//
      { 'ۑ', 'ی' },//
      { 'ے', 'ی' },//
      { 'ۓ', 'ی' },//
      { 'ە', 'ه' },//
      { 'ۮ', 'د' },//
      { 'ۯ', 'ر' },//
                   // Braille Patterns
                   // Range: 2800— 28FF
                   // Number of characters: 256
                                                      };

  public static void normalizationCharReport(String str) {
    for (int i = 0; i < str.length(); i++) {
      String key = str.substring(i, i + 1);
      if (chars.get(key) == null) {
        chars.put(key, key);
        //				try {
        //					saveCharacter(str.charAt(i));
        //				} catch (SQLException e) {
        //					e.printStackTrace();
        //				} catch (ClassNotFoundException e) {
        //					e.printStackTrace();
        //				}
        Logger.getLogger(StringNormalizer.class).warn("Found Unknown chars: " + key + " @[" + Integer.toHexString(key.codePointAt(0)) + "]");
      }
    }
  }

  //	public static int saveCharacter(char character) throws SQLException, ClassNotFoundException {
  //		HashMap<String, QueryParameter> whereClause = new HashMap<String, QueryParameter>();
  //		whereClause.put("chr_code", new QueryParameter((int) character));
  //		ResultSet rs = PGConnector.getInstance("srv26-vir6").selectFromTable("characters", whereClause);
  //		if (!rs.next()) {
  //			PGConnector.getInstance("srv26-vir6").insertIntoTable("characters",//
  //					new String[] { "chr", "chr_code" },//
  //					new QueryParameter[][] { {
  //							//
  //							new QueryParameter("" + character),//
  //							new QueryParameter((int) character) //
  //					},//
  //					});
  //			rs = PGConnector.getInstance("srv26-vir6").selectFromTable("characters", whereClause);
  //			rs.next();
  //		}
  //		return rs.getInt("id");
  //	}
  public static String normalizeString(String str) {
    for (int i = 0; i < char2replace.length; i++) {
      str = str.replace(char2replace[i][0], char2replace[i][1]);
    }
    Matcher matcher = Pattern.compile("[^" + farsi + number + english + german + extra + "]+").matcher(str);
    while (matcher.find()) {
      int start = matcher.start();
      int end = matcher.end();
      normalizationCharReport(str.substring(start, end));
    }
    return str;
  }

  public static String normalizeStringAndRemove(String str) {
    return normalizeString(str).replaceAll("[^" + farsi + number + english + german + extra + "]+", " ").replaceAll("\\s+", " ");
  }

  public static String getAllReplacingChars() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < char2replace.length; i++) {
      sb.append(char2replace[i][0]);
    }
    return sb.toString();
  }
}