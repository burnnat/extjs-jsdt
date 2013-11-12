package com.burnskids.jsdt.extjs.infer;

import org.eclipse.wst.jsdt.core.ast.IAssignment;
import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.core.ast.IFunctionCall;
import org.eclipse.wst.jsdt.core.ast.ILocalDeclaration;
import org.eclipse.wst.jsdt.core.ast.IObjectLiteral;
import org.eclipse.wst.jsdt.core.ast.IStringLiteral;
import org.eclipse.wst.jsdt.core.infer.InferredType;

public class ObjectCreationInferrer extends AbstractExtInferrer {
	
	private static final char[] CREATE = new char[] { 'c', 'r', 'e', 'a', 't', 'e' };
	private static final char[] XCLASS = new char[] { 'x', 'c', 'l', 'a', 's', 's' };
	
	public ObjectCreationInferrer(SenchaInferEngine parent) {
		super(parent);
	}
	
	@Override
	public boolean visit(IFunctionCall functionCall) {
		// To handle anonymous Ext.create() calls, we need to be able
		// to apply an inferred type to the function call directly.
		// This would require some alteration to the resolveType()
		// method of the MessageSend class.
		
//		if (functionCall instanceof MessageSend) {
//			InferredType type = inferType(functionCall);
//			
//			if (type != null) {
//				((MessageSend) functionCall).setInferredType(type);
//			}
//		}
		
		return super.visit(functionCall);
	}
	
	@Override
	public boolean visit(ILocalDeclaration declaration) {
		InferredType type = inferType(declaration.getInitialization());
		
		if (type != null) {
			declaration.setInferredType(type);
		}
		
		return super.visit(declaration);
	}
	
	@Override
	public boolean visit(IAssignment assignment) {
		InferredType type = inferType(assignment.getExpression());
		
		if (type != null) {
			assignment.setInferredType(type);
		}
		
		return super.visit(assignment);
	}
	
	private InferredType inferType(IExpression value) {
		if (value.getASTType() == IExpression.FUNCTION_CALL) {
			IFunctionCall functionCall = (IFunctionCall) value;
			
			if (isExtMethod(functionCall, CREATE)) {
				IStringLiteral name = parseCreatedName(functionCall.getArguments());
				
				if (name != null) {
					return parent.createTypeIfNeeded(name.source());
				}
			}
		}
		
		return null;
	}
	
	private IStringLiteral parseCreatedName(IExpression[] arguments) {
		if (arguments.length < 1) {
			return null;
		}
		
		IExpression arg = arguments[0];
		
		if (arg.getASTType() == IExpression.STRING_LITERAL) {
			return (IStringLiteral) arg;
		}
		else if (arg.getASTType() == IExpression.OBJECT_LITERAL) {
			IObjectLiteral definition = (IObjectLiteral) arg;
			IExpression xclass = getObjectField(definition, XCLASS);
			
			if (xclass.getASTType() == IExpression.STRING_LITERAL) {
				return (IStringLiteral) xclass;
			}
		}
		
		return null;
	}
}
