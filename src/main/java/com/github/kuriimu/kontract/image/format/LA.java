package com.github.kuriimu.kontract.image.format;

import java.util.ArrayList;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.nio.ByteOrder;

import com.github.kuriimu.kontract.image.support.Support;
import com.github.kuriimu.kontract.image.ImageSettings;
import com.github.kuriimu.kontract.interfaces.image.IImageFormat;
import com.github.kuriimu.kontract.interfaces.image.IImageSwizzle;
import com.github.kuriimu.kontract.io.BinaryReaderX;

public class LA extends IImageFormat {
  
  protected int lDepth;
  protected int aDepth;
  
  public LA(int l, int a) throws Exception {
    bitDepth = a + l;
    if (bitDepth % 4 != 0) throw new Exception("Overall bitDepth has to be dividable by 4. Given bitDepth: " + bitDepth);
    if (bitDepth > 16) throw new Exception("Overall bitDepth can't be bigger than 16. Given bitDepth: " + bitDepth);
    if (bitDepth < 4) throw new Exception("Overall bitDepth can't be smaller than 4. Given bitDepth: " + bitDepth);
    if (l < 4 && a < 4) throw new Exception("Luminance and Alpha value can't be smaller than 4.\nGiven Luminance: " + l + "; Given Alpha: " + a);
    
    lDepth = l;
    aDepth = a;
    
    formatName = (l != 0 ? "L" : "") + (a != 0 ? "A" : "") + (l != 0 ? l : "") +
                 (a != 0 ? a : "");
  }
  
  @Override
  public ArrayList<Color> loadColors(byte[] tex,
                                     boolean ignoreOpacity) throws Exception {
    BinaryReaderX br = new BinaryReaderX(tex);
    ArrayList<Color> out = new ArrayList<>();
    while (!br.empty()) {
      int r, g, b, a;
      switch (formatName) {
        case "LA88":
          a = br.read();
          r = br.read();
          g = r;
          b = r;
          break;
        case "L8":
          a = 255;
          r = br.read();
          g = r;
          b = r;
          break;
        case "A8":
          a = br.read();
          r = 255;
          g = 255;
          b = 255;
          break;
        case "LA44":
          a = br.readNibble();
          r = br.readNibble();
          g = r;
          b = r;
          break;
        case "L4":
          a = 255;
          r = br.readNibble();
          g = r;
          b = r;
          break;
        case "A4":
          a = br.readNibble();
          r = 255;
          g = 255;
          b = 255;
          break;
        default:
          r = 255;
          g = 255;
          b = 255;
          a = 0;
          break;
      }
      if (ignoreOpacity) a = 255;
      out.add(Color.rgb(r, g, b, a / 255.0));
    }
    return out;
  }
  
  @Override
  public byte[] save(ArrayList<Color> colors) {
    ArrayList<Byte> bytes = new ArrayList<>();
    try {
      for (Color color : colors) {
        long a = 0;
        if (aDepth != 0) {
          changeBitDepth((int) (color.getOpacity() * 255), 8, aDepth);
        }
        long l = 0;
        if (lDepth != 0) {
         changeBitDepth((int) (color.getGreen() * 255), 8, lDepth);
        }
        long value = a;
        value |= (l << aDepth);
        byte b = 0;
        int i = 0;
        switch (bitDepth) {
          case 4:
            if (i % 2 == 0) {
              b |= (byte) ((value >> 4) << 4);
              bytes.add(b);
            } else b = (byte) (value & 0xF);
            break;
          case 8:
            bytes.add((byte) value);
            break;
          case 16:
            bytes.add((byte) value);
            bytes.add((byte) (value >> 8));
            break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    byte[] out = new byte[bytes.size()];
    for (int i = 0; i < out.length; i++) out[i] = bytes.get(i);
    return out;
  }
}
