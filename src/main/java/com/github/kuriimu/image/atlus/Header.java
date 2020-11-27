package com.github.kuriimu.image.atlus;

import java.util.ArrayList;

import com.github.kuriimu.kontract.io.BinaryReaderX;

public class Header {
  
  public long magic;
  public long zero0;
  public long const0;
  public int width;
  public int height;
  public long type;
  public long imageFormat;
  public int dataSize;
  
  public Header() {
    this(Support.FormatSwitcher.RGBA8888);
  }
  
  public Header(String format) {
    this(Support.FormatSwitcher.valueOf(format));
  }
  
  public Header(Support.FormatSwitcher format) {
    magic = 0x58455453L;
    zero0 = 0;
    const0 = 0xDE1;
    width = 0;
    height = 0;
    type = format.key >> 16;
    imageFormat = format.key & 0xFFFF;
    dataSize = 0;
  }
  
  public Header(byte[] bytes) {
    this(new BinaryReaderX(bytes));
  }
  
  public Header(BinaryReaderX br) {
    magic = br.readUInt32();
    zero0 = br.readUInt32();
    const0 = br.readUInt32();
    width = (int) br.readInt32();
    height = (int) br.readInt32();
    type = br.readUInt32();
    imageFormat = br.readUInt32();
    dataSize = (int) br.readInt32();
  }
}
