package com.github.kuriimu.image.atlus;

import com.github.kuriimu.kontract.io.BinaryReaderX;

public class TexInfo {
  public int offset;
  public long unk1;
  public String name;
  
  public TexInfo() {
    offset = 0x80;
    unk1 = 0xFFFFFFFFL;
    name = "";
  }
  
  public TexInfo(String n) {
    offset = 0x80;
    unk1 = 0xFFFFFFFFL;
    name = n;
  }
  
  public TexInfo(byte[] bytes) {
    this(new BinaryReaderX(bytes));
  }
  
  public TexInfo(BinaryReaderX br) {
    offset = (int) br.readInt32();
    unk1 = br.readUInt32();
    name = br.readCStringA();
  }
}
