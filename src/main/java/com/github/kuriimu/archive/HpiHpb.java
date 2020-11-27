package com.github.kuriimu.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;

import com.github.kuriimu.image.atlus.STEX;
import com.github.kuriimu.kontract.compression.RevLZ77;

public class HpiHpb {
  public static class HashEntry {
    public short entryOffset;
    public short entryCount;
    
    public HashEntry(short ofs, short cnt) {
      entryOffset =  ofs;
      entryCount = cnt;
    }
  }
  
  public static final ArrayList<String> EXTENSIONS = new ArrayList<>();
  static {
    EXTENSIONS.add(".bam2");
    EXTENSIONS.add(".bcfnt");
    EXTENSIONS.add(".bch");
    EXTENSIONS.add(".bchenv");
    EXTENSIONS.add(".bcwav");
    EXTENSIONS.add(".bf");
    EXTENSIONS.add(".bmp");
    EXTENSIONS.add(".cbmd");
    EXTENSIONS.add(".ctpk");
    EXTENSIONS.add(".dat");
    EXTENSIONS.add(".epl");
    EXTENSIONS.add(".mbm");
    EXTENSIONS.add(".mgb");
    EXTENSIONS.add(".msb");
    EXTENSIONS.add(".png");
    EXTENSIONS.add(".ssb");
    EXTENSIONS.add(".ssnd");
    EXTENSIONS.add(".stex");
    EXTENSIONS.add(".tbl");
    EXTENSIONS.add(".tgd");
    EXTENSIONS.add(".tmx");
    EXTENSIONS.add(".ttd");
    EXTENSIONS.add(".ydd");
    EXTENSIONS.add(".ymd");
  }
  
  public static class Entry {
    public int stringOfs;
    public int fileOfs;
    public int fileSize;
    public int uncompressedSize;
    
    public Entry(int sofs, int fofs, int fsz, int usz) {
      stringOfs = sofs;
      fileOfs = fofs;
      fileSize = fsz;
      uncompressedSize = usz;
    }
    
    public Entry() {}
  }
  
  protected static final long HASH_SLOT_COUNT = 0x1000;
  protected static final long HASH_MAGIC = 0x25;
  
  protected String name;
  protected short hashCount;
  protected int entryCount;
  protected ArrayList<Entry> entries;
  protected ArrayList<String> names;
  protected ArrayList<String> files = new ArrayList<>();
  
  
  protected static long hash(String key) {
    long value = 0;
    byte[] bytes = key.getBytes(Charset.forName("SJIS"));
    for (byte b : bytes) {
      value = (value * HASH_MAGIC + (b & 0xFF)) & 0xFFFFFFFFL;
    }
    return (value % HASH_SLOT_COUNT) & 0xFFFFFFFFL;
  }
  
  protected static long readU32(InputStream is) throws IOException {
    return ((long) (is.read() & 0xFF)) | 
           ((long) (is.read() & 0xFF)) << 8 |
           ((long) (is.read() & 0xFF)) << 16 |
           ((long) (is.read() & 0xFF)) << 24;
  }
  
  protected static int readS32(InputStream is) throws IOException {
    return (is.read() & 0xFF) | (is.read() & 0xFF) << 8 |
           (is.read() & 0xFF) << 16 | (is.read() & 0xFF) << 24;
  }
  
  protected static short readS16(InputStream is) throws IOException {
    return (short) ((is.read() & 0xFF) | (is.read() & 0xFF) << 8);
  }
  
  protected static String readString(InputStream is) throws IOException {
    ArrayList<Byte> bytes = new ArrayList<>();
    byte b;
    while ((b = (byte) is.read()) != 0) bytes.add(b);
    byte[] arr = new byte[bytes.size()];
    for (int i = 0; i < arr.length; i++) arr[i] = bytes.get(i);
    return new String(arr, Charset.forName("SJIS"));
  }
  
