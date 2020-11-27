package com.github.kuriimu.kontract.image.support;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javafx.scene.paint.Color;

public class Support {
  
  public static class ETC1 {
    
    public static final int[] ORDER_3DS = new int[] {
      0, 4, 1, 5, 8, 12, 9, 13, 2, 6, 3, 7, 10, 14, 11, 15
    };
    
    public static int[] ORDER_NORMAL = new int[] {
      0, 4, 8, 12, 1, 5, 9, 13, 2, 6, 10, 14, 3, 7, 11, 15
    };
    
    public static int[][] MODIFIERS = new int[][] {
      new int[] { 2, 8, -2, -8 },
      new int[] { 5, 17, -5, -17 },
      new int[] { 9, 29, -9, -29 },
      new int[] { 13, 42, -13, -42 },
      new int[] { 18, 60, -18, -60 },
      new int[] { 24, 80, -24, -80 },
      new int[] { 33, 106, -33, -106 },
      new int[] { 47, 183, -47, -183 }
    };
    
    public static class RGB {
      public int r, g, b;
      
      public RGB(Color c) {
        this((int) c.getRed() * 255, (int) c.getGreen() * 255,
             (int) c.getBlue() * 255);
      }
      
      public RGB(int r, int g, int b) {
        this.r = r & 0xFF;
        this.g = g & 0xFF;
        this.b = b & 0xFF;
      }
      
      public String toString() {
        return "[" + r + ", " + g + ", " + b + "]";
      }
      
      public RGB add(int mod) {
        return new RGB(clamp(r + mod), clamp(g + mod), clamp(b + mod));
      }
      
      public int sub(RGB c) {
        return errorRGB(r - c.r, g - c.g, b - c.b);
      }
      
      public static RGB average(RGB... src) {
        int aR = 0;
        int aG = 0;
        int aB = 0;
        for (RGB rgb : src) {
          aR += rgb.r;
          aG += rgb.g;
          aB += rgb.b;
        }
        return new RGB((byte) (aR / src.length), (byte) (aG / src.length),
                       (byte) (aB / src.length));
      }
      
      public static RGB average(ArrayList<RGB> src) {
        int aR = 0;
        int aG = 0;
        int aB = 0;
        for (RGB rgb : src) {
          aR += rgb.r;
          aG += rgb.g;
          aB += rgb.b;
        }
        return new RGB((byte) (aR / src.size()), (byte) (aG / src.size()),
                       (byte) (aB / src.size()));
      }
      
      public RGB scale(int limit) {
        if (limit == 16) {
          return new RGB(r * 17, g * 17, b * 17);
        } else {
          return new RGB((r << 3) | (r >> 2), (g << 3) | (g >> 2),
                         (b << 3) | (b >> 2));
        }
      }
      
      public RGB unscale(int limit) {
        return new RGB(r * limit / 256, g * limit / 256, b * limit / 256);
      }
      
      @Override
      public int hashCode() {
        return r | (g << 8) | (b << 16);
      }
      
      @Override
      public boolean equals(Object obj) {
        return obj != null && obj.hashCode() == hashCode();
      }
    }
    
    public static class Block {
      public int lsb;
      public int msb;
      public int flags;
      public int b;
      public int g;
      public int r;
      public boolean isDefault;
      
      public Block() {
        isDefault = false;
      }
      
      public Block(boolean d) {
        isDefault = d;
      }
      
      public boolean getFlipBit() {
        return (flags & 1) == 1;
      }
      
      public void setFlipBit(boolean value) {
        flags = ((flags & ~1) | (value ? 1 : 0));
      }
      
      public boolean getDiffBit() {
        return (flags & 2) == 2;
      }
      
      public void setDiffBit(boolean value) {
        flags = ((flags & ~2) | (value ? 2 : 0));
      }
      
      public int getColorDepth() {
        return (getDiffBit() ? 32 : 16);
      }
      
      public int getTable0() {
        return (flags >> 5) & 7;
      }
      
      public void setTable0(int value) {
        flags = ((flags & ~(7 << 5)) | (value << 5));
      }
      
      public int getTable1() {
        return (flags >> 2) & 7;
      }
      
      public void setTable1(int value) {
        flags = ((flags & ~(7 << 2)) | (value << 2));
      }
      
      public int get(int i) {
        return (msb >> i) % 2 * 2 + (lsb >> i) % 2;
      }
      
      public RGB getColor0() {
        return new RGB(r * getColorDepth() / 256, g * getColorDepth() / 256,
                       b * getColorDepth() / 256);
      }
      
      public RGB getColor1() {
        if (!getDiffBit()) return new RGB(r % 16, g % 16, b % 16);
        RGB c0 = getColor0();
        int rD = sign3(r % 8);
        int gD = sign3(g % 8);
        int bD = sign3(b % 8);
        return new RGB(c0.r + rD, c0.g + gD, c0.b + bD);
      }
    }
    
    public static class PixelData {
      public BigInteger alpha;
      public boolean ignoreOpacity;
      public Block block;
      
      public PixelData() {}
      
      public PixelData(long a, Block b) {
        alpha = BigInteger.valueOf(a);
        block = b;
      }
      
      public PixelData(BigInteger a, Block b) {
        alpha = a;
        block = b;
      }
    }
    
    public static class SolutionSet {
      protected final static int MAX_ERROR = 99999999;
      
