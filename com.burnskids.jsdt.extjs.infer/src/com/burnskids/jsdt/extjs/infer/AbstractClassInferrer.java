package com.burnskids.jsdt.extjs.infer;

import org.eclipse.wst.jsdt.core.ast.ASTVisitor;
import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.core.ast.IFunctionCall;
import org.eclipse.wst.jsdt.core.ast.IFunctionExpression;
import org.eclipse.wst.jsdt.core.ast.IObjectLiteral;
import org.eclipse.wst.jsdt.core.ast.IObjectLiteralField;
import org.eclipse.wst.jsdt.core.ast.IReturnStatement;
import org.eclipse.wst.jsdt.core.ast.ISingleNameReference;
import org.eclipse.wst.jsdt.core.ast.IStringLiteral;

public abstract class AbstractClassInferrer extends ASTVisitor {

	private static final char[] EXT = new char[] { 'E', 'x', 't' };
	private static final char[] DEFINE = new char[] { 'd', 'e', 'f', 'i', 'n', 'e' };
	
	protected final SenchaInferEngine parent;
	
	public AbstractClassInferrer(SenchaInferEngine parent) {
		this.parent = parent;
	}
	
	@Override
	public boolean visit(IFunctionCall functionCall) {
		if (isExt(functionCall.getReceiver()) && equal(functionCall.getSelector(), DEFINE)) {
			parseDefine(functionCall.getArguments());
		}
		
		return super.visit(functionCall);
	}

	protected static boolean isExt(IExpression expression) {
		if (expression != null && expression.getASTType() == IExpression.SINGLE_NAME_REFERENCE) {
			ISingleNameReference name = (ISingleNameReference) expression;
			
			return equal(name.getToken(), EXT);
		}
		
		return false;
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
		
		this.handleClass(name, definition);
	}
	
	protected abstract void handleClass(IStringLiteral name, IObjectLiteral definition);
	
	protected static boolean equal(char[] first, char[] second) {
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
	
	class ReturnValueParser extends ASTVisitor {
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