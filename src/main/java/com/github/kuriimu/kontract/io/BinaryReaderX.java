package com.github.kuriimu.kontract.io;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;

public class BinaryReaderX {
  protected byte[] content;
  protected int pos;
  protected int nibble;
  
  public BinaryReaderX(byte[] bytes) {
    content = bytes;
    pos = 0;
    nibble = -1;
  }
  
  public byte[] getBytes() {
    return content;
  }
  
  public void setBytes(byte[] bytes) {
    content = bytes;
  }
  
  public void resize(int newSize, int r0L, int r1S, int r1D, int r1L) {
    nibble = -1;
    pos = 0;
    byte[] newBytes = new byte[newSize];
    System.arraycopy(content, 0, newBytes, 0, r0L);
    System.arraycopy(content, r1S, newBytes, r1D, r1L);
    content = newBytes;
  }
  
  public String readCStringA() {
    nibble = -1;
    StringBuilder string = new StringBuilder();
    byte b;
    while ((b = (byte) read()) != 0) string.append((char) b);
    return string.toString();
  }
  
  public void writeCStringA(String string) {
    nibble = -1;
    byte[] bytes = string.getBytes();
    for (byte b : bytes) content[pos++] = b;
    content[pos++] = 0;
  }
  
  public String readCStringW() {
    nibble = -1;
    StringBuilder string = new StringBuilder();
    while (peekByte() != 0) string.append((char) readUInt16());
    pos++;
    return string.toString();
  }
  
  public String readCStringSJIS() {
    nibble = -1;
    StringBuilder string = new StringBuilder();
    byte b;
    while ((b = (byte) read()) != 0) string.append((char) b);
    try {
      return new String(string.toString().getBytes(), "MS932");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      return null;
    }
  }
  
  public byte peekByte() {
    return content[pos];
  }
  
  public int readNibble() {
    if (nibble > -1) {
      int tmp = nibble;
      nibble = -1;
      return tmp;
    }
    int tmp = read();
    nibble = (tmp >> 4) & 0xF;
    return tmp & 0xF;
  }
  
  public int readUInt16() {
    nibble = -1;
    return read() | (read() << 8);
  }
  
  public int readInt16() {
    nibble = -1;
    int i16 = readUInt16();
    if ((i16 & 0x8000) > 0) i16 = -((~i16 & 0xFFFF) + 1);
    return i16;
  }
  
  public long readUInt32() {
    nibble = -1;
    return read() | read() << 8 | read() << 16 | read() << 24;
  }
  
  public void writeUInt32(long value) {
    nibble = -1;
    content[pos++] = (byte) (value & 0xFF);
    content[pos++] = (byte) ((value >> 8) & 0xFF);
    content[pos++] = (byte) ((value >> 16) & 0xFF);
    content[pos++] = (byte) ((value >> 24) & 0xFF);
  }
  
  public long readInt32() {
    nibble = -1;
    long i32 = readUInt32();
    if ((i32 & 0x80000000L) > 0) i32 = -((~i32 & 0xFFFFFFFFL) + 1);
    return i32;
  }
  
  public BigInteger readUInt64() {
    BigInteger result = BigInteger.valueOf(read());
    result = result.add(BigInteger.valueOf(read()).shiftLeft(8));
    result = result.add(BigInteger.valueOf(read()).shiftLeft(16));
    result = result.add(BigInteger.valueOf(read()).shiftLeft(24));
    result = result.add(BigInteger.valueOf(read()).shiftLeft(32));
    result = result.add(BigInteger.valueOf(read()).shiftLeft(40));
    result = result.add(BigInteger.valueOf(read()).shiftLeft(48));
    result = result.add(BigInteger.valueOf(read()).shiftLeft(56));
    return result;
  }
  
  public boolean empty() {
    return available() == 0 && nibble == -1;
  }
  
  public int available() {
    return content.length - pos;
  }
  
  public int read() {
    nibble = -1;
    return content[pos++] & 0xFF;
  }
  
  public int read(byte[] b, int off, int len) {
    nibble = -1;
    if (len > content.length - pos) len = content.length - pos;
    System.arraycopy(content, pos, b, off, len);
    pos += len;
    return len;
  }
  
  public byte[] read(int len) {
    nibble = -1;
    byte[] out = new byte[len];
    read(out, 0, len);
    return out;
  }
  
  public void skip(int skip) {
    nibble = -1;
    pos += skip;
  }
  
  public int position() {
    return pos;
  }
  
  public void position(int newPos) {
    nibble = -1;
    pos = newPos;
  }
}
