package com.github.kuriimu.kontract.interfaces.image;

import java.awt.Point;

public abstract class IImageSwizzle {
  public int width;
  public int height;
  
  public abstract Point get(Point point);
}
