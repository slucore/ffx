import java.io.File;
import ffx.xray.MTZFilter;

String mtzfile = args[0];
if (mtzfile == null) {
  println(" Usage: ffxc mtzInfo filename");
  return;
}

File file = new File(mtzfile);
if (!file.exists()){
  println("File: " + mtzfile + " not found!");
  return;
}

MTZFilter mtzfilter = new MTZFilter();
mtzfilter.getReflectionList(file);
mtzfilter.print_header();