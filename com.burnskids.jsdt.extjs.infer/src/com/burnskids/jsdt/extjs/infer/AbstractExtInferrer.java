package com.burnskids.jsdt.extjs.infer;

import org.eclipse.wst.jsdt.core.ast.ASTVisitor;
import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.core.ast.IFunctionCall;
import org.eclipse.wst.jsdt.core.ast.IObjectLiteral;
import org.eclipse.wst.jsdt.core.ast.IObjectLiteralField;
import org.eclipse.wst.jsdt.core.ast.ISingleNameReference;
import org.eclipse.wst.jsdt.core.ast.IStringLiteral;
import org.eclipse.wst.jsdt.core.compiler.CharOperation;

public class AbstractExtInferrer extends ASTVisitor {

	private static final char[] EXT = new char[] { 'E', 'x', 't' };
	
	protected final SenchaInferEngine parent;
	
	public AbstractExtInferrer(SenchaInferEngine parent) {
		this.parent = parent;
	}
	
	protected static boolean isExt(IExpression expression) {
		if (expression != null && expression.getASTType() == IExpression.SINGLE_NAME_REFERENCE) {
			ISingleNameReference name = (ISingleNameReference) expression;
			
			return CharOperation.equals(name.getToken(), EXT);
		}
		
		return false;
	}
	
	protected static boolean isExtMethod(IFunctionCall functionCall, char[] method) {
		return isExt(functionCall.getReceiver()) && CharOperation.equals(functionCall.getSelector(), method);
	}
	
	protected static IExpression getObjectField(IObjectLiteral object, char[] name) {
		for (IObjectLiteralField field : object.getFields()) {
			if (CharOperation.equals(fieldNameFor(field), name)) {
				return field.getInitializer();
			}
		}
		
		return null;
	}
	
	protected static char[] fieldNameFor(IObjectLiteralField field) {
		IExpression fieldName = field.getFieldName();
		
		if (fieldName.getASTType() == IExpression.SINGLE_NAME_REFERENCE) {
			return ((ISingleNameReference) fieldName).getToken();
		}
		else if (fieldName.getASTType() == IExpression.STRING_LITERAL) {
			return ((IStringLiteral) fieldName).source();
		}
		else {
			// TODO: throw an exception here instead of failing silently?
			return null;
		}
	}
}
