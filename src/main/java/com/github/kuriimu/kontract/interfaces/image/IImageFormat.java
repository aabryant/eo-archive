package com.github.kuriimu.kontract.interfaces.image;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import com.github.kuriimu.kontract.image.ImageSettings;
import com.github.kuriimu.kontract.io.BinaryReaderX;

public abstract class IImageFormat {
  public int bitDepth;
  public String formatName;
  
  public WritableImage loadImage(byte[] bytes,
                                 ImageSettings opt) throws Exception {
    int width = opt.width;
    int height = opt.height;
    
    ArrayList<Point> points = getPointSequence(opt);
    ArrayList<Color> colors = loadColors(bytes, opt.ignoreOpacity);
    ArrayList<PointData> pairs = new ArrayList<>();
    for (int i = 0; i < points.size() && i < colors.size(); i++) {
      pairs.add(new PointData(points.get(i), colors.get(i)));
    }
    WritableImage bmp = new WritableImage(width, height);
    PixelWriter writer = bmp.getPixelWriter();
    for (PointData pair : pairs) {
      int x = (int) pair.point.getX();
      int y = (int) pair.point.getY();
      if (0 <= x && x < width && 0 <= y && y < height) {
        Color color = pair.color;
        if (opt.pixelShader != null) color = opt.pixelShader.apply(color);
        writer.setColor(x, y, color);
      }
    }
    return bmp;
  }
  
  protected static ArrayList<Point> getPointSequence(ImageSettings opt) {
    int strideWidth = (opt.swizzle != null ? opt.swizzle.width : opt.width);
    int strideHeight = (opt.swizzle != null ? opt.swizzle.height : opt.height);
    
    ArrayList<Point> out = new ArrayList<>();
    for (int i = 0; i < strideWidth * strideHeight; i++) {
      Point point = new Point(i % strideWidth, i / strideWidth);
      if (opt.swizzle != null) point = opt.swizzle.get(point);
      out.add(point);
    }
    return out;
  }
  
  protected ArrayList<Color> loadColors(byte[] tex) throws Exception,
                                                           IOException {
    return loadColors(tex, false);
  }
  
  protected abstract ArrayList<Color> loadColors(byte[] tex,
                                                 boolean ignoreOpacity)
                                                 throws Exception, IOException;
  
  public byte[] save(Image img, ImageSettings settings) {
    ArrayList<Point> points = getPointSequence(settings);
    ArrayList<Color> colors = new ArrayList<>();
    PixelReader reader = img.getPixelReader();
    for (Point point : points) {
      int x = (int) point.getX();
      if (x < 0) x = 0;
      if (x > settings.width) x = settings.width;
      int y = (int) point.getY();
      if (y < 0) y = 0;
      if (y > settings.height) y = settings.height;
      colors.add(reader.getColor(x, y));
    }
    return save(colors);
  }
  
  public abstract byte[] save(ArrayList<Color> colors);
  
  protected static int changeBitDepth(int value, int bitDepthFrom,
                                      int bitDepthTo) throws Exception {
    if (bitDepthFrom < 0 || bitDepthTo < 0) {
      throw new Exception("BitDepths can't be negative!");
    }
    if (bitDepthFrom == 0 || bitDepthTo == 0) return 0;
    if (bitDepthFrom == bitDepthTo) return value;
    
    if (bitDepthFrom < bitDepthTo) {
      int fromMaxRange = (1 << bitDepthFrom) - 1;
      int toMaxRange = (1 << bitDepthTo) - 1;
      
      int div = 1;
      while (toMaxRange % fromMaxRange != 0) {
        div <<= 1;
        toMaxRange = ((toMaxRange + 1) << 1) - 1;
      }
      return value * (toMaxRange / fromMaxRange) / div;
    } else {
      int fromMax = 1 << bitDepthFrom;
      int toMax = 1 << bitDepthTo;
      
      int limit = fromMax / toMax;
      
      return value / limit;
    }
  }
  
  protected static class PointData {
    Point point;
    Color color;
    
    protected PointData(Point p, Color c) {
      point = p;
      color = c;
    }
  }
}
