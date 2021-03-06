
/**
 * Title: Force Field X.
 *
 * Description: Force Field X - Software for Molecular Biophysics.
 *
 * Copyright: Copyright (c) Michael J. Schnieders 2001-2016.
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
 *
 * Linking this library statically or dynamically with other modules is making a
 * combined work based on this library. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules, and
 * to copy and distribute the resulting executable under terms of your choice,
 * provided that you also meet, for each linked independent module, the terms
 * and conditions of the license of that module. An independent module is a
 * module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but
 * you are not obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */

// TEST LAMBDA GRADIENT

// Groovy Imports
import groovy.transform.Field
import groovy.util.CliBuilder
import java.util.regex.Pattern

// FFX Imports
import ffx.numerics.Potential
import ffx.potential.DualTopologyEnergy
import ffx.potential.ForceFieldEnergy
import ffx.potential.QuadTopologyEnergy
import ffx.potential.bonded.Atom
import ffx.potential.bonded.LambdaInterface

// First ligand atom.
@Field int ligandStart = 1;

// Last ligand atom.
@Field int ligandStop = -1;

// First active atom.
@Field int activeStart = 1;

// Last active atom.
@Field int activeStop = -1;

// Field annotation gives the variables private script scope, thus allowing the 
// openFile method to see these.

// First ligand atom of the 2nd topology.
@Field int ligandStart2 = 1;

// Last ligand atom of the 2nd topology.
@Field int ligandStop2 = -1;

@Field def ranges1 = []; // Groovy mechanism for creating an untyped ArrayList.
@Field def ranges2 = [];
@Field def rangesA = [];
@Field def rangesB = [];

// First atom for no electrostatics.
@Field int noElecStart = 1;

// Last atom for no electrostatics.
@Field int noElecStop = -1;

// First atom of the 2nd topology for no electrostatics.
@Field int noElecStart2 = 1;

// Last atom of the 2nd topology for no electrostatics.
@Field int noElecStop2 = -1;

// Initial lambda value.
double initialLambda = 0.5;
double step = 1.0e-5;

// Print out the energy for each step.
boolean print = false;
boolean qi = false;

@Field List<String> filenames = new ArrayList<>();
@Field def topologies = [];
@Field def energies = [];

@Field int threadsAvail = edu.rit.pj.ParallelTeam.getDefaultThreadCount();
int numParallel = 1;
@Field int threadsPer = threadsAvail;

// Things below this line normally do not need to be changed.
// ===============================================================================================

// Create the command line parser.
def cli = new CliBuilder(usage:' ffxc testLambdaGradient [options] <XYZ|PDB> [Topology 2 XYZ|PDB] [Topology B1 XYZ|PDB] [Topology B2 XYZ|PDB]');
cli.h(longOpt:'help', 'Print this help message.');
cli.s(longOpt:'start', args:1, argName:'1', 'Starting ligand atom.');
cli.s2(longOpt:'start2', args:1, argName:'1', 'Starting ligand atom for the 2nd topology.');
cli.f(longOpt:'final', args:1, argName:'n', 'Final ligand atom.');
cli.f2(longOpt:'final2', args:1, argName:'n', 'Final ligand atom for the 2nd topology.');
cli.as(longOpt:'activeStart', args:1, argName:'1', 'Starting active atom.');
cli.af(longOpt:'activeFinal', args:1, argName:'n', 'Final active atom.');
cli.es(longOpt:'noElecStart', args:1, argName:'1', 'No Electrostatics Starting Atom.');
cli.es2(longOpt:'noElecStart2', args:1, argName:'1', 'No Electrostatics Starting Atom for the 2nd Topology.');
cli.ef(longOpt:'noElecFinal', args:1, argName:'-1', 'No Electrostatics Final Atom.');
cli.ef2(longOpt:'noElecFinal2', args:1, argName:'-1', 'No Electrostatics Final Atom for the 2nd topology.');
cli.l(longOpt:'lambda', args:1, argName:'0.5', 'Lambda value to test.');
cli.v(longOpt:'verbose', 'Print out the energy for each step.');
cli.qi(longOpt:'quasi-internal', 'Use quasi-internal multipole tensors.');
cli.la1(longOpt:'ligAtoms1', args:1, argName:'None', 'Period-separated ranges of 1st topology ligand atoms (e.g. 40-50.72-83)');
cli.la2(longOpt:'ligAtoms2', args:1, argName:'None', 'Period-separated ranges of 2nd topology ligand atoms (e.g. 40-50.72-83)');
cli.uaA(longOpt:'unsharedAtomsA', args:1, argName:'None', 'Quad-Topology: Period-separated ranges of A dual-topology atoms not shared by B; define from topology A1');
cli.uaB(longOpt:'unsharedAtomsB', args:1, argName:'None', 'Quad-Topology: Period-separated ranges of B dual-topology atoms not shared by A; define from topology B1');
cli.np(longOpt:'numParallel', args:1, argName:'1', 'Number of topology energies to calculate in parallel');
cli.d(longOpt:'dx', args:1, argName:'1.0e-5', 'Finite-difference step size (Angstroms)');