  protected static String readString(RandomAccessFile is) throws IOException {
    ArrayList<Byte> bytes = new ArrayList<>();
    byte b;
    while ((b = (byte) is.read()) != 0) bytes.add(b);
    byte[] arr = new byte[bytes.size()];
    for (int i = 0; i < arr.length; i++) arr[i] = bytes.get(i);
    return new String(arr, Charset.forName("SJIS"));
  }
  
  public static HpiHpb load(String name) {
    try (FileInputStream hpi = new FileInputStream(name + ".HPI")) {
      HpiHpb result = new HpiHpb();
      long hpiLen = new File(name + ".HPI").length();
      long hpbLen = new File(name + ".HPB").length();
      result.name = name;
      hpi.skip(18);
      result.hashCount = readS16(hpi);
      result.entryCount = readS32(hpi);
      hpi.skip(result.hashCount * 4);
      result.entries = new ArrayList<>();
      for (int i = 0; i < result.entryCount; i++) {
        result.entries.add(new Entry(readS32(hpi), readS32(hpi), readS32(hpi),
                                     readS32(hpi)));
        if (result.entries.get(i).fileOfs >= hpbLen ||
            result.entries.get(i).fileOfs < 0) {
          result.entries.get(i).fileOfs = 0;
        }
      }
      result.entries.sort((a,b) -> Integer.compare(a.stringOfs, b.stringOfs));
      result.names = new ArrayList<>();
      for (int i = 0; i < result.entryCount; i++) {
        result.names.add(readString(hpi));
      }
      return result;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
  
  public void extract(String out) {
    extract(out, false);
  }
  
  public void extract(String out, boolean extractFiles) {
    try {
      RandomAccessFile hpb = new RandomAccessFile(name + ".HPB", "r");
      for (int i = 0; i < entryCount; i++) {
        File f = new File(out + "/" + names.get(i));
        f.getParentFile().mkdirs();
        hpb.seek(entries.get(i).fileOfs);
        byte[] bytes;
        if (entries.get(i).uncompressedSize > 0) {
          hpb.skipBytes(4);
          int size = (hpb.read() & 0xFF) | (hpb.read() & 0xFF) << 8 |
                     (hpb.read() & 0xFF) << 16 | (hpb.read() & 0xFF) << 24;
          hpb.skipBytes(24);
          bytes = new byte[size];
          hpb.read(bytes);
          bytes = RevLZ77.decompress(bytes);
        } else {
          bytes = new byte[entries.get(i).fileSize];
          hpb.read(bytes);
        }
        Files.write(Paths.get(f.getAbsolutePath()), bytes);
        if (names.get(i).endsWith(".STEX") && extractFiles) {
          STEX stex = new STEX(bytes);
          f = new File(f.getAbsolutePath() + ".png");
          ImageIO.write(SwingFXUtils.fromFXImage(stex.bmp, null), "png", f);
        }
      }
      hpb.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  protected static ArrayList<String> getFiles(ArrayList<String> files,
                                              Path dir) {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      for (Path path : stream) {
        if(path.toFile().isDirectory()) getFiles(files, path);
        else {
          String ext = path.toString()
                           .substring(path.toString().lastIndexOf('.'))
                           .toLowerCase();
          if (EXTENSIONS.contains(ext)) {
            files.add(path.toString().replace("\\", "/"));
          }
        }
      }
    } catch(IOException e) {
      e.printStackTrace();
    }
    return files;
  }
  
  public void replace(String toReplace, String replacement) {
    boolean isNewFile = !containsFile(toReplace);
    if (isNewFile) {
      entryCount++;
      Entry e = new Entry();
      e.stringOfs = nextStringOfs();
      entries.add(e);
      names.add(toReplace);
    }
    try {
      FileOutputStream hpi = new FileOutputStream(name + ".tmpi");
      FileOutputStream hpb = new FileOutputStream(name + ".tmpb");
      RandomAccessFile hpbOld = new RandomAccessFile(name + ".HPB", "r");
      hpi.write((byte) 0x48);
      hpi.write((byte) 0x50);
      hpi.write((byte) 0x49);
      hpi.write((byte) 0x48);
      for (int i = 0; i < 4; i++) hpi.write((byte) 0);
      hpi.write((byte) 0x10);
      for (int i = 0; i < 3; i++) hpi.write((byte) 0);
      for (int i = 0; i < 6; i++) hpi.write((byte) 0);
      hpi.write((byte) HASH_SLOT_COUNT);
      hpi.write((byte) (HASH_SLOT_COUNT >> 8));
      hpi.write((byte) entryCount);
      hpi.write((byte) (entryCount >> 8));
      hpi.write((byte) (entryCount >> 16));
      hpi.write((byte) (entryCount >> 24));
      int stringOfs = 0;
      int fileOfs = 0;
      HashMap<Long, ArrayList<Entry>> hashes = new HashMap<>();
      for (int f = 0; f < entryCount; f++) {
        String file = names.get(f);
        Entry e = entries.get(f);
        long h = hash(file);
        if (!hashes.containsKey(h)) hashes.put(h, new ArrayList<Entry>());
        hashes.get(h).add(e);
        if (toReplace.equals(file)) {
          byte[] unc;
          byte[] cmp;
          if (replacement.toLowerCase().contains(".png")) {
            STEX stex;
            if (!isNewFile) {
              hpbOld.seek(e.fileOfs);
              byte[] bytes;
              if (e.uncompressedSize > 0) {
                hpbOld.skipBytes(4);
                int size = (hpbOld.read() & 0xFF) |
                           (hpbOld.read() & 0xFF) << 8 |
                           (hpbOld.read() & 0xFF) << 16 |
                           (hpbOld.read() & 0xFF) << 24;
                hpbOld.skipBytes(24);
                bytes = new byte[size];
                hpbOld.read(bytes);
                bytes = RevLZ77.decompress(bytes);
              } else {
                bytes = new byte[e.fileSize];
                hpbOld.read(bytes);
              }
              stex = new STEX(bytes);
            } else {
              if (replacement.indexOf(':') > -1) {
                String[] settings = replacement.split(":");
                replacement = settings[0];
                if (settings.length == 2) {
                  stex = new STEX(settings[1]);
                } else {
                  stex = new STEX(settings[1], settings[2].toUpperCase());
                }
              } else {
                stex = new STEX(replacement.replace(".png", ".stex")
                                           .replace(".PNG", ".stex"));
              }
            }
            unc = stex.toBytes(replacement);
          } else {
            unc = Files.readAllBytes(Paths.get(replacement));
          }
          try {
            cmp = RevLZ77.compress(unc);
          } catch (ArrayIndexOutOfBoundsException ex) {
            cmp = null;
          }
          if (cmp == null) {
            e.fileSize = unc.length;
            e.uncompressedSize = 0;
            hpb.write(unc);
          } else {
            e.fileSize = cmp.length + 0x20;
            e.uncompressedSize = unc.length;
            hpb.write((byte) 0x41);
            hpb.write((byte) 0x43);
            hpb.write((byte) 0x4D);
            hpb.write((byte) 0x50);
            hpb.write((byte) cmp.length);
            hpb.write((byte) (cmp.length >> 8));
            hpb.write((byte) (cmp.length >> 16));
            hpb.write((byte) (cmp.length >> 24));
            hpb.write((byte) 0x20);
            for (int i = 0; i < 7; i++) hpb.write((byte) 0);
            hpb.write((byte) unc.length);
            hpb.write((byte) (unc.length >> 8));
            hpb.write((byte) (unc.length >> 16));
            hpb.write((byte) (unc.length >> 24));
            for (int i = 0; i < 3; i++) {
              hpb.write((byte) 0x67);
              hpb.write((byte) 0x45);
              hpb.write((byte) 0x23);
              hpb.write((byte) 0x01);
            }
            hpb.write(cmp);
          }
        } else {
          hpbOld.seek(e.fileOfs);
          byte[] bytes = new byte[e.fileSize];
          hpbOld.read(bytes);
          hpb.write(bytes);
        }
        e.fileOfs = fileOfs;
        fileOfs += e.fileSize;
      }
      hpb.close();
      hpbOld.close();
      int ofs = 0;
      for (long i = 0; i < HASH_SLOT_COUNT; i++) {
        int count;
        if (hashes.containsKey(i)) count = hashes.get(i).size();
        else count = 0;
        hpi.write((byte) ofs);
        hpi.write((byte) (ofs >> 8));
        hpi.write((byte) count);
        hpi.write((byte) (count >> 8));
        ofs += count;
      }
      for (long i = 0; i < HASH_SLOT_COUNT; i++) {
        if (!hashes.containsKey(i)) continue;
        for (Entry e : hashes.get(i)) {
          hpi.write((byte) e.stringOfs);
          hpi.write((byte) (e.stringOfs >> 8));
          hpi.write((byte) (e.stringOfs >> 16));
          hpi.write((byte) (e.stringOfs >> 24));
          hpi.write((byte) e.fileOfs);
          hpi.write((byte) (e.fileOfs >> 8));
          hpi.write((byte) (e.fileOfs >> 16));
          hpi.write((byte) (e.fileOfs >> 24));
          hpi.write((byte) e.fileSize);
          hpi.write((byte) (e.fileSize >> 8));
          hpi.write((byte) (e.fileSize >> 16));
          hpi.write((byte) (e.fileSize >> 24));
          hpi.write((byte) e.uncompressedSize);
          hpi.write((byte) (e.uncompressedSize >> 8));
          hpi.write((byte) (e.uncompressedSize >> 16));
          hpi.write((byte) (e.uncompressedSize >> 24));
        }
      }
      for (String file : names) {
        hpi.write(file.getBytes(Charset.forName("SJIS")));
        hpi.write((byte) 0);
      }
      hpi.close();
      Files.delete(Paths.get(name + ".HPI"));
      Files.delete(Paths.get(name + ".HPB"));
      Files.move(Paths.get(name + ".tmpi"), Paths.get(name + ".HPI"));
      Files.move(Paths.get(name + ".tmpb"), Paths.get(name + ".HPB"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public void batchReplace(String cfg) {
    try {
      List<String> lines;
      lines = Files.readAllLines(Paths.get(cfg), Charset.forName("UTF-8"));
      int s = lines.size();
      for (String line : lines) if (line.isEmpty() || line.startsWith("#")) s--;
      String[] toReplace = new String[s];
      String[] replacement = new String[s];
      for (int i = 0; i < s; i++) {
        if (lines.get(i).isEmpty() || lines.get(i).startsWith("#")) continue;
        String[] rep = lines.get(i).split(" : ");
        toReplace[i] = rep[0];
        replacement[i] = rep[1];
      }
      replace(toReplace, replacement);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public boolean containsFile(String file) {
    for (String f : names) if (f.equals(file)) return true;
    return false;
  }
  
  public int nextStringOfs() {
    int stringOfs = 0;
    for (String name : names) {
      stringOfs += name.getBytes(Charset.forName("SJIS")).length + 1;
    }
    return stringOfs;
  }
  
  public void importDirectory(String dir) {
    if (dir.endsWith("/")) dir = dir.substring(0, dir.length() - 1);
    ArrayList<String> files = new ArrayList<>();
    getFiles(files, Paths.get(dir));
    String[] targets = new String[files.size()];
    String[] sources = new String[files.size()];
    int dirLength = dir.length() + 1;
    for (int i = 0; i < targets.length; i++) {
      sources[i] = files.get(i);
      if (sources[i].toLowerCase().endsWith(".png")) {
        targets[i] = sources[i].substring(dirLength, sources[i].length() - 4);
      } else {
        targets[i] = sources[i].substring(dirLength);
      }
    }
    replace(targets, sources);
  }
  
  public void replace(String[] toReplace, String[] replacements) {
    ArrayList<String> targets = new ArrayList<>();
    ArrayList<String> sources = new ArrayList<>();
    for (int i = 0; i < toReplace.length; i++) {
      targets.add(toReplace[i]);
      sources.add(replacements[i]);
    }
    ArrayList<Boolean> isNewFile = new ArrayList<>();
    for (String t : targets) {
      if (containsFile(t)) {
        isNewFile.add(false);
        continue;
      }
      isNewFile.add(true);
      entryCount++;
      Entry e = new Entry();
      e.stringOfs = nextStringOfs();
      entries.add(e);
      names.add(t);
    }
    try {
      FileOutputStream hpi = new FileOutputStream(name + ".tmpi");
      FileOutputStream hpb = new FileOutputStream(name + ".tmpb");
      RandomAccessFile hpbOld = new RandomAccessFile(name + ".HPB", "r");
      hpi.write((byte) 0x48);
      hpi.write((byte) 0x50);
      hpi.write((byte) 0x49);
      hpi.write((byte) 0x48);
      for (int i = 0; i < 4; i++) hpi.write((byte) 0);
      hpi.write((byte) 0x10);
      for (int i = 0; i < 3; i++) hpi.write((byte) 0);
      for (int i = 0; i < 6; i++) hpi.write((byte) 0);
      hpi.write((byte) HASH_SLOT_COUNT);
      hpi.write((byte) (HASH_SLOT_COUNT >> 8));
      hpi.write((byte) entryCount);
      hpi.write((byte) (entryCount >> 8));
      hpi.write((byte) (entryCount >> 16));
      hpi.write((byte) (entryCount >> 24));
      int stringOfs = 0;
      int fileOfs = 0;
      HashMap<Long, ArrayList<Entry>> hashes = new HashMap<>();
      for (int f = 0; f < entryCount; f++) {
        String file = names.get(f);
        Entry e = entries.get(f);
        long h = hash(file);
        if (!hashes.containsKey(h)) hashes.put(h, new ArrayList<Entry>());
        hashes.get(h).add(e);
        if (targets.contains(file)) {
          String replacement = sources.get(targets.indexOf(file));
          byte[] unc;
          byte[] cmp;
          if (replacement.toLowerCase().contains(".png")) {
            STEX stex;
            if (!isNewFile.get(targets.indexOf(file))) {
              hpbOld.seek(e.fileOfs);
              byte[] bytes;
              if (e.uncompressedSize > 0) {
                hpbOld.skipBytes(4);
                int size = (hpbOld.read() & 0xFF) | (hpbOld.read() & 0xFF) << 8 |
                           (hpbOld.read() & 0xFF) << 16 |
                           (hpbOld.read() & 0xFF) << 24;
                hpbOld.skipBytes(24);
                bytes = new byte[size];
                hpbOld.read(bytes);
                bytes = RevLZ77.decompress(bytes);
              } else {
                bytes = new byte[e.fileSize];
                hpbOld.read(bytes);
              }
              stex = new STEX(bytes);
            } else {
              if (replacement.indexOf(':') > -1) {
                String[] settings = replacement.split(":");
                replacement = settings[0];
                if (settings.length == 2) {
                  stex = new STEX(settings[1]);
                } else {
                  stex = new STEX(settings[1], settings[2].toUpperCase());
                }
              } else {
                stex = new STEX(replacement.replace(".png", ".stex"));
              }
            }
            unc = stex.toBytes(replacement);
          } else {
            unc = Files.readAllBytes(Paths.get(replacement));
          }
          try {
            cmp = RevLZ77.compress(unc);
          } catch (ArrayIndexOutOfBoundsException ex) {
            cmp = null;
          }
          if (cmp == null) {
            e.fileSize = unc.length;
            e.uncompressedSize = 0;
            hpb.write(unc);
          } else {
            e.fileSize = cmp.length + 0x20;
            e.uncompressedSize = unc.length;
            hpb.write((byte) 0x41);
            hpb.write((byte) 0x43);
            hpb.write((byte) 0x4D);
            hpb.write((byte) 0x50);
            hpb.write((byte) cmp.length);
            hpb.write((byte) (cmp.length >> 8));
            hpb.write((byte) (cmp.length >> 16));
            hpb.write((byte) (cmp.length >> 24));
            hpb.write((byte) 0x20);
            for (int i = 0; i < 7; i++) hpb.write((byte) 0);
            hpb.write((byte) unc.length);
            hpb.write((byte) (unc.length >> 8));
            hpb.write((byte) (unc.length >> 16));
            hpb.write((byte) (unc.length >> 24));
            for (int i = 0; i < 3; i++) {
              hpb.write((byte) 0x67);
              hpb.write((byte) 0x45);
              hpb.write((byte) 0x23);
              hpb.write((byte) 0x01);
            }
            hpb.write(cmp);
          }
        } else {
          hpbOld.seek(e.fileOfs);
          byte[] bytes = new byte[e.fileSize];
          hpbOld.read(bytes);
          hpb.write(bytes);
        }
        e.fileOfs = fileOfs;
        fileOfs += e.fileSize;
      }
      hpb.close();
      hpbOld.close();
      int ofs = 0;
      for (long i = 0; i < HASH_SLOT_COUNT; i++) {
        int count;
        if (hashes.containsKey(i)) count = hashes.get(i).size();
        else count = 0;
        hpi.write((byte) ofs);
        hpi.write((byte) (ofs >> 8));
        hpi.write((byte) count);
        hpi.write((byte) (count >> 8));
        ofs += count;
      }
      for (long i = 0; i < HASH_SLOT_COUNT; i++) {
        if (!hashes.containsKey(i)) continue;
        for (Entry e : hashes.get(i)) {
          hpi.write((byte) e.stringOfs);
          hpi.write((byte) (e.stringOfs >> 8));
          hpi.write((byte) (e.stringOfs >> 16));
          hpi.write((byte) (e.stringOfs >> 24));
          hpi.write((byte) e.fileOfs);
          hpi.write((byte) (e.fileOfs >> 8));
          hpi.write((byte) (e.fileOfs >> 16));
          hpi.write((byte) (e.fileOfs >> 24));
          hpi.write((byte) e.fileSize);
          hpi.write((byte) (e.fileSize >> 8));
          hpi.write((byte) (e.fileSize >> 16));
          hpi.write((byte) (e.fileSize >> 24));
          hpi.write((byte) e.uncompressedSize);
          hpi.write((byte) (e.uncompressedSize >> 8));
          hpi.write((byte) (e.uncompressedSize >> 16));
          hpi.write((byte) (e.uncompressedSize >> 24));
        }
      }
      for (String file : names) {
        hpi.write(file.getBytes(Charset.forName("SJIS")));
        hpi.write((byte) 0);
      }
      hpi.close();
      Files.delete(Paths.get(name + ".HPI"));
      Files.delete(Paths.get(name + ".HPB"));
      Files.move(Paths.get(name + ".tmpi"), Paths.get(name + ".HPI"));
      Files.move(Paths.get(name + ".tmpb"), Paths.get(name + ".HPB"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public static void save(String dir, String out) {
    if (dir.endsWith("/")) dir = dir.substring(0, dir.length() - 1);
    try (FileOutputStream hpi = new FileOutputStream(out + ".HPI");
         FileOutputStream hpb = new FileOutputStream(out + ".HPB")) {
      hpi.write((byte) 0x48);
      hpi.write((byte) 0x50);
      hpi.write((byte) 0x49);
      hpi.write((byte) 0x48);
      for (int i = 0; i < 4; i++) hpi.write((byte) 0);
      hpi.write((byte) 0x10);
      for (int i = 0; i < 3; i++) hpi.write((byte) 0);
      for (int i = 0; i < 6; i++) hpi.write((byte) 0);
      hpi.write((byte) HASH_SLOT_COUNT);
      hpi.write((byte) (HASH_SLOT_COUNT >> 8));
      ArrayList<String> files = new ArrayList<>();
      getFiles(files, Paths.get(dir));
      hpi.write((byte) files.size());
      hpi.write((byte) (files.size() >> 8));
      hpi.write((byte) (files.size() >> 16));
      hpi.write((byte) (files.size() >> 24));
      int stringOfs = 0;
      int fileOfs = 0;
      HashMap<Long, ArrayList<Entry>> hashes = new HashMap<>();
      for (String file : files) {
        Entry e = new Entry();
        e.stringOfs = stringOfs;
        String path = file.substring(dir.length() + 1);
        stringOfs += path.getBytes(Charset.forName("SJIS")).length + 1;
        long h = hash(path);
        if (!hashes.containsKey(h)) hashes.put(h, new ArrayList<Entry>());
        hashes.get(h).add(e);
        e.fileOfs = fileOfs;
        byte[] unc = Files.readAllBytes(Paths.get(file));
        if (file.toLowerCase().endsWith(".stex") &&
            new File(file + ".png").exists()) {
          STEX stex = new STEX(unc);
          unc = stex.toBytes(file + ".png");
        }
        byte[] cmp;
        try {
          cmp = RevLZ77.compress(unc);
        } catch (ArrayIndexOutOfBoundsException ex) {
          cmp = null;
        }
        if (cmp == null) {
          e.fileSize = unc.length;
          e.uncompressedSize = 0;
          hpb.write(unc);
        } else {
          e.fileSize = cmp.length + 0x20;
          e.uncompressedSize = unc.length;
          hpb.write((byte) 0x41);
          hpb.write((byte) 0x43);
          hpb.write((byte) 0x4D);
          hpb.write((byte) 0x50);
          hpb.write((byte) cmp.length);
          hpb.write((byte) (cmp.length >> 8));
          hpb.write((byte) (cmp.length >> 16));
          hpb.write((byte) (cmp.length >> 24));
          hpb.write((byte) 0x20);
          for (int i = 0; i < 7; i++) hpb.write((byte) 0);
          hpb.write((byte) unc.length);
          hpb.write((byte) (unc.length >> 8));
          hpb.write((byte) (unc.length >> 16));
          hpb.write((byte) (unc.length >> 24));
          for (int i = 0; i < 3; i++) {
            hpb.write((byte) 0x67);
            hpb.write((byte) 0x45);
            hpb.write((byte) 0x23);
            hpb.write((byte) 0x01);
          }
          hpb.write(cmp);
        }
        fileOfs += e.fileSize;
      }
      int ofs = 0;
      for (long i = 0; i < HASH_SLOT_COUNT; i++) {
        int count;
        if (hashes.containsKey(i)) count = hashes.get(i).size();
        else count = 0;
        hpi.write((byte) ofs);
        hpi.write((byte) (ofs >> 8));
        hpi.write((byte) count);
        hpi.write((byte) (count >> 8));
        ofs += count;
      }
      for (long i = 0; i < HASH_SLOT_COUNT; i++) {
        if (!hashes.containsKey(i)) continue;
        for (Entry e : hashes.get(i)) {
          hpi.write((byte) e.stringOfs);
          hpi.write((byte) (e.stringOfs >> 8));
          hpi.write((byte) (e.stringOfs >> 16));
          hpi.write((byte) (e.stringOfs >> 24));
          hpi.write((byte) e.fileOfs);
          hpi.write((byte) (e.fileOfs >> 8));
          hpi.write((byte) (e.fileOfs >> 16));
          hpi.write((byte) (e.fileOfs >> 24));
          hpi.write((byte) e.fileSize);
          hpi.write((byte) (e.fileSize >> 8));
          hpi.write((byte) (e.fileSize >> 16));
          hpi.write((byte) (e.fileSize >> 24));
          hpi.write((byte) e.uncompressedSize);
          hpi.write((byte) (e.uncompressedSize >> 8));
          hpi.write((byte) (e.uncompressedSize >> 16));
          hpi.write((byte) (e.uncompressedSize >> 24));
        }
      }
      for (String file : files) {
        String path = file.substring(dir.length() + 1);
        hpi.write(path.getBytes(Charset.forName("SJIS")));
        hpi.write((byte) 0);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
