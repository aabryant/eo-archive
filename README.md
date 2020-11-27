Etrian Odyssey Archive Tool v1.2
================================

This tool can pack and unpack the HPI/HPB archive format used by the 3DS Etrian
Odyssey games. It primarily serves as a more portable CLI interface for code
derived from the [Kuriimu](https://github.com/IcySon55/Kuriimu) project, albeit
with some modifications aimed at expanding its utility.

Third-party licenses can be found in the 3rdparty-license directory. The tool
itself is placed under the GPLv3.

If you are on a Mac or non-Ubuntu Linux (or building from source), please
compile a copy of [tex3ds](https://github.com/devkitPro/tex3ds) and place it in
the `external/` directory. It is used for ETC1 encoding when importing files to
STEX. You're honestly probably fine with the tex3ds binary available in the
release archive even on most other Linux distributions, but given there *have*
been a few reported issues, better safe than sorry.

If you're building from source on Windows, acquire tex3ds.exe (such as from the
release archive or from devkitPro) and place it in the `external/` directory
(potentially along with its requisite dll files, if you do not already have
those libraries in your PATH).

Usage: eo-archive.jar [OPTIONS...]
  Options:
    -x,   --extract <archive> <out>
      Extracts the contents of the archive <archive> to the directory <out>.
      <archive> should be the name of the archive without the HPI or HPB
      extension.

    -xa,  --extract-all <archive> <out>
      Extracts the contents of the archive <archive> to the directory <out>,
      going a step further by extracting all STEX files to PNG files. In future
      versions of this tool, this may also do such things as extract MBM files
      to TXT files.

      <archive> should be the name of the archive without an HPI or HPB
      extension.

      Please be aware that this can be quite slow for large archives, such as
      a main game archive. By default, the Etrian Odyssey Nexus MORIS archive
      contains 1900 STEX files and requires nearly a minute and a half to
      extract all of them.

    -p,   --pack <dir> <archive>
      Packs the contents of <dir> into the archive <archive>, ignoring all
      files that are not of types used by Etrian Odyssey games. A full list
      of extensions that will be packed into the archive can be found at the
      end of this help text.

      <archive> should be the name of the archive without an HPI or HPB
      extension.

      If any PNG files exist with the same path as an STEX file (e.g.
      STEX/EXAMPLE.STEX.png for STEX/EXAMPLE.STEX) they will be imported
      in the place of the STEX file, using the STEX file's internal name
      and image format. PNG files that do not correspond to an existing STEX
      file will not be imported.

      Please be aware that packing a large directory can take some time. It
      is primarily useful for creating a brand new DLC or removing files, as
      a deletion command is not yet part of the tool (though it will be in
      an upcoming release). For all other cases, file replacement and
      directory importing will generally be superior.

    -r,   --replace <archive> <file> <replacement>
      Replaces the file <file> inside of the archive <archive> with the file
      <replacement>. If <file> does not exist, <replacement> will simply be
      added to the archive with the path <file>.

      <archive> should be the name of the archive without an HPI or HPB
      extension.

      If <replacement> is a PNG file, it will be imported as an STEX file. If
      it is replacing a file, it will use the internal name and format of the
      STEX file in the archive. If it is creating a new file, it will default
      to using its own name as its internal name and the RGBA8888 format, but
      both of these can be customized (and the internal name almost certainly
      should) like so: <replacement>:<internal-name>:<format>. A full list of
      valid image formats can be found at the end of this help text.

    -br,  --batch-replace <archive> <file>
      Replaces multiple files inside of the archive <archive> based on the
      contents of the text file <file>.

      <archive> should be the name of the archive without an HPI or HPB
      extension.

      <file> should contain a list like this:

      CHARMAKE/CHARIMGFORM.TBL : work/CHARMAKE/CHARIMGFORM.TBL
      CHARMAKE/CHARIMGMENU.TBL : work/CHARMAKE/CHARIMGMENU.TBL
      STEX/EXAMPLE.STEX : work/STEX/EXAMPLE.png
      STEX/NEW\_STEX.STEX : work/STEX/NEW\_STEX.png:Stex/new_stex.stex:ETC1A4

      Lines are written in the form <file> : <replacement> and follow all of
      the rules laid out for individual replacement, including adding files
      and automatic PNG importing. Empty lines and comments prefaced with #
      may be included for formatting.

    -i,   --import <dir> <archive>
      Imports the contents of <dir> into <archive>, following all of the rules
      of archive packing. <dir> must be structured in the same way as <archive>,
      and files with extensions other than those that would be collected by
      the pack command will be ignored.

      As with packing, PNG files will be imported when they correspond to
      existing STEX files, but not if they would create a new file.

      <archive> should be the name of the archive without an HPI or HPB
      extension.

    -sx,  --stex-export <file>
      Exports the contents of the STEX file <file> as a PNG file with the name
      <file>.png such that it can be easily imported with replacement or
      directory importing.

    -si,  --stex-import <file> <image>
      Imports the image file <image> into the existing STEX file <file>. This
      will use <file>'s internal name and image format.

    -sc,  --stex-create <file> <name> <format>
      Creates a new STEX file from the PNG file <file> with the internal name
      <name> and the format <format>. A full list of valid formats can be
      found at the end of this help text. The file name of the created STEX
      will be that of the image file but with PNG swapped for STEX, except for
      in the cases of files ending with .STEX.PNG, which will instead simply
      drop .PNG.

      <name> and <format> may be omitted, though the use of <name> is heavily
      encouraged.

    -lzx, --lz-extract <file> <out>
      Extracts the contents of the RevLZ77-compressed file <file> to the file
      <out>.

    -lzp, --lz-pack <file> <out>
      Compresses the file <file> to the RevLZ77-compressed file <out>.

  Valid File Formats:
    .bam2
    .bcfnt
    .bch
    .bchenv
    .bcwav
    .bf
    .bmp
    .cbmd
    .ctpk
    .dat
    .epl
    .mbm
    .mgb
    .msb
    .ssb
    .ssnd
    .stex
    .tbl
    .tgd
    .tmx
    .ttd
    .ydd
    .ymd

  Valid STEX Image Formats:
    RGBA8888
    RGBA4444
    RGBA5551
    RGB888
    RGB565
    A8
    A4
    L8
    L4
    LA88
    LA44
    ETC1
    ETC1A4