def options = cli.parse(args);
List<String> arguments = options.arguments();
if (options.h || arguments == null || arguments.size() < 1) {
    return cli.usage();
}

// Read in command line file.
//filenames[0] = arguments.get(0);
for (int i = 0; i < arguments.size(); i++) {
    filenames[i] = arguments.get(i);
}

// Starting ligand atom.
if (options.s) {
    ligandStart = Integer.parseInt(options.s);
}

// Final ligand atom.
if (options.f) {
    ligandStop = Integer.parseInt(options.f);
}

// Starting ligand atom.
if (options.s2) {
    ligandStart2 = Integer.parseInt(options.s2);
}

// Final ligand atom.
if (options.f2) {
    ligandStop2 = Integer.parseInt(options.f2);
}

// Starting ligand atom.
if (options.as) {
    activeStart = Integer.parseInt(options.as);
}

// Final ligand atom.
if (options.af) {
    activeStop = Integer.parseInt(options.af);
}

// No electrostatics on Topology 1.
if (options.es) {
    noElecStart = Integer.parseInt(options.es);
}

// First atom from Topology 1 with no electrostatics.
if (options.ef) {
    noElecStop = Integer.parseInt(options.ef);
}

// No electrostatics on Topology 2.
if (options.es2) {
    noElecStart2 = Integer.parseInt(options.es2);
}

// First atom from Topology 2 with no electrostatics.
if (options.ef2) {
    noElecStop2 = Integer.parseInt(options.ef2);
}

if (options.la1) {
    ranges1 = options.la1.tokenize(".");
}
if (options.la2) {
    ranges2 = options.la2.tokenize(".");
}

// Starting lambda value.
if (options.l) {
    initialLambda = Double.parseDouble(options.l);
}

// Load the finite-difference step size in Angstroms.
if (options.d) {
    step = Double.parseDouble(options.d);
}

// Print the energy for each step.
if (options.v) {
    print = true;
}

if (options.np) {
    numParallel = Integer.parseInt(options.np);
    if (threadsAvail % numParallel != 0) {
        logger.warning(String.format(" Number of threads available %d not evenly divisible by np %d; reverting to sequential", threadsAvail, numParallel));
        numParallel = 1;
    } else if (arguments.size() % numParallel != 0) {
        logger.warning(String.format(" Number of topologies %d not evenly divisible by np %d; reverting to sequential", arguments.size(), numParallel));
        numParallel = 1;
    } else {
        threadsPer = threadsAvail / numParallel;
    }
}

if (options.qi) {
    qi = true;
    System.setProperty("pme-qi","true");
    System.setProperty("no-ligand-condensed-scf","false");
    System.setProperty("ligand-vapor-elec","false");
    System.setProperty("polarization","NONE");
}
boolean debug = (System.getProperty("debug") != null);

/*int nThreads = ParallelTeam.getDefaultThreadCount() / 2;
System.setProperty("FF_THREADS", String.valueOf(nThreads));*/

