package com.burnskids.jsdt.extjs.infer;

import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.core.ast.IObjectLiteral;
import org.eclipse.wst.jsdt.core.ast.IObjectLiteralField;
import org.eclipse.wst.jsdt.core.ast.IStringLiteral;
import org.eclipse.wst.jsdt.core.infer.InferredType;

public class ClassInheritanceInferrer extends AbstractClassInferrer {
	
	private static final char[] EXTEND = new char[] { 'e', 'x', 't', 'e', 'n', 'd' };
	private static final char[] MIXINS = new char[] { 'm', 'i', 'x', 'i', 'n', 's' };
	
	public ClassInheritanceInferrer(SenchaInferEngine parent) {
		super(parent);
	}
	
	@Override
	protected void handleClass(IStringLiteral name, IObjectLiteral definition) {
		InferredType type = parent.getType(name.source());
		
		for (IObjectLiteralField field : definition.getFields()) {
			char[] fieldName = fieldNameFor(field);
			IExpression fieldValue = field.getInitializer();
			
			if (equal(fieldName, EXTEND) && fieldValue.getASTType() == IExpression.STRING_LITERAL) {
				handleExtend(type, (IStringLiteral) fieldValue);
			}
			else if (equal(fieldName, MIXINS) && fieldValue.getASTType() == IExpression.OBJECT_LITERAL) {
				handleMixins(type, (IObjectLiteral) fieldValue);
			}
		}
	}
	
	private void handleExtend(InferredType type, IStringLiteral value) {
		type.setSuperType(
			parent.createTypeIfNeeded(value.source())
		);
	}
	
	private void handleMixins(InferredType type, IObjectLiteral value) {
		for (IObjectLiteralField field : value.getFields()) {
			IExpression fieldValue = field.getInitializer();
			
			if (fieldValue.getASTType() == IExpression.STRING_LITERAL) {
				handleMixin(type, (IStringLiteral) fieldValue);
			}
		}
	}
	
	private void handleMixin(InferredType type, IStringLiteral value) {
		type.addMixin(value.source());
	}
	
	public static boolean isReserved(char[] name) {
		return equal(name, EXTEND) || equal(name, MIXINS);
	}
}
