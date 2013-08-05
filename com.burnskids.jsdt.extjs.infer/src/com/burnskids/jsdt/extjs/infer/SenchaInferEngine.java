package com.burnskids.jsdt.extjs.infer;

import org.eclipse.wst.jsdt.core.ast.ASTVisitor;
import org.eclipse.wst.jsdt.core.infer.IInferEngine;
import org.eclipse.wst.jsdt.core.infer.InferOptions;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;

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
		ASTVisitor inferrer = new ClassDefinitionInferrer(this);
		compUnit.traverse(inferrer);
		
		inferrer = new ClassInheritanceInferrer(this);
		compUnit.traverse(inferrer);
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
}
