package com.burnskids.jsdt.extjs.infer;

import org.eclipse.wst.jsdt.core.ast.ASTVisitor;
import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.core.ast.IFunctionCall;
import org.eclipse.wst.jsdt.core.ast.IFunctionExpression;
import org.eclipse.wst.jsdt.core.ast.IObjectLiteral;
import org.eclipse.wst.jsdt.core.ast.IReturnStatement;
import org.eclipse.wst.jsdt.core.ast.IStringLiteral;
import org.eclipse.wst.jsdt.core.infer.InferredType;

public abstract class AbstractClassInferrer extends AbstractExtInferrer {

	private static final char[] DEFINE = new char[] { 'd', 'e', 'f', 'i', 'n', 'e' };
	
	public AbstractClassInferrer(SenchaInferEngine parent) {
		super(parent);
	}
	
	@Override
	public boolean visit(IFunctionCall functionCall) {
		if (isExtMethod(functionCall, DEFINE)) {
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
		
		this.handleClass(name, definition);
	}
	
	protected abstract void handleClass(IStringLiteral name, IObjectLiteral definition);
	
	protected static void addModifiers(InferredType type, int modifiers) {
		type.setModifiers(type.getModifiers() | modifiers);
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