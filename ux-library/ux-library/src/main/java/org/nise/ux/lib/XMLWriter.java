package org.nise.ux.lib;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

public class XMLWriter {
  private BufferedWriter bw;
  private String         mainTag;
  private int            depth = 1;
  private boolean        neat;

  public XMLWriter(String xmlfile, String mainTag, boolean neat) throws IOException {
    bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(xmlfile), Charset.forName("utf8")));
    bw.write("<" + mainTag + ">");
    if (neat) {
      bw.newLine();
    }
    this.mainTag = mainTag;
    this.neat = neat;
  }

  public void fullTag(String tagName, String content) throws IOException {
    neatWrite();
    bw.append("<" + tagName + ">" + content.replaceAll("&", "&amp;") + "</" + tagName + ">");
    if (neat) {
      bw.newLine();
    }
  }

  public void startTag(String tagName) throws IOException {
    neatWrite();
    bw.append("<" + tagName + ">");
    if (neat) {
      bw.newLine();
    }
    depth++;
  }

  public void text(String content) throws IOException {
    neatWrite();
    bw.append(content.replaceAll("&", "&amp;"));
    if (neat) {
      bw.newLine();
    }
  }

  public void endTag(String tagName) throws IOException {
    depth--;
    neatWrite();
    bw.append("</" + tagName + ">");
    if (neat) {
      bw.newLine();
    }
  }

  private void neatWrite() throws IOException {
    if (neat) {
      for (int i = 0; i < depth * 2; i++) {
        bw.append(' ');
      }
    }
  }

  public void close() throws IOException {
    bw.write("</" + mainTag + ">");
    if (neat) {
      bw.newLine();
    }
    bw.flush();
    bw.close();
  }

  @Override
  protected void finalize() throws Throwable {
    this.close();
    super.finalize();
  }

  public static String getText(String xmlText) {
    return xmlText.replaceAll("&amp;", "&");
  }
}