      boolean flip;
      boolean diff;
      Solution soln0;
      Solution soln1;
      
      public int getTotalError() {
        return soln0.error + soln1.error;
      }
      
      public SolutionSet() {
        soln1 = soln0 = new Solution(MAX_ERROR);
      }
      
      public SolutionSet(boolean f, boolean d, Solution s0, Solution s1) {
        flip = f;
        diff = d;
        soln0 = s0;
        soln1 = s1;
      }
      
      public Block toBlock() {
        Block blk = new Block();
        blk.setDiffBit(diff);
        blk.setFlipBit(flip);
        for (int i = 0; i < MODIFIERS.length; i++) {
          boolean m0 = true;
          boolean m1 = true;
          for (int m = 0; m < 4; i++) {
            if (MODIFIERS[i][m] != soln0.intenTable[m]) m0 = false;
            if (MODIFIERS[i][m] != soln1.intenTable[m]) m1 = false;
            if (m0 == false && m1 == false) break;
          }
          if (m0) blk.setTable0(i);
          if (m1) blk.setTable1(i);
        }
        
        if (blk.getFlipBit()) {
          int m0 = soln0.selectorMSB;
          int m1 = soln1.selectorMSB;
          m0 = (m0 & 0xC0) * 64 + (m0 & 0x30) * 16 + (m0 & 0xC) * 4 + (m0 & 0x3);
          m1 = (m1 & 0xC0) * 64 + (m1 & 0x30) * 16 + (m1 & 0xC) * 4 + (m1 & 0x3);
          blk.msb = m0 + 4 * m1;
          int l0 = soln0.selectorLSB;
          int l1 = soln1.selectorLSB;
          l0 = (l0 & 0xC0) * 64 + (l0 & 0x30) * 16 + (l0 & 0xC) * 4 + (l0 & 0x3);
          l1 = (l1 & 0xC0) * 64 + (l1 & 0x30) * 16 + (l1 & 0xC) * 4 + (l1 & 0x3);
          blk.lsb = l0 + 4 * l1;
        } else {
          blk.msb = soln0.selectorMSB + 256 * soln1.selectorMSB;
          blk.lsb = soln0.selectorLSB + 256 * soln1.selectorLSB;
        }
        
        if (blk.getDiffBit()) {
          int rdiff = (soln1.blockColor.r - soln0.blockColor.r + 8) % 8;
          int gdiff = (soln1.blockColor.g - soln0.blockColor.g + 8) % 8;
          int bdiff = (soln1.blockColor.b - soln0.blockColor.b + 8) % 8;
          blk.r = (byte)(soln0.blockColor.r * 8 + rdiff);
          blk.g = (byte)(soln0.blockColor.g * 8 + gdiff);
          blk.b = (byte)(soln0.blockColor.b * 8 + bdiff);
        } else {
          blk.r = (byte)(soln0.blockColor.r * 16 + soln1.blockColor.r);
          blk.g = (byte)(soln0.blockColor.g * 16 + soln1.blockColor.g);
          blk.b = (byte)(soln0.blockColor.b * 16 + soln1.blockColor.b);
        }
        
        return blk;
      }
    }
    
    public static class Solution {
      public int error;
      public RGB blockColor;
      public int[] intenTable;
      public int selectorMSB;
      public int selectorLSB;
      
      public Solution() {}
      
      public Solution(RGB c, int[] i) {
        blockColor = c;
        intenTable = i;
      }
      
      public Solution(int e) {
        error = e;
      }
    }
    
    public static int clamp(int n) {
      if (n < 0) return 0;
      if (n > 255) return 255;
      return n;
    }
    
    public static int sign3(int n) {
      return (n + 4) % 8 - 4;
    }
    
    public static int errorRGB(int r, int g, int b) {
      return 2 * r * r + 4 * 6 * g + 3 * b * b;
    }
    
    public static class Decoder {
      protected ArrayDeque<Color> queue;
      
      protected boolean _3ds_order;
      
      public Decoder(boolean _3) {
        queue = new ArrayDeque<>();
        _3ds_order = _3;
      }
      
      public Color get(Supplier<PixelData> func) {
        if (queue.size() == 0) {
          PixelData data = func.get();
          if (data == null) return null;
          RGB baseC0 = data.block.getColor0().scale(data.block.getColorDepth());
          RGB baseC1 = data.block.getColor1().scale(data.block.getColorDepth());
          
          int flipBitmask = data.block.getFlipBit() ? 2 : 8;
          int[] order = (_3ds_order ? ORDER_3DS : ORDER_NORMAL);
          for (int i : order) {
            RGB baseC = ((i & flipBitmask) == 0 ? baseC0 : baseC1);
            int idx;
            if ((i & flipBitmask) == 0) idx = data.block.getTable0();
            else idx = data.block.getTable1();
            int[] mod = MODIFIERS[idx];
            RGB c = baseC.add(mod[data.block.get(i)]);
            double a;
            if (data.ignoreOpacity) {
              a = 1.0;
            } else {
              BigInteger newAlpha = data.alpha.shiftRight(4 * i);
              newAlpha = newAlpha.mod(BigInteger.valueOf(16));
              newAlpha = newAlpha.multiply(BigInteger.valueOf(17));
              a = newAlpha.intValue() / 255.0;
            }
            queue.add(Color.rgb(c.r, c.g, c.b, a));
          }
        }
        return queue.poll();
      }
    }
  }
}
