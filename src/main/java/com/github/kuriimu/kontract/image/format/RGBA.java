package com.github.kuriimu.kontract.image.format;

import java.util.ArrayList;
import javafx.scene.paint.Color;

import java.nio.ByteOrder;

import com.github.kuriimu.kontract.image.support.Support;
import com.github.kuriimu.kontract.interfaces.image.IImageFormat;
import com.github.kuriimu.kontract.interfaces.image.IImageSwizzle;
import com.github.kuriimu.kontract.io.BinaryReaderX;

public class RGBA extends IImageFormat {
  
  protected int rDepth;
  protected int gDepth;
  protected int bDepth;
  protected int aDepth;
  
  boolean swapChannels;
  
  public RGBA(int r, int g, int b) throws Exception {
    this(r, g, b, 0, false, false);
  }
  
  public RGBA(int r, int g, int b, int a) throws Exception {
    this(r, g, b, a, false, false);
  }
  
  public RGBA(int r, int g, int b, int a, boolean sC) throws Exception {
    this(r, g, b, a, sC, false);
  }
  
  public RGBA(int r, int g, int b, int a, boolean sC,
              boolean standard) throws Exception {
    bitDepth = r + g + b + a;
    if (bitDepth < 8) {
      throw new Exception("Overall bitDepth can't be smaller than 8. Given bitDepth: " + bitDepth);
    } else if (bitDepth > 32) {
      throw new Exception("Overall bitDepth can't be bigger than 32. Given bitDepth: " + bitDepth);
    }
    
    swapChannels = sC;
    
    rDepth = r;
    gDepth = g;
    bDepth = b;
    aDepth = a;
    
    formatName = (standard ? "s" : "");
    if (swapChannels) {
      formatName += (a != 0 ? "A" : "") + "BGR";
      formatName += (a != 0 ? Integer.toString(a) : "");
      formatName += Integer.toString(b) + Integer.toString(g);
      formatName += Integer.toString(r);
    } else {
      formatName += "RGB" + (a != 0 ? "A" : "");
      formatName += Integer.toString(r) + Integer.toString(g);
      formatName += Integer.toString(b);
      formatName += (a != 0 ? Integer.toString(a) : "");
    }
  }
  
  protected final static int[] CONVERT_5_TO_8 = new int[] {
      0x00,0x08,0x10,0x18,0x20,0x29,0x31,0x39,
      0x41,0x4A,0x52,0x5A,0x62,0x6A,0x73,0x7B,
      0x83,0x8B,0x94,0x9C,0xA4,0xAC,0xB4,0xBD,
      0xC5,0xCD,0xD5,0xDE,0xE6,0xEE,0xF6,0xFF
  };
  
  @Override
  public ArrayList<Color> loadColors(byte[] tex,
                                     boolean ignoreOpacity) throws Exception {
    BinaryReaderX br = new BinaryReaderX(tex);
    ArrayList<Color> out = new ArrayList<>();
    while (!br.empty()) {
      int value;
      int r, g, b, a;
      switch (formatName) {
        case "RGBA8888":
          a = br.read();
          b = br.read();
          g = br.read();
          r = br.read();
          break;
        case "RGB888":
          b = br.read();
          g = br.read();
          r = br.read();
          a = 255;
          break;
        case "RGBA5551":
          value = br.readUInt16();
          r = CONVERT_5_TO_8[(value >> 11) & 0x1F];
          g = CONVERT_5_TO_8[(value >> 6) & 0x1F];
          b = CONVERT_5_TO_8[(value >> 1) & 0x1F];
          a = ((value & 1) == 1 ? 255 : 0);
          break;
        case "RGB565":
          value = br.readUInt16();
          r = CONVERT_5_TO_8[(value >> 11) & 0x1F];
          g = (((value >> 5) & 0x3F) * 4) & 0xFF;
          b = CONVERT_5_TO_8[value & 0x1F];
          a = 255;
          break;
        case "RGBA4444":
          a = br.readNibble() * 0x11;
          b = br.readNibble() * 0x11;
          g = br.readNibble() * 0x11;
          r = br.readNibble() * 0x11;
          break;
        default:
          r = 255; br.read();
          g = 255; br.read();
          b = 255; br.read();
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
          a = changeBitDepth((int) (color.getOpacity() * 255), 8, aDepth);
        }
        long r = changeBitDepth((int) (color.getRed() * 255), 8, rDepth);
        long g = changeBitDepth((int) (color.getGreen() * 255), 8, gDepth);
        long b = changeBitDepth((int) (color.getBlue() * 255), 8, bDepth);
        long rShift, bShift, gShift, aShift;
        if (swapChannels) {
          rShift = 0;
          gShift = rDepth;
          bShift = gShift + gDepth;
          aShift = bShift + bDepth;
        } else {
          aShift = 0;
          bShift = aDepth;
          gShift = bShift + bDepth;
          rShift = gShift + gDepth;
        }
        long value = 0;
        value |= (a << aShift);
        value |= (b << bShift);
        value |= (g << gShift);
        value |= (r << rShift);
        if (bitDepth <= 8) {
          bytes.add((byte) value);
        } else if (bitDepth <= 16) {
          bytes.add((byte) value);
          bytes.add((byte) (value >> 8));
        } else if (bitDepth <= 24) {
          bytes.add((byte) value);
          bytes.add((byte) (value >> 8));
          bytes.add((byte) (value >> 16));
        } else if (bitDepth <= 32) {
          bytes.add((byte) value);
          bytes.add((byte) (value >> 8));
          bytes.add((byte) (value >> 16));
          bytes.add((byte) (value >> 24));
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
