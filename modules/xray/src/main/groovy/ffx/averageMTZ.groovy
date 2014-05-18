/**
 * Title: Force Field X.
 *
 * Description: Force Field X - Software for Molecular Biophysics.
 *
 * Copyright: Copyright (c) Michael J. Schnieders 2001-2012.
 *
 * This file is part of Force Field X.
 *
 * Force Field X is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * Force Field X is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Force Field X; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

// Groovy Imports
import groovy.util.CliBuilder;

import java.io.File;

import ffx.xray.MTZFilter;

// Things below this line normally do not need to be changed.
// ===============================================================================================

// Create the command line parser.
def cli = new CliBuilder(usage:' ffxc averageMTZ [options] <mtzfilename1> <mtzfilename2> <iteration>',
    header:'provide 2 MTZ files and iteration for cumulative moving average (use 1 for "normal" average of two files)');
cli.h(longOpt:'help', 'Print this help message.');
def options = cli.parse(args);
List<String> arguments = options.arguments();
if (options.h || arguments == null || arguments.size() != 3) {
    return cli.usage();
}

String mtzfile1 = arguments.get(0);
String mtzfile2 = arguments.get(1);

File file1 = new File(mtzfile1);
if (!file1.exists()){
    println("File: " + mtzfile1 + " not found!");
    return;
}

File file2 = new File(mtzfile2);
if (!file2.exists()){
    println("File: " + mtzfile2 + " not found!");
    return;
}

MTZFilter mtzfilter = new MTZFilter();
mtzfilter.averageFcs(file1, file2, mtzfilter.getReflectionList(file1), Integer.parseInt(arguments.get(2)), null);
