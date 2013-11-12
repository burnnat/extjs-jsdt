package com.burnskids.jsdt.extjs.infer;

import org.eclipse.wst.jsdt.core.ast.ASTVisitor;
import org.eclipse.wst.jsdt.core.infer.IInferEngine;
import org.eclipse.wst.jsdt.core.infer.InferOptions;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.core.search.indexing.IIndexConstants;

public class SenchaInferEngine implements IInferEngine {

	private final SenchaInferenceProvider provider;
	private CompilationUnitDeclaration compUnit;

	public SenchaInferEngine(SenchaInferenceProvider provider) {
		this.provider = provider;
	}

	@Override
	public void initialize() {
		
	}

	@Override
	public void initializeOptions(InferOptions inferOptions) {
		
	}

	@Override
	public void setCompilationUnit(CompilationUnitDeclaration parsedUnit) {
		this.compUnit = parsedUnit;
	}

	@Override
	public void doInfer() {
		ASTVisitor[] inferrers = new ASTVisitor[] {
			new ClassDefinitionInferrer(this),
			new ClassInheritanceInferrer(this),
			new ObjectCreationInferrer(this)
		};
		
		for (ASTVisitor inferrer : inferrers) {
			compUnit.traverse(inferrer);
		}
	}
	
	protected InferredType addType(char[] name) {
		return compUnit.addType(name, true, provider.getID());
	}
	
	protected InferredType getType(char[] name) {
		return compUnit.findInferredType(name);
	}
	
	protected InferredType createTypeIfNeeded(char[] name) {
		InferredType type = this.getType(name);
		
		if (type == null) {
			type = compUnit.addType(name, false, provider.getID());
		}
		
		return type;
	}
	
	protected InferredType getGlobalType() {
		InferredType global = createTypeIfNeeded(IIndexConstants.GLOBAL_SYMBOL);
		
		if (!global.isGlobal()) {
			global.isAnonymous = true;
			global.setIsDefinition(true);
			global.setIsGlobal(true);
		}
		
		return global;
	}
}