/*if (arguments.size() == 1) {
    logger.info("\n Testing lambda derivatives for " + filenames[0]);
} else if (arguments.size() >= 2) {
    //filenames[1] = arguments.get(1);
    if (arguments.size == 4) {
        //logger.info("\n Testing lambda derivatives for [" + filenames[0] + "," + filenames[1] + "] dual topology\n");
        logger.info("\n Testing lambda derivatives for %s quad topology", filenames);
        
    } else {
        logger.info("\n Testing lambda derivatives for [" + filenames[0] + "," + filenames[1] + "] dual topology\n");
    }
    logger.info(" Initializing Topology 1\n");
}*/
int nargs = arguments.size();
if (nargs == 1) {
    logger.info("\n Testing lambda derivatives for " + filenames[0]);
} else if (nargs == 2) {
    logger.info("\n Testing lambda derivatives for [" + filenames[0] + "," + filenames[1] + "] dual topology\n");
} else if (nargs == 4) {
    //logger.info("\n Testing lambda derivatives for %s quad topology", filenames);
    StringBuilder sb = new StringBuilder("\n Testing lambda derivatives for [");
    for (int i = 0; i < 3; i++) {
        sb.append(filenames[i] + ",");
    }
    sb.append(filenames[3]).append("] quad topology");
    logger.info(sb.toString());
}
logger.info(" Initializing topologies...");

// Turn on computation of lambda derivatives
System.setProperty("lambdaterm","true");

// Relative free energies via the DualTopologyEnergy class require different
// default OSRW parameters than absolute free energies.
if (arguments.size() > 1) {
    // Condensed phase polarization is evaluated over the entire range.
    System.setProperty("polarization-lambda-start","0.0");
    // Polarization energy is not scaled individually by lambda, but
    // along with the overall potential energy of a topology.
    System.setProperty("polarization-lambda-exponent","0.0");
    // Ligand vapor electrostatics are not calculated. This cancels when the
    // difference between protein and water environments is considered.
    System.setProperty("ligand-vapor-elec","false");
    // Condensed phase polarization, without the ligand present, is unecessary.
    System.setProperty("no-ligand-condensed-scf","false");
}

@Field Pattern rangeregex = Pattern.compile("([0-9]+)-?([0-9]+)?");

void openFile(int topNum) {
    String filename = filenames.get(topNum);
    //String filename = 'xDx_a.pdb';
    open(filename, threadsPer);
    Atom[] atoms = active.getAtomArray();
    ForceFieldEnergy forceFieldEnergy = active.getPotentialEnergy();
    int n = atoms.length;
    remainder = (topNum % 2) + 1;
    if (remainder == 1) {
        for (int i = ligandStart; i <= ligandStop; i++) {
            Atom ai = atoms[i-1];
            ai.setApplyLambda(true);
            ai.print();
        }
        if (ranges1) {
            for (range in ranges1) {
                def m = rangeregex.matcher(range);
                if (m.find()) {
                    int rangeStart = Integer.parseInt(m.group(1));
                    int rangeEnd = (m.groupCount() > 1) ? Integer.parseInt(m.group(2)) : rangeStart;
                    if (rangeStart > rangeEnd) {
                        logger.severe(String.format(" Range %s was invalid; start was greater than end", range));
                    }
                    // Don't need to worry about negative numbers; rangeregex just won't match.
                    for (int i = rangeStart; i <= rangeEnd; i++) {
                        Atom ai = atoms[i-1];
                        ai.setApplyLambda(true);
                        ai.print();
                    }
                } else {
                    logger.warning(" Could not recognize ${range} as a valid range; skipping");
                }
            }
        }
        
        // Apply the no electrostatics atom selection
        if (noElecStart < 1) {
            noElecStart = 1;
        }
        if (noElecStop > atoms.length) {
            noElecStop = atoms.length;
        }
        for (int i = noElecStart; i <= noElecStop; i++) {
            Atom ai = atoms[i - 1];
            ai.setElectrostatics(false);
            ai.print();
        }
    } else if (remainder == 2) {
        for (int i = ligandStart2; i <= ligandStop2; i++) {
            Atom ai = atoms[i-1];
            ai.setApplyLambda(true);
            ai.print();
        }
        if (ranges2) {
            for (range in ranges2) {
                def m = rangeregex.matcher(range);
                if (m.find()) {
                    int rangeStart = Integer.parseInt(m.group(1));
                    int rangeEnd = (m.groupCount() > 1) ? Integer.parseInt(m.group(2)) : rangeStart;
                    if (rangeStart > rangeEnd) {
                        logger.severe(String.format(" Range %s was invalid; start was greater than end", range));
                    }
                    // Don't need to worry about negative numbers; rangeregex just won't match.
                    for (int i = rangeStart; i <= rangeEnd; i++) {
                        Atom ai = atoms[i-1];
                        ai.setApplyLambda(true);
                        ai.print();
                    }
                } else {
                    logger.warning(" Could not recognize ${range} as a valid range; skipping");
                }
            }
        }
        
        // Apply the no electrostatics atom selection
        if (noElecStart2 < 1) {
            noElecStart2 = 1;
        }
        if (noElecStop2 > atoms.length) {
            noElecStop2 = atoms.length;
        }
        for (int i = noElecStart2; i <= noElecStop2; i++) {
            Atom ai = atoms[i - 1];
            ai.setElectrostatics(false);
            ai.print();
        }
    } else {
        logger.severe(" This branch should never be reached; topology number ${topNum} % 2 + 1 should be 1 or 2, not ${remainder}");
    }
    forceFieldEnergy.getCrystal().setSpecialPositionCutoff(0.0);
    topologies[topNum] = active;
    energies[topNum] = forceFieldEnergy;
}

