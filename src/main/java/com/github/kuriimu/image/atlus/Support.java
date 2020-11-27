package com.github.kuriimu.image.atlus;

import java.util.HashMap;

import com.github.kuriimu.kontract.image.format.ETC1;
import com.github.kuriimu.kontract.image.format.HL;
import com.github.kuriimu.kontract.image.format.LA;
import com.github.kuriimu.kontract.image.format.RGBA;
import com.github.kuriimu.kontract.interfaces.image.IImageFormat;

class Support {
  public static HashMap<Long,IImageFormat> Format = new HashMap<>();
  static {
    try {
      Format.put(0x14016752L, new RGBA(8, 8, 8, 8));
      Format.put(0x80336752L, new RGBA(4, 4, 4, 4));
      Format.put(0x80346752L, new RGBA(5, 5, 5, 1));
      Format.put(0x14016754L, new RGBA(8, 8, 8));
      Format.put(0x83636754L, new RGBA(5, 6, 5));
      Format.put(0x14016756L, new LA(0, 8));
      Format.put(0x67616756L, new LA(0, 4));
      Format.put(0x14016757L, new LA(8, 0));
      Format.put(0x67616757L, new LA(4, 0));
      Format.put(0x14016758L, new LA(8, 8));
      Format.put(0x67606758L, new LA(4, 4));
      Format.put(0x0000675AL, new ETC1());
      Format.put(0x0000675BL, new ETC1(true));
      Format.put(0x1401675AL, new ETC1());
      Format.put(0x1401675BL, new ETC1(true));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public static enum FormatSwitcher {
    RGBA8888(0x14016752L),
    RGBA4444(0x80336752L),
    RGBA5551(0x80346752L),
    RGB888(0x14016754L),
    RGB565(0x83636754L),
    A8(0x14016756L),
    A4(0x67616756L),
    L8(0x14016757L),
    L4(0x67616757L),
    LA88(0x14016758L),
    LA44(0x67606758L),
    ETC1(0x1401675AL),
    ETC1A4(0x1401675BL);
    
    public final long key;
    
    FormatSwitcher(long k) {
      key = k;
    }
  }
}
