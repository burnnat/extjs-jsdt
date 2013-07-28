package com.burnskids.jsdt.extjs.infer;

import org.eclipse.wst.jsdt.core.infer.IInferEngine;
import org.eclipse.wst.jsdt.core.infer.IInferenceFile;
import org.eclipse.wst.jsdt.core.infer.InferrenceProvider;
import org.eclipse.wst.jsdt.core.infer.RefactoringSupport;
import org.eclipse.wst.jsdt.core.infer.ResolutionConfiguration;

public class SenchaInferenceProvider implements InferrenceProvider {

	@Override
	public IInferEngine getInferEngine() {
		return new SenchaInferEngine(this);
	}

	@Override
	public int applysTo(IInferenceFile scriptFile) {
		return SenchaInferenceProvider.MAYBE_THIS;
	}

	@Override
	public String getID() {
		return this.getClass().getName();
	}

	@Override
	public ResolutionConfiguration getResolutionConfiguration() {
		return new ResolutionConfiguration();
	}

	@Override
	public RefactoringSupport getRefactoringSupport() {
		return new RefactoringSupport();
	}

}