/*
// Open the first topology.
open(filename);

// Select ligand atoms
Atom[] atoms = active.getAtomArray();
int n = atoms.length;

// Apply ligand atom selection
for (int i = ligandStart; i <= ligandStop; i++) {
    Atom ai = atoms[i - 1];
    ai.setApplyLambda(true);
    ai.print();
}
*/
openFile(0);
Potential potential;
LambdaInterface lambdaInterface;
// Only support active atoms for single topology

/*
// Apply the no electrostatics atom selection
if (noElecStart < 1) {
    noElecStart = 1;
}
if (noElecStop > atoms.length) {
    noElecStop = atoms.length;
}
for (int i = noElecStart; i <= noElecStop; i++) {
    Atom ai = atoms[i - 1];
    ai.setElectrostatics(false);
    ai.print();
}
*/

/*potential = active.getPotentialEnergy();
ForceFieldEnergy forceFieldEnergy = active.getPotentialEnergy();
// Turn off checks for overlapping atoms, which is expected for lambda=0.
forceFieldEnergy.getCrystal().setSpecialPositionCutoff(0.0);

LambdaInterface lambdaInterface = active.getPotentialEnergy();*/

/*if (nThreads % 2 == 1) {
    // Second topology gets the leftover thread.
    nThreads++;
    System.setProperty("FF_THREADS", String.valueOf(nThreads));
}*/

// Hook this up to double-decoupling (single-state) and retest vdW.
// Watch that lambda gets injected at FFE rather than needing DT.
// Apply PME to the water dimer test case (ie. disappearing one molecule therefrom).

// Check for a 2nd topology.

