package com.github.kuriimu.kontract.compression;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ArrayList;

public class RevLZ77 {
  protected static class SCompressInfo {
    public int windowPos;
    public int windowLen;
    public int[] offsetTable;
    public int[] reversedOffsetTable;
    public int[] byteTable;
    public int[] endTable;
    public byte[] bytes;
    
    public SCompressInfo(byte[] input) {
      bytes = input;
      windowPos = 0;
      windowLen = 0;
      offsetTable = new int[4098];
      Arrays.fill(offsetTable, 0);
      reversedOffsetTable = new int[4098];
      Arrays.fill(reversedOffsetTable, 0);
      byteTable = new int[256];
      Arrays.fill(byteTable, -1);
      endTable = new int[256];
      Arrays.fill(endTable, -1);
    }
    
    public int[] search(int src, int maxSize) {
      int ofs = 0;
      if (maxSize < 3) return new int[] { 0, 0 };
      int size = 2;
      for (int nOfs = endTable[bytes[src - 1] & 0xFF]; nOfs != -1;
           nOfs = reversedOffsetTable[nOfs]) {
        int search = src + windowPos - nOfs;
        if (nOfs >= windowPos) search += windowLen;
        if (search - src < 3) continue;
        if (bytes[search - 2] != bytes[src - 2] ||
            bytes[search - 3] != bytes[src - 3]) {
          continue;
        }
        int nMaxSz = Math.min(maxSize, search - src);
        int curSz = 3;
        while (curSz < nMaxSz &&
               bytes[search - curSz - 1] == bytes[src - curSz - 1]) {
          curSz++;
        }
        if (curSz > size) {
          size = curSz;
          ofs = search - src;
          if (size == maxSize) break;
        }
      }
      if (size < 3) return new int[] { 0, 0 };
      return new int[] { size, ofs };
    }
    
    public void slideByte(int src) {
      int uInData = bytes[src] & 0xFF;
      int uInsertOfs = 0;
      if (windowLen == 4098) {
        int uOutData = bytes[src + 4098] & 0xFF;
        if ((byteTable[uOutData] = offsetTable[byteTable[uOutData]]) == -1) {
          endTable[uOutData] = -1;
        } else {
          reversedOffsetTable[byteTable[uOutData]] = -1;
        }
        uInsertOfs = windowPos;
      } else {
        uInsertOfs = windowLen;
      }
      int nOfs = endTable[uInData];
      if (nOfs == -1) {
        byteTable[uInData] = uInsertOfs;
      } else {
        offsetTable[nOfs] = uInsertOfs;
      }
      endTable[uInData] = uInsertOfs;
      offsetTable[uInsertOfs] = -1;
      reversedOffsetTable[uInsertOfs] = nOfs;
      if (windowLen == 4098) {
        windowPos++;
        windowPos %= 4098;
      } else {
        windowLen++;
      }
    }
  }
  
