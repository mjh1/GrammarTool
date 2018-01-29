// NucleotideModelID.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)

package pal.substmodel;

import java.io.Serializable;

/**
 * interface for IDs of nucleotide models
 *
 * @version $Id: NucleotideModelID.java,v 1.2 2001/07/13 14:39:13 korbinian Exp $
 *
 * @author Korbinian Strimmer
 */
public interface NucleotideModelID extends Serializable
{
	//
	// Public stuff
	//

	int GTR = 0;
	int TN = 1;
	int HKY = 2;
	int F84 = 3;
	int F81 = 4;
	
	int MODELCOUNT = 5;

}