if (arguments.size() == 1) {
    potential = energies[0];
    lambdaInterface = energies[0];
    // Apply active atom selection
    if (activeStop > activeStart && activeStart > 0 && activeStop <= n) {
        // Make all atoms inactive.
        for (int i = 0; i <= n; i++) {
            Atom ai = atoms[i - 1];
            ai.setActive(false);
        }
        // Make requested atoms active.
        for (int i = activeStart; i <= activeStop; i++) {
            Atom ai = atoms[i - 1];
            ai.setActive(true);
        }
    }
} else if (arguments.size() == 2) {
    openFile(1);
    /*topology1 = active;

    // Read in the 2nd command line file.
    filenames[0] = arguments.get(1);
    logger.info("\n Initializing Topology 2\n");
    open(filename);

    // Select ligand atoms
    atoms = active.getAtomArray();
    n = atoms.length;
    // Apply ligand atom selection
    for (int i = ligandStart2; i <= ligandStop2; i++) {
        Atom ai = atoms[i - 1];
        ai.setApplyLambda(true);
    }

    // Apply the no electrostatics atom selection
    if (noElecStart2 < 1) {
        noElecStart2 = 1;
    }
    if (noElecStop2 > atoms.length) {
        noElecStop2 = atoms.length;
    }
    for (int i = noElecStart2; i <= noElecStop2; i++) {
        Atom ai = atoms[i - 1];
        ai.setElectrostatics(false);
        ai.print();
    }

    forceFieldEnergy = active.getPotentialEnergy();
    // Turn off checks for overlapping atoms, which is expected for lambda=0.
    forceFieldEnergy.getCrystal().setSpecialPositionCutoff(0.0);*/

    DualTopologyEnergy dualTopologyEnergy = new DualTopologyEnergy(topologies[0], topologies[1]);
    if (numParallel == 2) {
        dualTopologyEnergy.setParallel(true);
    }
    potential = dualTopologyEnergy;
    lambdaInterface = dualTopologyEnergy;
} else if (arguments.size() == 4) {
    openFile(1);
    openFile(2);
    openFile(3);
    DualTopologyEnergy dtA = new DualTopologyEnergy(topologies[0], topologies[1]);
    DualTopologyEnergy dtB = new DualTopologyEnergy(topologies[3], topologies[2]);
    List<Integer> uniqueA = new ArrayList<>();
    List<Integer> uniqueB = new ArrayList<>();
    
    if (options.uaA) {
        rangesA = options.uaA.tokenize(".");
        def ra = [] as Set;
        for (range in rangesA) {
            def m = rangeregex.matcher(range);
            if (m.find()) {
                int rangeStart = Integer.parseInt(m.group(1));
                int rangeEnd = (m.groupCount() > 1) ? Integer.parseInt(m.group(2)) : rangeStart;
                if (rangeStart > rangeEnd) {
                    logger.severe(String.format(" Range %s was invalid; start was greater than end", range));
                }
                for (int i = rangeStart; i <= rangeEnd; i++) {
                    ra.add(i-1);
                }
            }
        }
        Atom[] atA1 = topologies[0].getAtomArray();
        int counter = 0;
        def raAdj = [] as Set; // Indexed by common variables in dtA.
        for (int i = 0; i < atA1.length; i++) {
            Atom ai = atA1[i];
            if (i in ra) {
                if (ai.applyLambda()) {
                    logger.warning(String.format(" Ranges defined in uaA should not overlap with ligand atoms; they are assumed to not be shared."));
                } else {
                    logger.info(String.format(" Unshared A: %d variables %d-%d", i, counter, counter+2));
                    for (int j = 0; j < 3; j++) {
                        raAdj.add(new Integer(counter + j));
                    }
                }
            }
            if (! ai.applyLambda()) {
                counter += 3;
            }
        }
        if (raAdj) {
            uniqueA.addAll(raAdj);
        }
    }
    if (options.uaB) {
        rangesB = options.uaB.tokenize(".");
        def rb = [] as Set;
        for (range in rangesB) {
            def m = rangeregex.matcher(range);
            if (m.find()) {
                int rangeStart = Integer.parseInt(m.group(1));
                int rangeEnd = (m.groupCount() > 1) ? Integer.parseInt(m.group(2)) : rangeStart;
                if (rangeStart > rangeEnd) {
                    logger.severe(String.format(" Range %s was invalid; start was greater than end", range));
                }
                for (int i = rangeStart; i <= rangeEnd; i++) {
                    rb.add(i-1);
                }
            }
        }
        Atom[] atB1 = topologies[2].getAtomArray();
        int counter = 0;
        def rbAdj = [] as Set; // Indexed by common variables in dtA.
        for (int i = 0; i < atB1.length; i++) {
            Atom bi = atB1[i];
            if (i in rb) {
                if (bi.applyLambda()) {
                    logger.warning(String.format(" Ranges defined in uaA should not overlap with ligand atoms; they are assumed to not be shared."));
                } else {
                    logger.info(String.format(" Unshared B: %d variables %d-%d", i, counter, counter+2));
                    for (int j = 0; j < 3; j++) {
                        rbAdj.add(counter + j);
                    }
                }
            }
            if (! bi.applyLambda()) {
                counter += 3;
            }
        }
        if (rbAdj) {
            uniqueB.addAll(rbAdj);
        }
    }
    
    QuadTopologyEnergy qte = new QuadTopologyEnergy(dtA, dtB, uniqueA, uniqueB);
    if (numParallel >= 2) {
        qte.setParallel(true);
        if (numParallel == 4) {
            dtA.setParallel(true);
            dtB.setParallel(true);
        }
    }
    potential = qte;
    lambdaInterface = qte;
} else {
    logger.severe(" Must have 1, 2, or 4 topologies to test.");
}

// Reset the number of variables for the case of dual topology.
n = potential.getNumberOfVariables();
double[] x = new double[n];
double[] gradient = new double[n];
double[] lambdaGrad = new double[n];
double[][] lambdaGradFD = new double[2][n];

// Number of independent atoms.
assert(n % 3 == 0);
int nAtoms = n / 3;

// Compute the Lambda = 0.0 energy.
lambda = 0.0;
lambdaInterface.setLambda(lambda);
potential.getCoordinates(x);
double e0 = potential.energyAndGradient(x,gradient);

