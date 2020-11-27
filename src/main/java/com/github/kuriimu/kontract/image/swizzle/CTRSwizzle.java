package com.github.kuriimu.kontract.image.swizzle;

import java.awt.Point;
import java.util.ArrayList;

import com.github.kuriimu.kontract.interfaces.image.IImageSwizzle;

public class CTRSwizzle extends IImageSwizzle {
  protected byte _orientation;
  protected MasterSwizzle _zorder;
  
  public CTRSwizzle(int w, int h) {
    this(w, h, (byte) 0, true);
  }
  
  public CTRSwizzle(int w, int h, byte o) {
    this(w, h, o, true);
  }
  
  public CTRSwizzle(int w, int h, byte o, boolean toPowerOf2) {
    width = (toPowerOf2 ? 2 << (int) (Math.log(w - 1) / Math.log(2)) : w);
    height = (toPowerOf2 ? 2 << (int) (Math.log(h - 1) / Math.log(2)) : h);
    
    _orientation = o;
    ArrayList<Point> points = new ArrayList<>();
    points.add(new Point(1, 0));
    points.add(new Point(0, 1));
    points.add(new Point(2, 0));
    points.add(new Point(0, 2));
    points.add(new Point(4, 0));
    points.add(new Point(0, 4));
    _zorder = new MasterSwizzle(_orientation == 0 ? width : height,
                                new Point(0, 0), points);
  }
  
  public Point get(Point point) {
    int pointCount = ((int) point.getY()) * width + (int) point.getX();
    Point newPoint = _zorder.get(pointCount);
    
    switch (_orientation) {
      case 8: return new Point((int) newPoint.getY(), (int) newPoint.getX());
      case 4: return new Point((int) newPoint.getY(),
                               height - 1 - (int) newPoint.getX());
      default: return newPoint;
    }
  }
}
