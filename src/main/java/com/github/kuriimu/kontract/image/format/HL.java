package com.github.kuriimu.kontract.image.format;

import java.util.ArrayList;
import javafx.scene.paint.Color;

import java.nio.ByteOrder;

import com.github.kuriimu.kontract.image.support.Support;
import com.github.kuriimu.kontract.interfaces.image.IImageFormat;
import com.github.kuriimu.kontract.interfaces.image.IImageSwizzle;
import com.github.kuriimu.kontract.io.BinaryReaderX;

public class HL extends IImageFormat {
  
  protected int rDepth;
  protected int gDepth;
  
  public HL(int r, int g) throws Exception {
    bitDepth = r + g;
    if (bitDepth % 4 != 0) throw new Exception("Overall bitDepth has to be dividable by 4. Given bitDepth: " + bitDepth);
    if (bitDepth > 16) throw new Exception("Overall bitDepth can't be bigger than 16. Given bitDepth: " + bitDepth);
    if (bitDepth < 4) throw new Exception("Overall bitDepth can't be smaller than 4. Given bitDepth: " + bitDepth);
    if (r < 4 && g < 4) throw new Exception("Red and Green value can't be smaller than 4.\nGiven Red: " + r + "; Given Green: " + g);
    
    rDepth = r;
    gDepth = g;
    
    formatName = "HL" + Integer.toString(r) + Integer.toString(g);
  }
  
  @Override
  public ArrayList<Color> loadColors(byte[] tex,
                                     boolean ignoreOpacity) throws Exception {
    BinaryReaderX br = new BinaryReaderX(tex);
    int rShift = gDepth;
    
    int gBitMask = (1 << gDepth) - 1;
    int rBitMask = (1 << rDepth) - 1;
    
    ArrayList<Color> out = new ArrayList<>();
    while (!br.empty()) {
      long value = 0;
      switch (bitDepth) {
        case 4:
          value = br.readNibble();
          break;
        case 8:
          value = br.read();
          break;
        case 16:
          value = br.readUInt16();
          break;
        default:
          throw new Exception("BitDepth " + bitDepth + " not supported!");
      }
      int r, g;
      r = changeBitDepth((int) (value >> rShift & rBitMask), rDepth, 8);
      g = changeBitDepth((int) (value & gBitMask), gDepth, 8);
      out.add(Color.rgb(r, g, 255));
    }
    return out;
  }
  
  @Override
  public byte[] save(ArrayList<Color> colors) {
    ArrayList<Byte> bytes = new ArrayList<>();
    try {
      for (Color color : colors) {
        long r = 0;
        if (rDepth != 0) {
          changeBitDepth((int) (color.getRed() * 255), 8, rDepth);
        }
        long g = 0;
        if (gDepth != 0) {
         changeBitDepth((int) (color.getGreen() * 255), 8, gDepth);
        }
        long value = g;
        value |= (r << gDepth);
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