// Compute the Lambda = 1.0 energy.
double lambda = 1.0;
lambdaInterface.setLambda(lambda);
double e1 = potential.energyAndGradient(x,gradient);

logger.info(String.format(" E(0):      %20.8f.", e0));
logger.info(String.format(" E(1):      %20.8f.", e1));
logger.info(String.format(" E(1)-E(0): %20.8f.\n", e1-e0));

// Finite-difference step size.
double width = 2.0 * step;

// Error tolerence
double errTol = 1.0e-3;
// Upper bound for typical gradient sizes (expected gradient)
double expGrad = 1000.0;

// Test Lambda gradient in the neighborhood of the lambda variable.
for (int j=0; j<3; j++) {
    lambda = initialLambda - 0.01 + 0.01 * j;

    if (lambda - step < 0.0) {
        continue;
    }
    if (lambda + step > 1.0) {
        continue;
    }

    logger.info(String.format(" Current lambda value %6.4f", lambda));
    lambdaInterface.setLambda(lambda);

    // Calculate the energy, dE/dX, dE/dL, d2E/dL2 and dE/dL/dX
    double e = potential.energyAndGradient(x,gradient);

    // Analytic dEdL, d2E/dL2 and dE/dL/dX
    double dEdL = lambdaInterface.getdEdL();
    double d2EdL2 = lambdaInterface.getd2EdL2();
    for (int i = 0; i < n; i++) {
        lambdaGrad[i] = 0.0;
    }
    potential.getdEdXdL(lambdaGrad);

    // Calculate the finite-difference dEdLambda, d2EdLambda2 and dEdLambdadX
    lambdaInterface.setLambda(lambda + step);
    double lp = potential.energyAndGradient(x,lambdaGradFD[0]);
    double dedlp = lambdaInterface.getdEdL();
    lambdaInterface.setLambda(lambda - step);
    double lm = potential.energyAndGradient(x,lambdaGradFD[1]);
    double dedlm = lambdaInterface.getdEdL();

    double dEdLFD = (lp - lm) / width;
    double d2EdL2FD = (dedlp - dedlm) / width;

    if (debug) {
        logger.info(String.format(" db> Lambda FD Test  lower,center,upper: %g %g %g", lambda - step , lambda, lambda + step));
        logger.info(String.format(" db> dE/dL   Numeric lp,lm,width,val: %+9.6g %+9.6g %4.2g (%+9.6g)", lp, lm, width, dEdLFD));
        logger.info(String.format(" db> d2E/dL2 Numeric lp,lm,width,val: %+9.6g %+9.6g %4.2g [%+9.6g]", dedlp, dedlm, width, d2EdL2FD));
        logger.info(String.format(" db> Analytic vers   l,dEdL,d2EdL2: %4.2f (%+9.6g) [%+9.6g]", lambda, dEdL, d2EdL2));
    }
        
    double err = Math.abs(dEdLFD - dEdL);
    if (err < errTol) {
        logger.info(String.format(" dE/dL passed:   %10.6f", err));
    } else {
        logger.info(String.format(" dE/dL failed: %10.6f", err));
    }
    logger.info(String.format(" Numeric:   %15.8f", dEdLFD));
    logger.info(String.format(" Analytic:  %15.8f", dEdL));

    err = Math.abs(d2EdL2FD - d2EdL2);
    if (err < errTol) {
        logger.info(String.format(" d2E/dL2 passed: %10.6f", err));
    } else {
        logger.info(String.format(" d2E/dL2 failed: %10.6f", err));
    }
    logger.info(String.format(" Numeric:   %15.8f", d2EdL2FD));
    logger.info(String.format(" Analytic:  %15.8f", d2EdL2));

    boolean passed = true;

    for (int i = 0; i < nAtoms; i++) {
        int ii = i * 3;
        double dX = (lambdaGradFD[0][ii] - lambdaGradFD[1][ii]) / width;
        double dXa = lambdaGrad[ii];
        double eX = dX - dXa;
        ii++;
        double dY = (lambdaGradFD[0][ii] - lambdaGradFD[1][ii]) / width;
        double dYa = lambdaGrad[ii];
        double eY = dY - dYa;
        ii++;
        double dZ = (lambdaGradFD[0][ii] - lambdaGradFD[1][ii]) / width;
        double dZa = lambdaGrad[ii];
        double eZ = dZ - dZa;

        double error = Math.sqrt(eX * eX + eY * eY + eZ * eZ);
        if (error < errTol) {
            logger.fine(String.format(" dE/dX/dL for Atom %d passed: %10.6f", i + 1, error));
        } else {
            logger.info(String.format(" dE/dX/dL for Atom %d failed: %10.6f", i + 1, error));
            logger.info(String.format(" Analytic: (%15.8f, %15.8f, %15.8f)", dXa,dYa,dZa));
            logger.info(String.format(" Numeric:  (%15.8f, %15.8f, %15.8f)", dX, dY, dZ));
            passed = false;
        }
    }
    if (passed) {
        logger.info(String.format(" dE/dX/dL passed for all atoms"));
    }

    logger.info("");
}

