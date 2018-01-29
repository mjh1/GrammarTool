// SequenceSimulator.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)

package pal.substmodel;

/**
 * <p>Title: Sequence Simulator</p>
 * <p>Description: A basic tool for simulating sequences via a substitution model </p>
 * @author Matthew Goode
 * @version 1.0
 */
import pal.math.MersenneTwisterFast;
public class SequenceSimulator {
	private final SubstitutionModel model_;
	private final int[] siteCategories_;
	private final int sequenceLength_;
	private final int numberOfStates_;
	private final int numberOfCategories_;

	private final MersenneTwisterFast random_;
	private final double[][][] transitionProbabilityStore_;

	/**
	 * A constructor (with no provided random number generator - a fresh one is created)
	 * @param model The substitution model used for simulation
	 * @param sequenceLength The length of all sequences generated by this simulator
	 */
  public SequenceSimulator(SubstitutionModel model, int sequenceLength, boolean stochasticDistribution) {
	  this(model,sequenceLength, new MersenneTwisterFast(),stochasticDistribution);
  }
	/**
	 * A constructor (with no provided random number generator - a fresh one is created)
	 * @param model The substitution model used for simulation
	 * @param sequenceLength The length of all sequences generated by this simulator
	 * @param random A random number generator
	 */
  public SequenceSimulator(SubstitutionModel model, int sequenceLength, MersenneTwisterFast random, boolean stochasticDistribution) {
		this.model_ = model;
		this.sequenceLength_ = sequenceLength;
		this.siteCategories_ = new int[sequenceLength];
		this.random_ = random;
	  this.transitionProbabilityStore_ = SubstitutionModel.Utils.generateTransitionProbabilityTables(model);
		this.numberOfStates_ = model_.getDataType().getNumStates();
		this.numberOfCategories_ = model_.getNumberOfTransitionCategories();

		resetSiteCategoryDistribution(stochasticDistribution);
  }

	/**
	 * Reassigns model classes to each site (a site belongs to a particular class/category of the model)
	 */
	public void resetSiteCategoryDistribution(boolean stochasticDistribution) {
	  resetSiteCategoryDistribution(model_.getTransitionCategoryProbabilities(),stochasticDistribution);
	}
	public void resetSiteCategoryDistribution(final double[] categoryDistribution, boolean stochasticDistribution) {
		if(stochasticDistribution) {
			for( int i = 0; i<sequenceLength_; i++ ) {
				siteCategories_[i] = cumulativeSelect( categoryDistribution, numberOfCategories_ );
			}
		} else {
		  int total = 0;
			int index = 0;
			for(int category = 0 ; category < numberOfCategories_ ; category++) {
			  int count  = (int)(sequenceLength_*categoryDistribution[category]);
				for( int i = 0; i<count; i++ ) {
				  siteCategories_[index++] = category;
			  }
				total+=count;
			}
			//Pad out the remainder due to rounding errors
			for( int i = total; i<sequenceLength_; i++ ) {
				siteCategories_[index++] = numberOfCategories_-1;
			}



		}
	}
	public void resetSiteCategoryDistribution(double[][] posteriorCategoryDistribution) {
		resetSiteCategoryDistribution(posteriorCategoryDistribution,siteCategories_);
	}
	public int[] getSiteCategoryDistribution() { return siteCategories_; }
	public void resetSiteCategoryDistribution(double[][] posteriorCategoryDistribution, SequenceSimulator base) {
		if(base.sequenceLength_==sequenceLength_) {
		  throw new IllegalArgumentException("Base simulator has incompatible sequence length:"+base.sequenceLength_+" found, "+sequenceLength_+" expected");
		}
		resetSiteCategoryDistribution(posteriorCategoryDistribution,base.siteCategories_);
	}
	public void resetSiteCategoryDistribution(double[][] posteriorCategoryDistribution, int[] baseSiteCategories) {
	  for(int i = 0 ; i < sequenceLength_ ; i++) {
	  	final int oldCategory = baseSiteCategories[i];
		  siteCategories_[i] = cumulativeSelect(posteriorCategoryDistribution[oldCategory],numberOfCategories_);
		}
	}
	public void simulate(int[] startingSequence, double distance, int[] endingSequenceStore) {
		model_.getTransitionProbabilities(distance,transitionProbabilityStore_);
		for(int i = 0 ; i < sequenceLength_ ; i++) {
		  final int startState = startingSequence[i];
			final int category = siteCategories_[i];
			final double[] distribution = transitionProbabilityStore_[category][startState];
		  endingSequenceStore[i] = cumulativeSelect(distribution,numberOfStates_);
		}
	}
	public int[] getSimulated(int[] startingSequence, double distance) {
	  final int[] endingSequenceStore = new int[sequenceLength_];
		simulate(startingSequence,distance,endingSequenceStore);
		return endingSequenceStore;
	}
	public int[] generateRoot() {
	  final int[] root = new int[sequenceLength_];
		final double[] equilibriumDistribution = model_.getEquilibriumFrequencies();
		for(int i = 0 ; i < sequenceLength_ ; i++) {
		  root[i] = cumulativeSelect(equilibriumDistribution,numberOfStates_);
		}
		return root;
	}
// =--=-= Internal Utils -==-=--=

	//Selects an index according to a distribution (that should sum to one), by cumulative method
	private final int cumulativeSelect(double[] distribution, int numberInDistribution ) {
	  double r = random_.nextDouble();
		double total = 0;
		for(int i = 0 ; i < numberInDistribution ; i++) {
		  total+=distribution[i];
			if(r<total) {
				return i;
			}
		}
		//Might want to add warning stuff as this shouldn't happen if distribution sums to one...
		return numberInDistribution-1;
	}

}