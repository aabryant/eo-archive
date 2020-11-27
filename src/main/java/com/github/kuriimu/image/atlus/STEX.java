package com.github.kuriimu.image.atlus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

import com.github.kuriimu.kontract.image.ImageSettings;
import com.github.kuriimu.kontract.image.swizzle.CTRSwizzle;
import com.github.kuriimu.kontract.io.BinaryReaderX;

public class STEX {
  public Header header;
  public TexInfo texInfo;
  public long format;
  public WritableImage bmp;
  public ImageSettings settings;
  
  public STEX(byte[] bytes) {
    try {
      BinaryReaderX br = new BinaryReaderX(bytes);
      header = new Header(br);
      texInfo = new TexInfo(br);
      format = ((header.type << 16) | header.imageFormat);
      settings = new ImageSettings();
      settings.width = header.width;
      settings.height = header.height;
      settings.format = Support.Format.get(format);
      settings.swizzle = new CTRSwizzle(header.width, header.height);
      br.position(texInfo.offset);
      bmp = settings.format.loadImage(br.read(header.dataSize), settings);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public STEX() {
    header = new Header();
    texInfo = new TexInfo();
    format = ((header.type << 16) | header.imageFormat);
    settings = new ImageSettings();
    settings.width = header.width;
    settings.height = header.height;
    settings.format = Support.Format.get(format);
    settings.swizzle = new CTRSwizzle(header.width, header.height);
  }
  
  public STEX(String name) {
    header = new Header();
    texInfo = new TexInfo(name);
    format = ((header.type << 16) | header.imageFormat);
    settings = new ImageSettings();
    settings.width = header.width;
    settings.height = header.height;
    settings.format = Support.Format.get(format);
    settings.swizzle = new CTRSwizzle(header.width, header.height);
  }
  
  public STEX(String name, String fmt) {
    header = new Header(fmt);
    texInfo = new TexInfo(name);
    format = ((header.type << 16) | header.imageFormat);
    settings = new ImageSettings();
    settings.width = header.width;
    settings.height = header.height;
    settings.format = Support.Format.get(format);
    settings.swizzle = new CTRSwizzle(header.width, header.height);
  }
  
  public byte[] toBytes(String imgPath) {
    try {
      ArrayList<Byte> out = new ArrayList<>();
      Image img = null;
      byte[] bytes = null;
      if (settings.format.formatName.startsWith("ETC1")) {
        String t3dsPath;
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
          t3dsPath = "\\external\\tex3ds.exe";
        } else {
          t3dsPath = "/external/tex3ds";
        }
        new ProcessBuilder(new File(getClass().getProtectionDomain()
                                              .getCodeSource().getLocation()
                                              .getPath().toString())
                                   .getParentFile() + t3dsPath,
                           "-o", "tmp.etc1", "-f",
                           settings.format.formatName.toLowerCase(),
                           "-z", "none", imgPath).start().waitFor();
        FileInputStream in = new FileInputStream("tmp.etc1");
        in.skip(5);
        settings.width = in.read() | (in.read() << 8);
        settings.height = in.read() | (in.read() << 8);
        in.skip(12);
        bytes = new byte[in.available()];
        in.read(bytes);
        in.close();
        new File("tmp.etc1").delete();
      } else {
        img = new Image("file:" + imgPath);
        settings.width = (int) img.getWidth();
        settings.height = (int) img.getHeight();
      }
      header.width = settings.width;
      header.height = settings.height;
      settings.swizzle = new CTRSwizzle(header.width, header.height);
      if (bytes == null) bytes = settings.format.save(img, settings);
      header.dataSize = bytes.length;
      out.add((byte) header.magic);
      out.add((byte) (header.magic >> 8));
      out.add((byte) (header.magic >> 16));
      out.add((byte) (header.magic >> 24));
      out.add((byte) header.zero0);
      out.add((byte) (header.zero0 >> 8));
      out.add((byte) (header.zero0 >> 16));
      out.add((byte) (header.zero0 >> 24));
      out.add((byte) header.const0);
      out.add((byte) (header.const0 >> 8));
      out.add((byte) (header.const0 >> 16));
      out.add((byte) (header.const0 >> 24));
      out.add((byte) header.width);
      out.add((byte) (header.width >> 8));
      out.add((byte) (header.width >> 16));
      out.add((byte) (header.width >> 24));
      out.add((byte) header.height);
      out.add((byte) (header.height >> 8));
      out.add((byte) (header.height >> 16));
      out.add((byte) (header.height >> 24));
      out.add((byte) header.type);
      out.add((byte) (header.type >> 8));
      out.add((byte) (header.type >> 16));
      out.add((byte) (header.type >> 24));
      out.add((byte) header.imageFormat);
      out.add((byte) (header.imageFormat >> 8));
      out.add((byte) (header.imageFormat >> 16));
      out.add((byte) (header.imageFormat >> 24));
      out.add((byte) header.dataSize);
      out.add((byte) (header.dataSize >> 8));
      out.add((byte) (header.dataSize >> 16));
      out.add((byte) (header.dataSize >> 24));
      out.add((byte) 0x80);
      for (int i = 0; i < 3; i++) out.add((byte) 0);
      out.add((byte) texInfo.unk1);
      out.add((byte) (texInfo.unk1 >> 8));
      out.add((byte) (texInfo.unk1 >> 16));
      out.add((byte) (texInfo.unk1 >> 24));
      byte[] nameBytes = texInfo.name.getBytes(Charset.forName("ASCII"));
      for (byte b : nameBytes) out.add(b);
      out.add((byte) 0);
      int cnt = 41 + nameBytes.length;
      if (cnt < 0x80) for (; cnt < 0x80; cnt++) out.add((byte) 0);
      for (byte b : bytes) out.add(b);
      bytes = new byte[out.size()];
      for (int i = 0; i < bytes.length; i++) bytes[i] = out.get(i);
      return bytes;
    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
      return null;
    }
  }
  
  public void save(String path, String imgPath) {
    try (FileOutputStream out = new FileOutputStream(path)) {
      Image img = null;
      byte[] bytes = null;
      if (settings.format.formatName.startsWith("ETC1")) {
        new ProcessBuilder(new File(getClass().getProtectionDomain()
                                              .getCodeSource().getLocation()
                                              .getPath().toString())
                                   .getParentFile() + "/external/tex3ds",
                           "-o", "tmp.etc1", "-f",
                           settings.format.formatName.toLowerCase(),
                           "-z", "none", imgPath).start().waitFor();
        FileInputStream in = new FileInputStream("tmp.etc1");
        in.skip(5);
        settings.width = in.read() | (in.read() << 8);
        settings.height = in.read() | (in.read() << 8);
        in.skip(12);
        bytes = new byte[in.available()];
        in.read(bytes);
        in.close();
        new File("tmp.etc1").delete();
      } else {
        img = new Image("file:" + imgPath);
        settings.width = (int) img.getWidth();
        settings.height = (int) img.getHeight();
      }
      header.width = settings.width;
      header.height = settings.height;
      header.type = (format >> 16);
      header.imageFormat = (format & 0xFFFF);
      settings.swizzle = new CTRSwizzle(header.width, header.height);
      if (bytes == null) bytes = settings.format.save(img, settings);
      header.dataSize = bytes.length;
      out.write((byte) header.magic);
      out.write((byte) (header.magic >> 8));
      out.write((byte) (header.magic >> 16));
      out.write((byte) (header.magic >> 24));
      out.write((byte) header.zero0);
      out.write((byte) (header.zero0 >> 8));
      out.write((byte) (header.zero0 >> 16));
      out.write((byte) (header.zero0 >> 24));
      out.write((byte) header.const0);
      out.write((byte) (header.const0 >> 8));
      out.write((byte) (header.const0 >> 16));
      out.write((byte) (header.const0 >> 24));
      out.write((byte) header.width);
      out.write((byte) (header.width >> 8));
      out.write((byte) (header.width >> 16));
      out.write((byte) (header.width >> 24));
      out.write((byte) header.height);
      out.write((byte) (header.height >> 8));
      out.write((byte) (header.height >> 16));
      out.write((byte) (header.height >> 24));
      out.write((byte) header.type);
      out.write((byte) (header.type >> 8));
      out.write((byte) (header.type >> 16));
      out.write((byte) (header.type >> 24));
      out.write((byte) header.imageFormat);
      out.write((byte) (header.imageFormat >> 8));
      out.write((byte) (header.imageFormat >> 16));
      out.write((byte) (header.imageFormat >> 24));
      out.write((byte) header.dataSize);
      out.write((byte) (header.dataSize >> 8));
      out.write((byte) (header.dataSize >> 16));
      out.write((byte) (header.dataSize >> 24));
      out.write((byte) 0x80);
      for (int i = 0; i < 3; i++) out.write((byte) 0);
      out.write((byte) texInfo.unk1);
      out.write((byte) (texInfo.unk1 >> 8));
      out.write((byte) (texInfo.unk1 >> 16));
      out.write((byte) (texInfo.unk1 >> 24));
      byte[] nameBytes = texInfo.name.getBytes(Charset.forName("ASCII"));
      for (byte b : nameBytes) out.write(b);
      out.write((byte) 0);
      int cnt = 41 + nameBytes.length;
      if (cnt < 0x80) for (; cnt < 0x80; cnt++) out.write((byte) 0);
      for (byte b : bytes) out.write(b);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
