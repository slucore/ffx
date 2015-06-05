/**
 * Title: Force Field X.
 *
 * Description: Force Field X - Software for Molecular Biophysics.
 *
 * Copyright: Copyright (c) Michael J. Schnieders 2001-2015.
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
package ffx.potential.bonded;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * The ResidueState class encodes the current chemical and coordinate state of a 
 * Residue, particularly a MultiResidue, for ease of reverting coordinates. Should
 * not be too difficult to get it to also store velocities, etc.
 *
 * @author Michael J. Schnieders
 * @author Jacob M. Litman
 * @since 1.0
 *
 */
public class ResidueState {
    private static final Logger logger = Logger.getLogger(ResidueState.class.getName());
    private final Residue parent;
    private final Residue res;
    private final HashMap<Atom, double[]> atomMap;
    private final Atom[] atoms;
    
    public ResidueState(Residue parent, Residue res) {
        this.parent = parent;
        this.res = res;
        
        List<Atom> atomList = res.getAtomList();
        int nAtoms = atomList.size();
        atomMap = new HashMap<>(nAtoms);
        atoms = new Atom[nAtoms];
        
        atomList.toArray(atoms);
        for (Atom atom : atoms) {
            double[] atXYZ = new double[3];
            atom.getXYZ(atXYZ);
            atomMap.put(atom, atXYZ);
        }
    }
    
    public Residue getParent() {
        return parent;
    }
    
    public Residue getResidue() {
        return res;
    }
    
    public Atom[] getAtoms() {
        return atoms;
    }
    
    /**
     * If parameter true, returns atomic coordinates indexed by the atom list as
     * this ResidueState was constructed; else, uses the residue's current atom
     * list. Use with care.
     * @param useOriginalOrder Use the original atom ordering.
     * @return An array of coordinate arrays.
     */
    public double[][] getAllAtomCoords(boolean useOriginalOrder) {
        int nAtoms = atoms.length;
        double[][] xyz = new double[nAtoms][3];
        
        if (useOriginalOrder) {
            for (int i = 0; i < nAtoms; i++) {
                double[] atXYZ = atomMap.get(atoms[i]);
                System.arraycopy(atXYZ, 0, xyz[i], 0, 3);
            }
        } else {
            List<Atom> atomList = res.getAtomList();
            int i = 0;
            for (Atom atom : atomList) {
                double[] atXYZ = atomMap.get(atom);
                System.arraycopy(atXYZ, 0, xyz[i++], 0, 3);
            }
        }
        
        return xyz;
    }
    
    /**
     * Returns the coordinates of the selected atom; or null if atom not contained.
     * @param atom An Atom.
     * @return xyz coordinates.
     */
    public double[] getAtomCoords(Atom atom) {
        double[] xyz = new double[3];
        System.arraycopy(atomMap.get(atom), 0, xyz, 0, 3);
        return xyz;
    }
    
    /**
     * Resets stored coordinates to current coordinates of residue.
     */
    public void resetAtomicCoordinates() {
        for (Atom atom : atoms) {
            atom.getXYZ(atomMap.get(atom));
        }
    }
    
    // A set of public static methods for storing/reverting coordinates for lists of residues.
    
    public static ResidueState[] storeAllCoordinates(List<Residue> residueList) {
        return storeAllCoordinates(residueList.toArray(new Residue[residueList.size()]));
    }
    
    public static ResidueState[] storeAllCoordinates(Residue[] residues) {
        int nResidues = residues.length;
        ResidueState states[] = new ResidueState[nResidues];
        for (int i = 0; i < nResidues; i++) {
            states[i] = residues[i].storeCoordinates();
        }
        return states;
    }
    
    public static double[][][] storeAllCoordinateArrays(Residue[] residueArray) {
        int nResidues = residueArray.length;
        double[][][] xyz = new double[nResidues][][];
        for (int i = 0; i < nResidues; i++) {
            xyz[i] = residueArray[i].storeCoordinateArray();
        }
        return xyz;
    }
    
    public static double[][][] storeAllCoordinateArrays(List<Residue> residueList) {
        return storeAllCoordinateArrays(residueList.toArray(new Residue[residueList.size()]));
    }
    
    public static void revertAllCoordinates(List<Residue> residueList, ResidueState[] states) {
        revertAllCoordinates(residueList.toArray(new Residue[residueList.size()]), states);
    }
    
    public static void revertAllCoordinates(Residue[] residueArray, ResidueState[] states) {
        int nResidues = residueArray.length;
        if (nResidues != states.length) {
            throw new IllegalArgumentException(String.format("Length of residue "
                    + "array %d and residue state array %d do not match.", nResidues, states.length));
        }
        
        for (int i = 0; i < nResidues; i++) {
            Residue resi = residueArray[i];
            // If indexing did not get thrown off:
            if (resi.equals(states[i].getParent())) {
                resi.revertCoordinates(states[i]);
            } else {
                // Else must search states[]
                boolean matchFound = false;
                for (int j = 0; j < nResidues; j++) {
                    if (resi.equals(states[j].getParent())) {
                        matchFound = true;
                        resi.revertCoordinates(states[j]);
                        break;
                    }
                }
                
                if (!matchFound) {
                    throw new IllegalArgumentException(String.format("Could not "
                            + "find match for residue %s among residue states array.", resi.toString()));
                }
            }
        }
    }
}