lambdaInterface.setLambda(initialLambda);
potential.getCoordinates(x);
potential.energyAndGradient(x,gradient, print);

logger.info(String.format(" Checking Cartesian coordinate gradient"));

double[] numeric = new double[3];
double avLen = 0.0;
int nFailures = 0;
double avGrad = 0.0;
for (int i=0; i<nAtoms; i++) {
    int i3 = i*3;
    int i0 = i3 + 0;
    int i1 = i3 + 1;
    int i2 = i3 + 2;

    // Find numeric dX
    double orig = x[i0];
    x[i0] = x[i0] + step;
    double e = potential.energyAndGradient(x,lambdaGradFD[0], print);
    x[i0] = orig - step;
    e -= potential.energyAndGradient(x,lambdaGradFD[1], print);
    x[i0] = orig;
    numeric[0] = e / width;

    // Find numeric dY
    orig = x[i1];
    x[i1] = x[i1] + step;
    e = potential.energyAndGradient(x,lambdaGradFD[0], print);
    x[i1] = orig - step;
    e -= potential.energyAndGradient(x,lambdaGradFD[1], print);
    x[i1] = orig;
    numeric[1] = e / width;

    // Find numeric dZ
    orig = x[i2];
    x[i2] = x[i2] + step;
    e = potential.energyAndGradient(x,lambdaGradFD[0], print);
    x[i2] = orig - step;
    e -= potential.energyAndGradient(x,lambdaGradFD[1], print);
    x[i2] = orig;
    numeric[2] = e / width;

    double dx = gradient[i0] - numeric[0];
    double dy = gradient[i1] - numeric[1];
    double dz = gradient[i2] - numeric[2];
    double len = dx * dx + dy * dy + dz * dz;
    avLen += len;
    len = Math.sqrt(len);

    double grad2 = gradient[i0] * gradient[i0] + gradient[i1] * gradient[i1] + gradient[i2] * gradient[i2];
    avGrad += grad2;

    if (len > errTol) {
        logger.info(String.format(" Atom %d failed: %10.6f.",i+1,len)
            + String.format("\n Analytic: (%12.4f, %12.4f, %12.4f)\n", gradient[i0], gradient[i1], gradient[i2])
            + String.format(" Numeric:  (%12.4f, %12.4f, %12.4f)\n", numeric[0], numeric[1], numeric[2]));
        ++nFailures;
        //return;
    } else {
        logger.info(String.format(" Atom %d passed: %10.6f.",i+1,len)
            + String.format("\n Analytic: (%12.4f, %12.4f, %12.4f)\n", gradient[i0], gradient[i1], gradient[i2])
            + String.format(" Numeric:  (%12.4f, %12.4f, %12.4f)", numeric[0], numeric[1], numeric[2]));
    }

    if (grad2 > expGrad) {
        logger.info(String.format(" Atom %d has an unusually large gradient: %10.6f", i+1, grad2));
    }
    logger.info("\n");
}

avLen = avLen / nAtoms;
avLen = Math.sqrt(avLen);
if (avLen > errTol) {
    logger.info(String.format(" Test failure: RMSD from analytic solution is %10.6f > %10.6f", avLen, errTol));
} else {
    logger.info(String.format(" Test success: RMSD from analytic solution is %10.6f < %10.6f", avLen, errTol));
}
logger.info(String.format(" Number of atoms failing gradient test: %d", nFailures));

avGrad = avGrad / nAtoms;
avGrad = Math.sqrt(avGrad);
if (avGrad > expGrad) {
    logger.info(String.format(" Unusually large RMS gradient: %10.6f > %10.6f", avGrad, expGrad));
} else {
    logger.info(String.format(" RMS gradient: %10.6f", avGrad));
}
