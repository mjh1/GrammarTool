// ModelParameters.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)


package pal.eval;

import pal.alignment.*;
import pal.distance.*;
import pal.math.*;
import pal.substmodel.*;
import pal.tree.*;

/**
 * estimates substitution model parameters from the data
 *
 * @version $Id: ModelParameters.java,v 1.9 2002/12/05 04:27:28 matt Exp $
 * @author Korbinian Strimmer
 */
public class ModelParameters implements MultivariateFunction
{
	//
	// public stuff
	//

	/** fractional digits desired for parameters */
	public final static int FRACDIGITS = 3;

	/**
	 * Constructor
	 *
	 * @param sp site pattern
	 * @param m substitution model
	 */
	public ModelParameters(SitePattern sp, SubstitutionModel m)
	{
		sitePattern = sp;
		model = m;
		numParams = model.getNumParameters();

		lv = new LikelihoodValue(sitePattern);
		lv.setModel(model);

		if (numParams == 1)
		{
			mvm = new OrthogonalSearch();
		}
		else
		{
			mvm = new ConjugateGradientSearch();
			//((ConjugateGradientSearch) mvm).prin = 2;
		}
	}

	/**
	 * estimate (approximate) values for the model parameters
	 * from the data using a neighbor-joining tree
	 *
	 * @return parameter estimates
	 */
	public double[] estimate()
	{
		double[] p = new double[numParams];
		for (int i = 0; i < numParams; i++)
		{
			p[i] = model.getDefaultValue(i);
		}
		double fp;

		AlignmentDistanceMatrix distMat = null;
		double tolfp = Math.pow(10, -1-FRACDIGITS);
		double tolp = Math.pow(10, -1-FRACDIGITS);
		boolean first = true;
		do
		{
			if (first)
			{
				distMat = new AlignmentDistanceMatrix(sitePattern, model);;
			}
			else
			{
				distMat.recompute(sitePattern, model);
			}

			NeighborJoiningTree t = new NeighborJoiningTree(distMat);
			ParameterizedTree pt = new UnconstrainedTree(t);

			//ChiSquareValue cs = new ChiSquareValue(distMat, true);
			//cs.setTree(pt);
			//cs.optimiseParameters();

			lv.setTree(pt);

			if (first)
			{
				fp = evaluate(p);
				mvm.stopCondition(fp, p, tolfp, tolp, true);
				first = false;
			}

			mvm.optimize(this, p, tolfp, tolp);

			fp = evaluate(p);
		}
		while (!mvm.stopCondition(fp, p, tolfp, tolp, false));

		// trim p
		double m = Math.pow(10, FRACDIGITS);
		for (int i = 0;  i < p.length; i++)
		{
			p[i] = Math.round(p[i]*m)/m;
		}

		fp = evaluate(p);

		// Corresponding SEs
		double[] pSE = new double[numParams];
		pSE = NumericalDerivative.diagonalHessian(this, p);
		for (int i = 0; i < numParams; i++)
		{
			pSE[i] = Math.sqrt(1.0/pSE[i]);
			model.setParameterSE(pSE[i], i);
		}
		return p;
	}

	/**
	 * estimate (approximate) values for the model parameters
	 * from the data using a given (parameterized) tree
	 * @return parameter estimates
	 */
	public double[] estimateFromTree(ParameterizedTree t)
	{
		// there is a horrible amount of code duplication
		// here - should be cleaned up at some time


		double[] p = new double[numParams];
		for (int i = 0; i < numParams; i++)
		{
			p[i] = model.getDefaultValue(i);
		}
		double fp;

		double tolfp = Math.pow(10, -1-FRACDIGITS);
		double tolp = Math.pow(10, -1-FRACDIGITS);
		boolean first = true;
		do
		{
			lv.setTree(t);

			if (first)
			{
				fp = evaluate(p);
				mvm.stopCondition(fp, p, tolfp, tolp, true);
				first = false;
			}

			mvm.optimize(this, p, tolfp, tolp);

			fp = evaluate(p);
		}
		while (!mvm.stopCondition(fp, p, tolfp, tolp, false));

		// trim p
		double m = Math.pow(10, FRACDIGITS);
		for (int i = 0;  i < p.length; i++)
		{
			p[i] = Math.round(p[i]*m)/m;
		}

		fp = evaluate(p);

		// Corresponding SEs
		double[] pSE = new double[numParams];
		pSE = NumericalDerivative.diagonalHessian(this, p);
		for (int i = 0; i < numParams; i++)
		{
			pSE[i] = Math.sqrt(1.0/pSE[i]);
			model.setParameterSE(pSE[i], i);
		}
		return p;
	}



	// interface MultivariateFunction

	public double evaluate(double[] params)
	{
		// set model parameters
		for (int i = 0; i < numParams; i++)
		{
			model.setParameter(params[i], i);
		}

		double r = -lv.compute();
		//double r = -lv.optimiseParameters();

		return r;
	}

	public int getNumArguments()
	{
		return numParams;
	}

	public double getLowerBound(int n)
	{
		return model.getLowerLimit(n);
	}

	public double getUpperBound(int n)
	{
		return model.getUpperLimit(n);
	}

	/**
	 * @return null
	 */
	public OrthogonalHints getOrthogonalHints() { return null; }

	//
	// Private stuff
	//

	private int numParams;
	private SubstitutionModel model;
	private SitePattern sitePattern;
	private ParameterizedTree tree;
	private LikelihoodValue lv;
	private MultivariateMinimum mvm;
}
