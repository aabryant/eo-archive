package com.github.kuriimu.kontract.image.format;

import java.math.BigInteger;
import java.util.ArrayList;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.paint.Color;

import com.github.kuriimu.kontract.image.support.Support;
import com.github.kuriimu.kontract.interfaces.image.IImageFormat;
import com.github.kuriimu.kontract.io.BinaryReaderX;

public class ETC1 extends IImageFormat {
  
  public int blockBitDepth;
  
  boolean alpha;
  boolean _3ds_order;
  
  public ETC1() {
    this(false, true);
  }
  
  public ETC1(boolean a) {
    this(a, true);
  }
  
  public ETC1(boolean a, boolean _3) {
    bitDepth = (a ? 8 : 4);
    blockBitDepth = (alpha ? 128 : 64);
    
    alpha = a;
    _3ds_order = _3;
    
    formatName = (alpha ? "ETC1A4" : "ETC1");
  }
  
  @Override
  public ArrayList<Color> loadColors(byte[] tex,
                                     boolean ignoreOpacity) throws Exception {
    BinaryReaderX br = new BinaryReaderX(tex);
    Support.ETC1.Decoder etc1Decoder = new Support.ETC1.Decoder(_3ds_order);
    ArrayList<Color> out = new ArrayList<>();
    try {
      while (true) {
        Color c = etc1Decoder.get(() -> {
          BigInteger etc1Alpha;
          if (alpha) {
            etc1Alpha = br.readUInt64();
          } else {
            etc1Alpha = BigInteger.valueOf(0xFFFFFFFFL).shiftLeft(32);
            etc1Alpha = etc1Alpha.add(BigInteger.valueOf(0xFFFFFFFFL));
          }
          // I have no idea why this is in the original code. It does literally
          // nothing.
          //if (!etc1Alpha.equals(BigInteger.valueOf(0)));
          Support.ETC1.Block etc1Block = new Support.ETC1.Block();
          etc1Block.lsb = br.readUInt16() & 0xFFFF;
          etc1Block.msb = br.readUInt16() & 0xFFFF;
          etc1Block.flags = br.read() & 0xFF;
          etc1Block.b = br.read() & 0xFF;
          etc1Block.g = br.read() & 0xFF;
          etc1Block.r = br.read() & 0xFF;
          Support.ETC1.PixelData pixelData = new Support.ETC1.PixelData();
          pixelData.alpha = etc1Alpha;
          pixelData.ignoreOpacity = ignoreOpacity;
          pixelData.block = etc1Block;
          return pixelData;
        });
        if (c == null) break;
        out.add(c);
      }
    } catch (ArrayIndexOutOfBoundsException e) {}
    return out;
  }
  
  @Override
  public byte[] save(ArrayList<Color> colors) {
    return null;
  }
}
