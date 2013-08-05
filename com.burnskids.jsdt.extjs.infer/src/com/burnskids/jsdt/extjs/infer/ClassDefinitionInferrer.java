package com.burnskids.jsdt.extjs.infer;

import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.core.ast.IFunctionDeclaration;
import org.eclipse.wst.jsdt.core.ast.IFunctionExpression;
import org.eclipse.wst.jsdt.core.ast.IObjectLiteral;
import org.eclipse.wst.jsdt.core.ast.IObjectLiteralField;
import org.eclipse.wst.jsdt.core.ast.IStringLiteral;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;

public class ClassDefinitionInferrer extends AbstractClassInferrer {
	
	private static final char[] CONSTRUCTOR = new char[] { 'c', 'o', 'n', 's', 't', 'r', 'u', 'c', 't', 'o', 'r' };
	
	public ClassDefinitionInferrer(SenchaInferEngine parent) {
		super(parent);
	}

	@Override
	protected void handleClass(IStringLiteral name, IObjectLiteral definition) {
		InferredType type = parent.addType(name.source());
		
		for (IObjectLiteralField field : definition.getFields()) {
			addField(field, type);
		}
		
		type.setIsGlobal(true);
		type.setIsDefinition(true);
		type.setModifiers(ClassFileConstants.AccPublic);
		
		type.setNameStart(name.sourceStart() + 1);
		type.updatePositions(definition.sourceStart(), definition.sourceEnd());
		
		definition.setInferredType(type);
	}
	
	private void addField(IObjectLiteralField field, InferredType type) {
		char[] name = fieldNameFor(field);
		
		IExpression fieldName = field.getFieldName();
		int nameStart = fieldName.sourceStart() + (fieldName.getASTType() == IExpression.STRING_LITERAL ? 1 : 0);
		
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
		else if (!ClassInheritanceInferrer.isReserved(name)) {
			type.addAttribute(name, fieldValue, nameStart);
		}
	}
}
