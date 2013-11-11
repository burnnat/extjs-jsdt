package com.burnskids.jsdt.extjs.infer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.wst.jsdt.core.ast.ASTVisitor;
import org.eclipse.wst.jsdt.core.ast.IExpression;
import org.eclipse.wst.jsdt.core.ast.IFunctionDeclaration;
import org.eclipse.wst.jsdt.core.ast.IFunctionExpression;
import org.eclipse.wst.jsdt.core.ast.IObjectLiteral;
import org.eclipse.wst.jsdt.core.ast.IObjectLiteralField;
import org.eclipse.wst.jsdt.core.ast.IStringLiteral;
import org.eclipse.wst.jsdt.core.infer.InferredAttribute;
import org.eclipse.wst.jsdt.core.infer.InferredMember;
import org.eclipse.wst.jsdt.core.infer.InferredType;
import org.eclipse.wst.jsdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.classfmt.ClassFileConstants;

public class ClassDefinitionInferrer extends AbstractClassInferrer {
	
	private static final char[] CONSTRUCTOR = new char[] { 'c', 'o', 'n', 's', 't', 'r', 'u', 'c', 't', 'o', 'r' };
	private static final char[] STATICS = new char[] { 's', 't', 'a', 't', 'i', 'c', 's' };
	private static final char[] CONFIG = new char[] { 'c', 'o', 'n', 'f', 'i', 'g' };
	private static final char[] ALIAS = new char[] { 'a', 'l', 'i', 'a', 's' };
	private static final char[] SINGLETON = new char[] { 's', 'i', 'n', 'g', 'l', 'e', 't', 'o', 'n' };
	private static final char[] ALTERNATE_CLASS_NAME = new char[] { 'a', 'l', 't', 'e', 'r', 'n', 'a', 't', 'e', 'C', 'l', 'a', 's', 's', 'N', 'a', 'm', 'e' };
	private static final char[] OVERRIDE = new char[] { 'o', 'v', 'e', 'r', 'r', 'i', 'd', 'e' };
	
	public ClassDefinitionInferrer(SenchaInferEngine parent) {
		super(parent);
	}

	@Override
	protected void handleClass(IStringLiteral name, IObjectLiteral definition) {
		IStringLiteral className = name;
		
		if (definition == null) {
			parent.addType(className.source());
			return;
		}
		
		for (IObjectLiteralField field : definition.getFields()) {
			if (equal(fieldNameFor(field), OVERRIDE)) {
				IExpression value = field.getInitializer();
				
				if (value.getASTType() == IExpression.STRING_LITERAL) {
					className = (IStringLiteral) value;
					break;
				}
			}
		}
		
		InferredType type = parent.addType(className.source());
		
		type.setIsGlobal(true);
		type.setIsDefinition(true);
		
		type.setNameStart(className.sourceStart() + 1);
		type.updatePositions(definition.sourceStart(), definition.sourceEnd());
		
		for (IObjectLiteralField field : definition.getFields()) {
			addField(field, type);
		}
		
		addModifiers(type, ClassFileConstants.AccPublic);
		
		InferredType[] synonyms = type.getSynonyms();
		
		if (synonyms != null) {
			for (InferredType synonym : synonyms) {
				synonym.mixin(type);
			}
		}
		
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
			if (equal(name, STATICS) && fieldValue.getASTType() == IExpression.OBJECT_LITERAL) {
				handleStatics(type, (IObjectLiteral) fieldValue);
			}
			else if (equal(name, CONFIG) && fieldValue.getASTType() == IExpression.OBJECT_LITERAL) {
				handleConfig(type, (IObjectLiteral) fieldValue);
			}
			else if (equal(name, ALIAS)) {
				
			}
			else if (equal(name, ALTERNATE_CLASS_NAME)) {
				final List<IStringLiteral> synonyms = new ArrayList<IStringLiteral>();
				
				if (fieldValue.getASTType() == IExpression.STRING_LITERAL) {
					synonyms.add((IStringLiteral) fieldValue);
				}
				else if (fieldValue.getASTType() == IExpression.ARRAY_INITIALIZER) {
					fieldValue.traverse(new ASTVisitor() {
						@Override
						public boolean visit(IStringLiteral name) {
							synonyms.add(name);
							return true;
						}
					});
				}
				
				for (IStringLiteral synonym : synonyms) {
					InferredType linked = parent.addType(synonym.source());
					
					linked.setIsGlobal(true);
					linked.setIsDefinition(true);
					linked.setNameStart(synonym.sourceStart() + 1);
					
					type.addSynonym(linked);
				}
			}
			else if (equal(name, SINGLETON)) {
				type.isAnonymous = true;
				
				parent.getGlobalType().addAttribute(
					new InferredAttribute(type.getName(), type, type.sourceStart(), type.sourceEnd())
				);
			}
			else {
				type.addAttribute(name, fieldValue, nameStart);
			}
		}
	}
	
	private void handleStatics(InferredType type, IObjectLiteral statics) {
		for (IObjectLiteralField field : statics.getFields()) {
			char[] name = fieldNameFor(field);
			
			IExpression fieldName = field.getFieldName();
			int nameStart = fieldName.sourceStart() + (fieldName.getASTType() == IExpression.STRING_LITERAL ? 1 : 0);
			
			IExpression value = field.getInitializer();
			InferredMember member;
			
			if (value.getASTType() == IExpression.FUNCTION_EXPRESSION) {
				MethodDeclaration declaration = ((IFunctionExpression) value).getMethodDeclaration();
				declaration.getInferredMethod().isStatic = true;
				
				member = type.addMethod(name, declaration, nameStart);
			}
			else {
				member = type.addAttribute(name, value, nameStart);
			}
			
			member.isStatic = true;
		}
	}
	
	private void handleConfig(InferredType type, IObjectLiteral statics) {
		for (IObjectLiteralField field : statics.getFields()) {
			char[] name = fieldNameFor(field);
			
			String capitalized = String.valueOf(name);
			capitalized = capitalized.substring(0, 1).toUpperCase() + capitalized.substring(1);
			
			type.addMethod(("get" + capitalized).toCharArray(), new MethodDeclaration(null), 0);
			type.addMethod(("set" + capitalized).toCharArray(), new MethodDeclaration(null), 0);
		}
	}
}
