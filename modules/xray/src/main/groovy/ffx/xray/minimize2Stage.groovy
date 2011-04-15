// XRAY MINIMIZE

// Apache Imports
import org.apache.commons.io.FilenameUtils;

// Force Field X Imports
import ffx.xray.CrystalReciprocalSpace.SolventModel;
import ffx.xray.DiffractionData;
import ffx.xray.DiffractionFile;
import ffx.xray.RefinementMinimize;
import ffx.xray.RefinementMinimize.RefinementMode;

// Name of the file (PDB or XYZ).
String modelfilename = args[0];

// input MTZ/CIF/CNS data (optional - if not given, data must be present as pdbfilename.[mtz/cif/ent/cns]
String datafilename = args[1];

// data weight
double wA = 1.0;

// is this neutron data?
boolean neutron = false;

// Set the RMS gradient per atom convergence criteria (optional)
String coordepsString = args[2];
// default if epsString not given on the command line
double coordeps = 1.0;

// same, but for B factors
String bepsString = args[3];
double beps = 0.01;

// set the maximum number of refinement cycles
int maxiter = 50000;


// Things below this line normally do not need to be changed.
// ===============================================================================================

if (modelfilename == null) {
   modelfilename = "examples/1n7s.P212121.xyz";
}

if (coordepsString != null) {
   coordeps = Double.parseDouble(coordepsString);
}

if (bepsString != null) {
   beps = Double.parseDouble(bepsString);
}

println("\n Running x-ray minimize on " + modelfilename);
systems = open(modelfilename);

DiffractionFile diffractionfile = null;
if (datafilename != null) {
  diffractionfile = new DiffractionFile(datafilename, wA, neutron);
} else {
  diffractionfile = new DiffractionFile(systems, wA, neutron);
}

DiffractionData diffractiondata = new DiffractionData(systems, systems[0].getProperties(), SolventModel.POLYNOMIAL, diffractionfile);

diffractiondata.scalebulkfit();
diffractiondata.printstats();
energy();

RefinementMinimize refinementMinimize = new RefinementMinimize(diffractiondata, RefinementMode.COORDINATES);
refinementMinimize.minimize(coordeps, maxiter);
diffractiondata.scalebulkfit();
diffractiondata.printstats();

refinementMinimize = new RefinementMinimize(diffractiondata, RefinementMode.BFACTORS);
refinementMinimize.minimize(beps, maxiter);
diffractiondata.scalebulkfit();
diffractiondata.printstats();

energy();

diffractiondata.writedata(FilenameUtils.removeExtension(modelfilename) + "_refine.mtz");
saveAsPDB(systems, new File(FilenameUtils.removeExtension(modelfilename) + "_refine.pdb"));