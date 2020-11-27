/*
 * Copyright (C) 2020 Amy Bryant
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.aabryant.eo.archive;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import javafx.stage.Stage;

import com.github.kuriimu.archive.HpiHpb;
import com.github.kuriimu.image.atlus.STEX;
import com.github.kuriimu.kontract.compression.RevLZ77;

public class Main extends Application {
  
  protected static void printHelp() {
    System.out.println("Etrian Odyssey Archive Tool v1.2");
    System.out.println();
    System.out.print("This tool can pack and unpack the HPI/HPB archive");
    System.out.println(" format used by the 3DS Etrian");
    System.out.println("Odyssey games.");
    System.out.println();
    System.out.print("Third-party licenses can be found in the");
    System.out.println(" 3rdparty-license directory.");
    System.out.println();
    System.out.print("If you are on a Mac or non-Ubuntu Linux, please");
    System.out.println(" compile a copy of");
    System.out.print("[tex3ds](https://github.com/devkitPro/tex3ds) and ");
    System.out.println("place it in the external");
    System.out.print("directory. It is used for ETC1 encoding when ");
    System.out.println("importing files to STEX.");
    System.out.println();
    System.out.println("Usage: eo-archive.jar [OPTIONS...]");
    System.out.println("  Options:");
    System.out.println("    -x,   --extract <archive> <out>");
    System.out.print("      Extracts the contents of the archive <archive>");
    System.out.println(" to the directory <out>.");
    System.out.print("      <archive> should be the name of the archive");
    System.out.println(" without the HPI or HPB");
    System.out.println("      extension.");
    System.out.println();
    System.out.println("    -xa,  --extract-all <archive> <out>");
    System.out.print("      Extracts the contents of the archive <archive> to");
    System.out.println(" the directory <out>,");
    System.out.print("      going a step further by extracting all STEX");
    System.out.println(" files to PNG files. In future");
    System.out.print("      versions of this tool, this may also do such");
    System.out.println(" things as extract MBM files");
    System.out.println("      to TXT files.");
    System.out.println();
    System.out.print("      <archive> should be the name of the archive ");
    System.out.println("without an HPI or HPB");
    System.out.println("      extension.");
    System.out.println();
    System.out.print("      Please be aware that this can be quite slow for");
    System.out.println(" large archives, such as");
    System.out.print("      a main game archive. By default, the Etrian");
    System.out.println(" Odyssey Nexus MORIS archive");
    System.out.print("      contains 1900 STEX files and requires nearly a");
    System.out.println(" minute and a half to");
    System.out.println("      extract all of them.");
    System.out.println();
    System.out.println("    -p,   --pack <dir> <archive>");
    System.out.print("      Packs the contents of <dir> into the archive");
    System.out.println(" <archive>, ignoring all");
    System.out.print("      files that are not of types used by Etrian");
    System.out.println(" Odyssey games. A full list");
    System.out.print("      of extensions that will be packed into the");
    System.out.println(" archive can be found at the");
    System.out.println("      end of this help text.");
    System.out.println();
    System.out.print("      <archive> should be the name of the archive");
    System.out.println(" without an HPI or HPB");
    System.out.println("      extension.");
    System.out.println();
    System.out.print("      If any PNG files exist with the same path as");
    System.out.println(" an STEX file (e.g.");
    System.out.print("      STEX/EXAMPLE.STEX.png for STEX/EXAMPLE.STEX)");
    System.out.println(" they will be imported");
    System.out.print("      in the place of the STEX file, using the STEX");
    System.out.println(" file's internal name");
    System.out.print("      and image format. PNG files that do not");
    System.out.println(" correspond to an existing STEX");
    System.out.println("      file will not be imported.");
    System.out.println();
    System.out.print("      Please be aware that packing a large");
    System.out.println(" directory can take some time. It");
    System.out.print("      is primarily useful for creating a brand new DLC");
    System.out.println(" or removing files, as");
    System.out.print("      a deletion command is not yet part of the tool");
    System.out.println(" (though it will be in");
    System.out.print("      an upcoming release). For all other cases, file");
    System.out.println(" replacement and");
    System.out.println("      directory importing will generally be superior.");
    System.out.println();
    System.out.println("    -r,   --replace <archive> <file> <replacement>");
    System.out.print("      Replaces the file <file> inside of the archive");
    System.out.println(" <archive> with the file");
    System.out.print("      <replacement>. If <file> does not exist,");
    System.out.println(" <replacement> will simply be");
    System.out.println("      added to the archive with the path <file>.");
    System.out.println();
    System.out.print("      <archive> should be the name of the archive");
    System.out.println(" without an HPI or HPB");
    System.out.println("      extension.");
    System.out.println();
    System.out.print("      If <replacement> is a PNG file, it will be");
    System.out.println(" imported as an STEX file. If");
    System.out.print("      it is replacing a file, it will use the internal");
    System.out.println(" name and format of the");
    System.out.print("      STEX file in the archive. If it is creating a new");
    System.out.println(" file, it will default");
    System.out.print("      to using its own name as its internal name and");
    System.out.println(" the RGBA8888 format, but");
    System.out.print("      both of these can be customized (and the internal");
    System.out.println(" name almost certainly");
    System.out.print("      should) like so: <replacement>:<internal-name>:");
    System.out.println("<format>. A full list of");
    System.out.print("      valid image formats can be found at the end of");
    System.out.println(" this help text.");
    System.out.println();
    System.out.println("    -br,  --batch-replace <archive> <file>");
    System.out.print("      Replaces multiple files inside of the archive");
    System.out.println(" <archive> based on the");
    System.out.println("      contents of the text file <file>.");
    System.out.println();
    System.out.print("      <archive> should be the name of the archive");
    System.out.println(" without an HPI or HPB");
    System.out.println("      extension.");
    System.out.println();
    System.out.println("      <file> should contain a list like this:");
    System.out.println();
    System.out.print("      CHARMAKE/CHARIMGFORM.TBL : work/CHARMAKE/CHAR");
    System.out.println("IMGFORM.TBL");
    System.out.print("      CHARMAKE/CHARIMGMENU.TBL : work/CHARMAKE/CHAR");
    System.out.println("IMGMENU.TBL");
    System.out.println("      STEX/EXAMPLE.STEX : EXAMPLE.png");
    System.out.print("      STEX/NEW_STEX.STEX : NEW_STEX.png:Stex/new_stex.");
    System.out.println("stex:ETC1A4");
    System.out.println();
    System.out.print("      Lines are written in the form <file> : ");
    System.out.println("<replacement> and follow all of");
    System.out.print("      the rules laid out for individual replacement, ");
    System.out.println("including adding files");
    System.out.print("      and automatic PNG importing. Empty lines and ");
    System.out.println("comments prefaced with #");
    System.out.println("      may be included for formatting.");
    System.out.println();
    System.out.println("    -i,   --import <dir> <archive>");
    System.out.print("      Imports the contents of <dir> into <archive>, ");
    System.out.println("following all of the rules");
    System.out.print("      of archive packing. <dir> must be structured in");
    System.out.println(" the same way as <archive>,");
    System.out.print("      and files with extensions other than those that");
    System.out.println(" would be collected by");
    System.out.println("      the pack command will be ignored.");
    System.out.println();
    System.out.print("      As with packing, PNG files will be imported when");
    System.out.println(" they correspond to");
    System.out.print("      existing STEX files, but not if they would create");
    System.out.println(" a new file.");
    System.out.println();
    System.out.print("      <archive> should be the name of the archive");
    System.out.println(" without an HPI or HPB");
    System.out.println("      extension.");
    System.out.println();
    System.out.println("    -sx,  --stex-export <file>");
    System.out.print("      Exports the contents of the STEX file <file> as a");
    System.out.println(" PNG file with the name");
    System.out.print("      <file>.png such that it can be easily imported");
    System.out.println(" with replacement or");
    System.out.println("      directory importing.");
    System.out.println();
    System.out.println("    -si,  --stex-import <file> <image>");
    System.out.print("      Imports the image file <image> into the existing");
    System.out.println(" STEX file <file>. This");
    System.out.println("      will use <file>'s internal name and image format.");
    System.out.println();
    System.out.println("    -sc,  --stex-create <file> <name> <format>");
    System.out.print("      Creates a new STEX file from the PNG file <file>");
    System.out.println(" with the internal name");
    System.out.print("      <name> and the format <format>. A full list of ");
    System.out.println("valid formats can be");
    System.out.print("      found at the end of this help text. The file name");
    System.out.println(" of the created STEX");
    System.out.print("      will be that of the image file but with PNG");
    System.out.println(" swapped for STEX, except for");
    System.out.print("      in the cases of files ending with .STEX.PNG,");
    System.out.println(" which will instead simply");
    System.out.println("      drop .PNG.");
    System.out.println();
    System.out.print("      <name> and <format> may be omitted, though the");
    System.out.println(" use of <name> is heavily");
    System.out.println("      encouraged.");
    System.out.println();
    System.out.println("    -lzx, --lz-extract <file> <out>");
    System.out.print("      Extracts the contents of the RevLZ77-compressed");
    System.out.println(" file <file> to the file");
    System.out.println("      <out>.");
    System.out.println();
    System.out.println("    -lzp, --lz-pack <file> <out>");
    System.out.print("      Compresses the file <file> to the RevLZ77-");
    System.out.println("compressed file <out>.");
    System.out.println();
    System.out.println("  Valid File Formats:");
    System.out.println("    .bam2");
    System.out.println("    .bcfnt");
    System.out.println("    .bch");
    System.out.println("    .bchenv");
    System.out.println("    .bcwav");
    System.out.println("    .bf");
    System.out.println("    .bmp");
    System.out.println("    .cbmd");
    System.out.println("    .ctpk");
    System.out.println("    .dat");
    System.out.println("    .epl");
    System.out.println("    .mbm");
    System.out.println("    .mgb");
    System.out.println("    .msb");
    System.out.println("    .ssb");
    System.out.println("    .ssnd");
    System.out.println("    .stex");
    System.out.println("    .tbl");
    System.out.println("    .tgd");
    System.out.println("    .tmx");
    System.out.println("    .ttd");
    System.out.println();
    System.out.println("  Valid STEX Image Formats:");
    System.out.println("    RGBA8888");
    System.out.println("    RGBA4444");
    System.out.println("    RGBA5551");
    System.out.println("    RGB888");
    System.out.println("    RGB565");
    System.out.println("    A8");
    System.out.println("    A4");
    System.out.println("    L8");
    System.out.println("    L4");
    System.out.println("    LA88");
    System.out.println("    LA44");
    System.out.println("    ETC1");
    System.out.println("    ETC1A4");
  }
  
  static String[] ARGS;
  
  public static void extractSTEX(String path) throws IOException {
    STEX stex = new STEX(Files.readAllBytes(Paths.get(path)));
    File file = new File(path + ".png");
    ImageIO.write(SwingFXUtils.fromFXImage(stex.bmp, null), "png", file);
  }
  
  public static void importSTEX(String path, String img) throws IOException {
    new STEX(Files.readAllBytes(Paths.get(path))).save(path, img);
  }
  
  public static void createSTEX(String path) throws IOException {
    createSTEX(path, path.replace(".png", ".stex"), "RGBA8888");
  }
  
  public static void createSTEX(String path, String name) throws IOException {
    createSTEX(path, name, "RGBA8888");
  }
  
  public static void createSTEX(String path, String name,
                                String format) throws IOException {
    String outname;
    if (path.toLowerCase().endsWith(".stex.png")) {
      outname = path.substring(0, path.length() - 4);
    } else {
      outname = path.replace(".png", ".stex").replace(".PNG", ".STEX");
    }
    new STEX(name, format.toUpperCase()).save(outname, path);
  }
  
  @Override
  public void start(Stage primaryStage) throws IOException {
    if (ARGS.length == 0) {
      printHelp();
      System.exit(1);
    }
    HpiHpb arc;
    switch (ARGS[0]) {
      case "-x":
      case "--extract":
        if (ARGS.length == 3) {
          arc = HpiHpb.load(ARGS[1]);
          arc.extract(ARGS[2]);
        } else {
          printHelp();
        }
        break;
      case "-xa":
      case "--extract-all":
        if (ARGS.length == 3) {
          arc = HpiHpb.load(ARGS[1]);
          arc.extract(ARGS[2], true);
        } else {
          printHelp();
        }
        break;
      case "-p":
      case "--pack":
        if (ARGS.length == 3) {
          HpiHpb.save(ARGS[1], ARGS[2]);
        } else {
          printHelp();
        }
        break;
      case "-r":
      case "--replace":
        if (ARGS.length == 4) {
          arc = HpiHpb.load(ARGS[1]);
          arc.replace(ARGS[2], ARGS[3]);
        } else {
          printHelp();
        }
        break;
      case "-br":
      case "--batch-replace":
        if (ARGS.length == 3) {
          arc = HpiHpb.load(ARGS[1]);
          arc.batchReplace(ARGS[2]);
        } else {
          printHelp();
        }
        break;
      case "-i":
      case "--import":
        if (ARGS.length == 3) {
          arc = HpiHpb.load(ARGS[2]);
          arc.importDirectory(ARGS[1]);
        } else {
          printHelp();
        }
        break;
      case "-sx":
      case "--stex-export":
        if (ARGS.length == 2) {
          extractSTEX(ARGS[1]);
        } else {
          printHelp();
        }
        break;
      case "-si":
      case "--stex-import":
        if (ARGS.length == 3) {
          importSTEX(ARGS[1], ARGS[2]);
        } else {
          printHelp();
        }
        break;
      case "-sc":
      case "--stex-create":
        if (ARGS.length == 2) createSTEX(ARGS[1]);
        else if (ARGS.length == 3) createSTEX(ARGS[1], ARGS[2]);
        else if (ARGS.length == 4) createSTEX(ARGS[1], ARGS[2], ARGS[3]);
        else printHelp();
        break;
      case "-lzx":
      case "--lz-extract":
        if (ARGS.length == 3) {
          RevLZ77.unpack(ARGS[1], ARGS[2]);
        }
        break;
      case "-lzp":
      case "--lz-pack":
        if (ARGS.length == 3) {
          RevLZ77.pack(ARGS[1], ARGS[2]);
        } else {
          printHelp();
        }
        break;
      default:
        printHelp();
    }
    Platform.exit();
  }
  
  
  public static void main(String[] args) {
    ARGS = args;
    launch(args);
  }
}
