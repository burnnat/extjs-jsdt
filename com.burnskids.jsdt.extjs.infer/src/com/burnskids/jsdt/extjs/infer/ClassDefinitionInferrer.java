package com.burnskids.jsdt.extjs.infer;

import org.eclipse.wst.jsdt.core.ast.ASTVisitor;
import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.core.ast.IFunctionCall;
import org.eclipse.wst.jsdt.core.ast.IFunctionDeclaration;
import org.eclipse.wst.jsdt.core.ast.IFunctionExpression;
import org.eclipse.wst.jsdt.core.ast.IObjectLiteral;
import org.eclipse.wst.jsdt.core.ast.IObjectLiteralField;
import org.eclipse.wst.jsdt.core.ast.IReturnStatement;
import org.eclipse.wst.jsdt.core.ast.ISingleNameReference;
import org.eclipse.wst.jsdt.core.ast.IStringLiteral;
import org.eclipse.wst.jsdt.core.infer.InferredType;

public class ClassDefinitionInferrer extends ASTVisitor {
	
	private static final char[] EXT = new char[] { 'E', 'x', 't' };
	private static final char[] DEFINE = new char[] { 'd', 'e', 'f', 'i', 'n', 'e' };
	private static final char[] CONSTRUCTOR = new char[] { 'c', 'o', 'n', 's', 't', 'r', 'u', 'c', 't', 'o', 'r' };
	
	private final SenchaInferEngine parent;
	
	public ClassDefinitionInferrer(SenchaInferEngine parent) {
		this.parent = parent;
	}
	
	@Override
	public boolean visit(IFunctionCall functionCall) {
		if (isExt(functionCall.getReceiver()) && equal(functionCall.getSelector(), DEFINE)) {
			parseDefine(functionCall.getArguments());
		}
		
		return super.visit(functionCall);
	}

	private void parseDefine(IExpression[] arguments) {
		if (arguments.length < 2) {
			return;
		}
		
		IExpression arg = arguments[0];
		
		if (arg.getASTType() != IExpression.STRING_LITERAL) {
			return;
		}
		
		IStringLiteral name = (IStringLiteral) arg;
		IObjectLiteral definition;
		
		arg = arguments[1];
		
		if (arg.getASTType() == IExpression.OBJECT_LITERAL) {
			definition = (IObjectLiteral) arg;
		}
		else if (arg.getASTType() == IExpression.FUNCTION_EXPRESSION) {
			ReturnValueParser bodyParser = new ReturnValueParser((IFunctionExpression) arg);
			IExpression returnValue = bodyParser.getReturnValue();
			
			if (returnValue.getASTType() == IExpression.OBJECT_LITERAL) {
				definition = (IObjectLiteral) returnValue;
			}
			else {
				return;
			}
		}
		else {
			return;
		}
		
		InferredType type = parent.addType(name.source());
		
		for (IObjectLiteralField field : definition.getFields()) {
			addField(field, type);
		}
		
		type.setIsGlobal(true);
		type.setNameStart(name.sourceStart() + 1);
		type.updatePositions(definition.sourceStart(), definition.sourceEnd());
		
		definition.setInferredType(type);
	}

	private void addField(IObjectLiteralField field, InferredType type) {
		IExpression fieldName = field.getFieldName();
		char[] name;
		int nameStart;
		
		if (fieldName.getASTType() == IExpression.SINGLE_NAME_REFERENCE) {
			name = ((ISingleNameReference) fieldName).getToken();
			nameStart = fieldName.sourceStart();
		}
		else if (fieldName.getASTType() == IExpression.STRING_LITERAL) {
			name = ((IStringLiteral) fieldName).source();
			nameStart = fieldName.sourceStart() + 1;
		}
		else {
			// TODO: throw an exception here instead of failing silently?
			return;
		}
		
		IExpression fieldValue = field.getInitializer();
		
		if (fieldValue.getASTType() == IExpression.FUNCTION_EXPRESSION) {
			IFunctionDeclaration declaration = ((IFunctionExpression) fieldValue).getMethodDeclaration();
			
			if (equal(name, CONSTRUCTOR)) {
				type.addConstructorMethod(name, declaration, nameStart);
			}
			else {
				type.addMethod(name, declaration, nameStart);
			}
		}
		else {
			type.addAttribute(name, fieldValue, nameStart);
		}
	}

	private static boolean isExt(IExpression expression) {
		if (expression != null && expression.getASTType() == IExpression.SINGLE_NAME_REFERENCE) {
			ISingleNameReference name = (ISingleNameReference) expression;
			
			return equal(name.getToken(), EXT);
		}
		
		return false;
	}
	
	private static boolean equal(char[] first, char[] second) {
		if (first.length != second.length) {
			return false;
		}
		
		for (int i = 0; i < first.length; i++) {
			if (first[i] != second[i]) {
				return false;
			}
		}
		
		return true;
	}
	
	private class ReturnValueParser extends ASTVisitor {
		private IReturnStatement returnStatement;
		
		protected ReturnValueParser(IFunctionExpression body) {
			body.traverse(this);
		}
		
		@Override
		public boolean visit(IReturnStatement returnStatement) {
			this.returnStatement = returnStatement;
			return super.visit(returnStatement);
		}
		
		protected IExpression getReturnValue() {
			return returnStatement.getExpression();
		}
	}
}
