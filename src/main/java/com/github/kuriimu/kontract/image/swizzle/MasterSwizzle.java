package com.github.kuriimu.kontract.image.swizzle;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;

public class MasterSwizzle {
  protected ArrayList<Point> _bitFieldCoords;
  protected ArrayList<Point> _initPointTransformOnY;
  
  public int macroTileWidth;
  public int macroTileHeight;
  
  protected int _widthInTiles;
  protected Point _init;
  
  public MasterSwizzle(int stride, Point init, ArrayList<Point> coords) {
    this(stride, init, coords, null);
  }
  
  public MasterSwizzle(int stride, Point init, ArrayList<Point> coords,
                       ArrayList<Point> tOnY) {
    _bitFieldCoords = coords;
    _initPointTransformOnY = (tOnY == null ? new ArrayList<>() : tOnY);
    
    _init = init;
    
    macroTileWidth = 0;
    macroTileHeight = 0;
    for (Point p : coords) {
      macroTileWidth = macroTileWidth | (int) p.getX();
      macroTileHeight = macroTileHeight | (int) p.getY();
    }
    macroTileWidth += 1;
    macroTileHeight += 1;
    _widthInTiles = (stride + macroTileWidth - 1) / macroTileWidth;
  }
  
  public Point get(int pointCount) {
    int macroTileCount = pointCount / macroTileWidth / macroTileHeight;
    int macroX = macroTileCount % _widthInTiles;
    int macroY = macroTileCount / _widthInTiles;
    
    ArrayList<Point> list = new ArrayList<>();
    list.add(new Point(macroX * macroTileWidth, macroY * macroTileHeight));
    for (int i = 0; i < _bitFieldCoords.size(); i++) {
      if (((pointCount >> i) % 2) == 1) list.add(_bitFieldCoords.get(i));
    }
    for (int i = 0; i < _initPointTransformOnY.size(); i++) {
      if (((macroY >> i) % 2) == 1) list.add(_initPointTransformOnY.get(i));
    }
    int x = (int) _init.getX();
    int y = (int) _init.getY();
    for (Point p : list) {
      x = (x ^ (int) p.getX());
      y = (y ^ (int) p.getY());
    }
    return new Point(x, y);
  }
}