  public static void pack(String input, String output) {
    try {
      byte[] bytes = compress(Files.readAllBytes(Paths.get(input)));
      Files.write(Paths.get(output), bytes);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public static void unpack(String input, String output) {
    try {
      byte[] bytes = decompress(Files.readAllBytes(Paths.get(input)));
      Files.write(Paths.get(output), bytes);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public static byte[] decompress(byte[] bytes) {
    int f = bytes.length - 8;
    int bufferTopAndBottom = (bytes[f] & 0xFF) | (bytes[f + 1] & 0xFF) << 8 |
                             (bytes[f + 2] & 0xFF) << 16 |
                             (bytes[f + 3] & 0xFF) << 24;
    int originalBottom = (bytes[f + 4] & 0xFF) | (bytes[f + 5] & 0xFF) << 8 |
                         (bytes[f + 6] & 0xFF) << 16 |
                         (bytes[f + 7] & 0xFF) << 24;
    int dest = bytes.length + originalBottom;
    int src = bytes.length - ((bufferTopAndBottom >> 24) & 0xFF);
    int end = bytes.length - (bufferTopAndBottom & 0xFFFFFF);
    bytes = Arrays.copyOf(bytes, dest);
    while (true) {
      int flag = bytes[--src];
      int mask = 0x80;
      do {
        if ((flag & mask) == 0) bytes[--dest] = bytes[--src];
        else {
          int size = bytes[--src];
          int offset = (((size & 0x0F) << 8) | (bytes[--src] & 0xFF)) + 3;
          size = (size >> 4 & 0x0F) + 3;
          for (int i = 0; i < size; i++) bytes[--dest] = bytes[dest + offset];
        }
        if (src - end <= 0) return bytes;
      } while ((mask >>= 1) != 0);
    }
  }
  
  @FunctionalInterface
  public static interface Function { 
    public byte[] apply(int origSize, int unusedSize);
  }
  
  public static byte[] compress(byte[] bytes) {
    if (bytes.length <= 8) return null;
    byte[] result = new byte[bytes.length];
    SCompressInfo info = new SCompressInfo(bytes);
    int maxSize = 0xF + 3;
    int src = bytes.length;
    int dest = bytes.length;
    while (src > 0 && dest > 0) {
      int flagPos = --dest;
      int mask = 0x80;
      result[flagPos] = 0;
      do {
        int ofs;
        int[] r = info.search(src, Math.min(Math.min(maxSize, src),
                                            bytes.length - src));
        int size = r[0];
        ofs = r[1];
        if (size < 3) {
          if (dest < 1) return null;
          info.slideByte(--src);
          result[--dest] = bytes[src];
        } else {
          if (dest < 2) return null;
          for (int i = 0; i < size; i++) info.slideByte(--src);
          result[flagPos] |= (byte) mask;
          result[--dest] = (byte) (((size - 3) << 4) | ((ofs - 3) >> 8 & 0x0F));
          result[--dest] = (byte) (ofs - 3);
        }
        if (src <= 0) break;
      } while ((mask >>= 1) != 0);
    }
    Function func = (origSize, unusedSize) -> {
      int compressedRegion = bytes.length - unusedSize;
      int padOfs = origSize + compressedRegion;
      int compressedSize = padOfs + 11 & ~3;
      if (compressedSize >= Math.min(bytes.length, origSize + 0x1000000)) {
        return null;
      }
      ArrayList<Byte> out = new ArrayList<>();
      for (int i = 0; i < origSize; i++) out.add(bytes[i]);
      int st = bytes.length - compressedRegion;
      for (int i = 0; i < compressedRegion; i++) out.add(result[st + i]);
      for (int i = 0; i < (-padOfs & 3); i++) out.add((byte) 0xFF);
      int bufferTopAndBottom = ((compressedSize - origSize) |
                                ((compressedSize - padOfs) << 24));
      out.add((byte) bufferTopAndBottom);
      out.add((byte) (bufferTopAndBottom >> 8));
      out.add((byte) (bufferTopAndBottom >> 16));
      out.add((byte) (bufferTopAndBottom >> 24));
      int originalBottom = (bytes.length - compressedSize);
      out.add((byte) originalBottom);
      out.add((byte) (originalBottom >> 8));
      out.add((byte) (originalBottom >> 16));
      out.add((byte) (originalBottom >> 24));
      byte[] tmp = new byte[out.size()];
      for (int i = 0; i < out.size(); i++) tmp[i] = out.get(i);
      return tmp;
    };
    int orig = bytes.length;
    int unused = bytes.length;
    while (true) {
      int flag = result[--unused];
      int mask = 0x80;
      do {
        if ((flag & mask) == 0) {
          orig--;
          unused--;
        } else {
          orig -= (result[--unused] >> 4 & 0x0F) + 3;
          unused--;
          if (orig + dest < unused) return func.apply(orig, unused);
        }
        if (orig <= 0) return func.apply(0, dest);
      } while ((mask >>= 1) != 0);
    }
  }
}
