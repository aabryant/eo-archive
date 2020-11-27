package com.github.kuriimu.kontract.image;

import java.util.function.Function;
import javafx.scene.paint.Color;

import com.github.kuriimu.kontract.interfaces.image.IImageFormat;
import com.github.kuriimu.kontract.interfaces.image.IImageSwizzle;

public class ImageSettings {
  public int width;
  public int height;
  public int offset;
  public IImageFormat format;
  public int padWidth = 0;
  public int padHeight = 0;
  public IImageSwizzle swizzle;
  public Function<Color,Color> pixelShader;
  public boolean ignoreOpacity;
}
