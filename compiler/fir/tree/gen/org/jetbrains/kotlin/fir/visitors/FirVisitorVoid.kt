/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.contracts.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.types.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirVisitorVoid : FirVisitor<Unit, Nothing?>() {

    abstract fun visitElement(element: FirElement)

    final override fun visitAnnotationContainer(annotationContainer: FirAnnotationContainer, data: Nothing?) {
        visitAnnotationContainer(annotationContainer)
    }

    open fun visitAnnotationContainer(annotationContainer: FirAnnotationContainer) {
        visitElement(annotationContainer)
    }

    final override fun visitTypeRef(typeRef: FirTypeRef, data: Nothing?) {
        visitTypeRef(typeRef)
    }

    open fun visitTypeRef(typeRef: FirTypeRef) {
        visitElement(typeRef)
    }

    final override fun visitReference(reference: FirReference, data: Nothing?) {
        visitReference(reference)
    }

    open fun visitReference(reference: FirReference) {
        visitElement(reference)
    }

    final override fun visitLabel(label: FirLabel, data: Nothing?) {
        visitLabel(label)
    }

    open fun visitLabel(label: FirLabel) {
        visitElement(label)
    }

    final override fun visitResolvable(resolvable: FirResolvable, data: Nothing?) {
        visitResolvable(resolvable)
    }

    open fun visitResolvable(resolvable: FirResolvable) {
        visitElement(resolvable)
    }

    final override fun visitTargetElement(targetElement: FirTargetElement, data: Nothing?) {
        visitTargetElement(targetElement)
    }

    open fun visitTargetElement(targetElement: FirTargetElement) {
        visitElement(targetElement)
    }

    final override fun visitDeclarationStatus(declarationStatus: FirDeclarationStatus, data: Nothing?) {
        visitDeclarationStatus(declarationStatus)
    }

    open fun visitDeclarationStatus(declarationStatus: FirDeclarationStatus) {
        visitElement(declarationStatus)
    }

    final override fun visitResolvedDeclarationStatus(resolvedDeclarationStatus: FirResolvedDeclarationStatus, data: Nothing?) {
        visitResolvedDeclarationStatus(resolvedDeclarationStatus)
    }

    open fun visitResolvedDeclarationStatus(resolvedDeclarationStatus: FirResolvedDeclarationStatus) {
        visitElement(resolvedDeclarationStatus)
    }

    final override fun visitControlFlowGraphOwner(controlFlowGraphOwner: FirControlFlowGraphOwner, data: Nothing?) {
        visitControlFlowGraphOwner(controlFlowGraphOwner)
    }

    open fun visitControlFlowGraphOwner(controlFlowGraphOwner: FirControlFlowGraphOwner) {
        visitElement(controlFlowGraphOwner)
    }

    final override fun visitStatement(statement: FirStatement, data: Nothing?) {
        visitStatement(statement)
    }

    open fun visitStatement(statement: FirStatement) {
        visitElement(statement)
    }

    final override fun visitExpression(expression: FirExpression, data: Nothing?) {
        visitExpression(expression)
    }

    open fun visitExpression(expression: FirExpression) {
        visitElement(expression)
    }

    final override fun visitLazyExpression(lazyExpression: FirLazyExpression, data: Nothing?) {
        visitLazyExpression(lazyExpression)
    }

    open fun visitLazyExpression(lazyExpression: FirLazyExpression) {
        visitElement(lazyExpression)
    }

    final override fun visitContextReceiver(contextReceiver: FirContextReceiver, data: Nothing?) {
        visitContextReceiver(contextReceiver)
    }

    open fun visitContextReceiver(contextReceiver: FirContextReceiver) {
        visitElement(contextReceiver)
    }

    final override fun visitElementWithResolveState(elementWithResolveState: FirElementWithResolveState, data: Nothing?) {
        visitElementWithResolveState(elementWithResolveState)
    }

    open fun visitElementWithResolveState(elementWithResolveState: FirElementWithResolveState) {
        visitElement(elementWithResolveState)
    }

    final override fun visitFileAnnotationsContainer(fileAnnotationsContainer: FirFileAnnotationsContainer, data: Nothing?) {
        visitFileAnnotationsContainer(fileAnnotationsContainer)
    }

    open fun visitFileAnnotationsContainer(fileAnnotationsContainer: FirFileAnnotationsContainer) {
        visitElement(fileAnnotationsContainer)
    }

    final override fun visitDeclaration(declaration: FirDeclaration, data: Nothing?) {
        visitDeclaration(declaration)
    }

    open fun visitDeclaration(declaration: FirDeclaration) {
        visitElement(declaration)
    }

    final override fun visitTypeParameterRefsOwner(typeParameterRefsOwner: FirTypeParameterRefsOwner, data: Nothing?) {
        visitTypeParameterRefsOwner(typeParameterRefsOwner)
    }

    open fun visitTypeParameterRefsOwner(typeParameterRefsOwner: FirTypeParameterRefsOwner) {
        visitElement(typeParameterRefsOwner)
    }

    final override fun visitTypeParametersOwner(typeParametersOwner: FirTypeParametersOwner, data: Nothing?) {
        visitTypeParametersOwner(typeParametersOwner)
    }

    open fun visitTypeParametersOwner(typeParametersOwner: FirTypeParametersOwner) {
        visitElement(typeParametersOwner)
    }

    final override fun visitMemberDeclaration(memberDeclaration: FirMemberDeclaration, data: Nothing?) {
        visitMemberDeclaration(memberDeclaration)
    }

    open fun visitMemberDeclaration(memberDeclaration: FirMemberDeclaration) {
        visitElement(memberDeclaration)
    }

    final override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: Nothing?) {
        visitAnonymousInitializer(anonymousInitializer)
    }

    open fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer) {
        visitElement(anonymousInitializer)
    }

    final override fun visitCallableDeclaration(callableDeclaration: FirCallableDeclaration, data: Nothing?) {
        visitCallableDeclaration(callableDeclaration)
    }

    open fun visitCallableDeclaration(callableDeclaration: FirCallableDeclaration) {
        visitElement(callableDeclaration)
    }

    final override fun visitTypeParameterRef(typeParameterRef: FirTypeParameterRef, data: Nothing?) {
        visitTypeParameterRef(typeParameterRef)
    }

    open fun visitTypeParameterRef(typeParameterRef: FirTypeParameterRef) {
        visitElement(typeParameterRef)
    }

    final override fun visitTypeParameter(typeParameter: FirTypeParameter, data: Nothing?) {
        visitTypeParameter(typeParameter)
    }

    open fun visitTypeParameter(typeParameter: FirTypeParameter) {
        visitElement(typeParameter)
    }

    final override fun visitConstructedClassTypeParameterRef(constructedClassTypeParameterRef: FirConstructedClassTypeParameterRef, data: Nothing?) {
        visitConstructedClassTypeParameterRef(constructedClassTypeParameterRef)
    }

    open fun visitConstructedClassTypeParameterRef(constructedClassTypeParameterRef: FirConstructedClassTypeParameterRef) {
        visitElement(constructedClassTypeParameterRef)
    }

    final override fun visitOuterClassTypeParameterRef(outerClassTypeParameterRef: FirOuterClassTypeParameterRef, data: Nothing?) {
        visitOuterClassTypeParameterRef(outerClassTypeParameterRef)
    }

    open fun visitOuterClassTypeParameterRef(outerClassTypeParameterRef: FirOuterClassTypeParameterRef) {
        visitElement(outerClassTypeParameterRef)
    }

    final override fun visitVariable(variable: FirVariable, data: Nothing?) {
        visitVariable(variable)
    }

    open fun visitVariable(variable: FirVariable) {
        visitElement(variable)
    }

    final override fun visitValueParameter(valueParameter: FirValueParameter, data: Nothing?) {
        visitValueParameter(valueParameter)
    }

    open fun visitValueParameter(valueParameter: FirValueParameter) {
        visitElement(valueParameter)
    }

    final override fun visitReceiverParameter(receiverParameter: FirReceiverParameter, data: Nothing?) {
        visitReceiverParameter(receiverParameter)
    }

    open fun visitReceiverParameter(receiverParameter: FirReceiverParameter) {
        visitElement(receiverParameter)
    }

    final override fun visitProperty(property: FirProperty, data: Nothing?) {
        visitProperty(property)
    }

    open fun visitProperty(property: FirProperty) {
        visitElement(property)
    }

    final override fun visitField(field: FirField, data: Nothing?) {
        visitField(field)
    }

    open fun visitField(field: FirField) {
        visitElement(field)
    }

    final override fun visitEnumEntry(enumEntry: FirEnumEntry, data: Nothing?) {
        visitEnumEntry(enumEntry)
    }

    open fun visitEnumEntry(enumEntry: FirEnumEntry) {
        visitElement(enumEntry)
    }

    final override fun visitFunctionTypeParameter(functionTypeParameter: FirFunctionTypeParameter, data: Nothing?) {
        visitFunctionTypeParameter(functionTypeParameter)
    }

    open fun visitFunctionTypeParameter(functionTypeParameter: FirFunctionTypeParameter) {
        visitElement(functionTypeParameter)
    }

    final override fun visitClassLikeDeclaration(classLikeDeclaration: FirClassLikeDeclaration, data: Nothing?) {
        visitClassLikeDeclaration(classLikeDeclaration)
    }

    open fun visitClassLikeDeclaration(classLikeDeclaration: FirClassLikeDeclaration) {
        visitElement(classLikeDeclaration)
    }

    final override fun visitClass(klass: FirClass, data: Nothing?) {
        visitClass(klass)
    }

    open fun visitClass(klass: FirClass) {
        visitElement(klass)
    }

    final override fun visitRegularClass(regularClass: FirRegularClass, data: Nothing?) {
        visitRegularClass(regularClass)
    }

    open fun visitRegularClass(regularClass: FirRegularClass) {
        visitElement(regularClass)
    }

    final override fun visitTypeAlias(typeAlias: FirTypeAlias, data: Nothing?) {
        visitTypeAlias(typeAlias)
    }

    open fun visitTypeAlias(typeAlias: FirTypeAlias) {
        visitElement(typeAlias)
    }

    final override fun visitFunction(function: FirFunction, data: Nothing?) {
        visitFunction(function)
    }

    open fun visitFunction(function: FirFunction) {
        visitElement(function)
    }

    final override fun visitContractDescriptionOwner(contractDescriptionOwner: FirContractDescriptionOwner, data: Nothing?) {
        visitContractDescriptionOwner(contractDescriptionOwner)
    }

    open fun visitContractDescriptionOwner(contractDescriptionOwner: FirContractDescriptionOwner) {
        visitElement(contractDescriptionOwner)
    }

    final override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: Nothing?) {
        visitSimpleFunction(simpleFunction)
    }

    open fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
        visitElement(simpleFunction)
    }

    final override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: Nothing?) {
        visitPropertyAccessor(propertyAccessor)
    }

    open fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor) {
        visitElement(propertyAccessor)
    }

    final override fun visitBackingField(backingField: FirBackingField, data: Nothing?) {
        visitBackingField(backingField)
    }

    open fun visitBackingField(backingField: FirBackingField) {
        visitElement(backingField)
    }

    final override fun visitConstructor(constructor: FirConstructor, data: Nothing?) {
        visitConstructor(constructor)
    }

    open fun visitConstructor(constructor: FirConstructor) {
        visitElement(constructor)
    }

    final override fun visitFile(file: FirFile, data: Nothing?) {
        visitFile(file)
    }

    open fun visitFile(file: FirFile) {
        visitElement(file)
    }

    final override fun visitScript(script: FirScript, data: Nothing?) {
        visitScript(script)
    }

    open fun visitScript(script: FirScript) {
        visitElement(script)
    }

    final override fun visitCodeFragment(codeFragment: FirCodeFragment, data: Nothing?) {
        visitCodeFragment(codeFragment)
    }

    open fun visitCodeFragment(codeFragment: FirCodeFragment) {
        visitElement(codeFragment)
    }

    final override fun visitPackageDirective(packageDirective: FirPackageDirective, data: Nothing?) {
        visitPackageDirective(packageDirective)
    }

    open fun visitPackageDirective(packageDirective: FirPackageDirective) {
        visitElement(packageDirective)
    }

    final override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: Nothing?) {
        visitAnonymousFunction(anonymousFunction)
    }

    open fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
        visitElement(anonymousFunction)
    }

    final override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression, data: Nothing?) {
        visitAnonymousFunctionExpression(anonymousFunctionExpression)
    }

    open fun visitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression) {
        visitElement(anonymousFunctionExpression)
    }

    final override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: Nothing?) {
        visitAnonymousObject(anonymousObject)
    }

    open fun visitAnonymousObject(anonymousObject: FirAnonymousObject) {
        visitElement(anonymousObject)
    }

    final override fun visitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression, data: Nothing?) {
        visitAnonymousObjectExpression(anonymousObjectExpression)
    }

    open fun visitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression) {
        visitElement(anonymousObjectExpression)
    }

    final override fun visitDiagnosticHolder(diagnosticHolder: FirDiagnosticHolder, data: Nothing?) {
        visitDiagnosticHolder(diagnosticHolder)
    }

    open fun visitDiagnosticHolder(diagnosticHolder: FirDiagnosticHolder) {
        visitElement(diagnosticHolder)
    }

    final override fun visitImport(import: FirImport, data: Nothing?) {
        visitImport(import)
    }

    open fun visitImport(import: FirImport) {
        visitElement(import)
    }

    final override fun visitResolvedImport(resolvedImport: FirResolvedImport, data: Nothing?) {
        visitResolvedImport(resolvedImport)
    }

    open fun visitResolvedImport(resolvedImport: FirResolvedImport) {
        visitElement(resolvedImport)
    }

    final override fun visitErrorImport(errorImport: FirErrorImport, data: Nothing?) {
        visitErrorImport(errorImport)
    }

    open fun visitErrorImport(errorImport: FirErrorImport) {
        visitElement(errorImport)
    }

    final override fun visitLoop(loop: FirLoop, data: Nothing?) {
        visitLoop(loop)
    }

    open fun visitLoop(loop: FirLoop) {
        visitElement(loop)
    }

    final override fun visitErrorLoop(errorLoop: FirErrorLoop, data: Nothing?) {
        visitErrorLoop(errorLoop)
    }

    open fun visitErrorLoop(errorLoop: FirErrorLoop) {
        visitElement(errorLoop)
    }

    final override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: Nothing?) {
        visitDoWhileLoop(doWhileLoop)
    }

    open fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop) {
        visitElement(doWhileLoop)
    }

    final override fun visitWhileLoop(whileLoop: FirWhileLoop, data: Nothing?) {
        visitWhileLoop(whileLoop)
    }

    open fun visitWhileLoop(whileLoop: FirWhileLoop) {
        visitElement(whileLoop)
    }

    final override fun visitBlock(block: FirBlock, data: Nothing?) {
        visitBlock(block)
    }

    open fun visitBlock(block: FirBlock) {
        visitElement(block)
    }

    final override fun visitLazyBlock(lazyBlock: FirLazyBlock, data: Nothing?) {
        visitLazyBlock(lazyBlock)
    }

    open fun visitLazyBlock(lazyBlock: FirLazyBlock) {
        visitElement(lazyBlock)
    }

    final override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: Nothing?) {
        visitBinaryLogicExpression(binaryLogicExpression)
    }

    open fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression) {
        visitElement(binaryLogicExpression)
    }

    final override fun <E : FirTargetElement> visitJump(jump: FirJump<E>, data: Nothing?) {
        visitJump(jump)
    }

    open fun <E : FirTargetElement> visitJump(jump: FirJump<E>) {
        visitElement(jump)
    }

    final override fun visitLoopJump(loopJump: FirLoopJump, data: Nothing?) {
        visitLoopJump(loopJump)
    }

    open fun visitLoopJump(loopJump: FirLoopJump) {
        visitElement(loopJump)
    }

    final override fun visitBreakExpression(breakExpression: FirBreakExpression, data: Nothing?) {
        visitBreakExpression(breakExpression)
    }

    open fun visitBreakExpression(breakExpression: FirBreakExpression) {
        visitElement(breakExpression)
    }

    final override fun visitContinueExpression(continueExpression: FirContinueExpression, data: Nothing?) {
        visitContinueExpression(continueExpression)
    }

    open fun visitContinueExpression(continueExpression: FirContinueExpression) {
        visitElement(continueExpression)
    }

    final override fun visitCatch(catch: FirCatch, data: Nothing?) {
        visitCatch(catch)
    }

    open fun visitCatch(catch: FirCatch) {
        visitElement(catch)
    }

    final override fun visitTryExpression(tryExpression: FirTryExpression, data: Nothing?) {
        visitTryExpression(tryExpression)
    }

    open fun visitTryExpression(tryExpression: FirTryExpression) {
        visitElement(tryExpression)
    }

    final override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: Nothing?) {
        visitConstExpression(constExpression)
    }

    open fun <T> visitConstExpression(constExpression: FirConstExpression<T>) {
        visitElement(constExpression)
    }

    final override fun visitTypeProjection(typeProjection: FirTypeProjection, data: Nothing?) {
        visitTypeProjection(typeProjection)
    }

    open fun visitTypeProjection(typeProjection: FirTypeProjection) {
        visitElement(typeProjection)
    }

    final override fun visitStarProjection(starProjection: FirStarProjection, data: Nothing?) {
        visitStarProjection(starProjection)
    }

    open fun visitStarProjection(starProjection: FirStarProjection) {
        visitElement(starProjection)
    }

    final override fun visitPlaceholderProjection(placeholderProjection: FirPlaceholderProjection, data: Nothing?) {
        visitPlaceholderProjection(placeholderProjection)
    }

    open fun visitPlaceholderProjection(placeholderProjection: FirPlaceholderProjection) {
        visitElement(placeholderProjection)
    }

    final override fun visitTypeProjectionWithVariance(typeProjectionWithVariance: FirTypeProjectionWithVariance, data: Nothing?) {
        visitTypeProjectionWithVariance(typeProjectionWithVariance)
    }

    open fun visitTypeProjectionWithVariance(typeProjectionWithVariance: FirTypeProjectionWithVariance) {
        visitElement(typeProjectionWithVariance)
    }

    final override fun visitArgumentList(argumentList: FirArgumentList, data: Nothing?) {
        visitArgumentList(argumentList)
    }

    open fun visitArgumentList(argumentList: FirArgumentList) {
        visitElement(argumentList)
    }

    final override fun visitCall(call: FirCall, data: Nothing?) {
        visitCall(call)
    }

    open fun visitCall(call: FirCall) {
        visitElement(call)
    }

    final override fun visitAnnotation(annotation: FirAnnotation, data: Nothing?) {
        visitAnnotation(annotation)
    }

    open fun visitAnnotation(annotation: FirAnnotation) {
        visitElement(annotation)
    }

    final override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: Nothing?) {
        visitAnnotationCall(annotationCall)
    }

    open fun visitAnnotationCall(annotationCall: FirAnnotationCall) {
        visitElement(annotationCall)
    }

    final override fun visitAnnotationArgumentMapping(annotationArgumentMapping: FirAnnotationArgumentMapping, data: Nothing?) {
        visitAnnotationArgumentMapping(annotationArgumentMapping)
    }

    open fun visitAnnotationArgumentMapping(annotationArgumentMapping: FirAnnotationArgumentMapping) {
        visitElement(annotationArgumentMapping)
    }

    final override fun visitErrorAnnotationCall(errorAnnotationCall: FirErrorAnnotationCall, data: Nothing?) {
        visitErrorAnnotationCall(errorAnnotationCall)
    }

    open fun visitErrorAnnotationCall(errorAnnotationCall: FirErrorAnnotationCall) {
        visitElement(errorAnnotationCall)
    }

    final override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression, data: Nothing?) {
        visitComparisonExpression(comparisonExpression)
    }

    open fun visitComparisonExpression(comparisonExpression: FirComparisonExpression) {
        visitElement(comparisonExpression)
    }

    final override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Nothing?) {
        visitTypeOperatorCall(typeOperatorCall)
    }

    open fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall) {
        visitElement(typeOperatorCall)
    }

    final override fun visitAssignmentOperatorStatement(assignmentOperatorStatement: FirAssignmentOperatorStatement, data: Nothing?) {
        visitAssignmentOperatorStatement(assignmentOperatorStatement)
    }

    open fun visitAssignmentOperatorStatement(assignmentOperatorStatement: FirAssignmentOperatorStatement) {
        visitElement(assignmentOperatorStatement)
    }

    final override fun visitIncrementDecrementExpression(incrementDecrementExpression: FirIncrementDecrementExpression, data: Nothing?) {
        visitIncrementDecrementExpression(incrementDecrementExpression)
    }

    open fun visitIncrementDecrementExpression(incrementDecrementExpression: FirIncrementDecrementExpression) {
        visitElement(incrementDecrementExpression)
    }

    final override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: Nothing?) {
        visitEqualityOperatorCall(equalityOperatorCall)
    }

    open fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall) {
        visitElement(equalityOperatorCall)
    }

    final override fun visitWhenExpression(whenExpression: FirWhenExpression, data: Nothing?) {
        visitWhenExpression(whenExpression)
    }

    open fun visitWhenExpression(whenExpression: FirWhenExpression) {
        visitElement(whenExpression)
    }

    final override fun visitWhenBranch(whenBranch: FirWhenBranch, data: Nothing?) {
        visitWhenBranch(whenBranch)
    }

    open fun visitWhenBranch(whenBranch: FirWhenBranch) {
        visitElement(whenBranch)
    }

    final override fun visitContextReceiverArgumentListOwner(contextReceiverArgumentListOwner: FirContextReceiverArgumentListOwner, data: Nothing?) {
        visitContextReceiverArgumentListOwner(contextReceiverArgumentListOwner)
    }

    open fun visitContextReceiverArgumentListOwner(contextReceiverArgumentListOwner: FirContextReceiverArgumentListOwner) {
        visitElement(contextReceiverArgumentListOwner)
    }

    final override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: Nothing?) {
        visitCheckNotNullCall(checkNotNullCall)
    }

    open fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall) {
        visitElement(checkNotNullCall)
    }

    final override fun visitElvisExpression(elvisExpression: FirElvisExpression, data: Nothing?) {
        visitElvisExpression(elvisExpression)
    }

    open fun visitElvisExpression(elvisExpression: FirElvisExpression) {
        visitElement(elvisExpression)
    }

    final override fun visitArrayLiteral(arrayLiteral: FirArrayLiteral, data: Nothing?) {
        visitArrayLiteral(arrayLiteral)
    }

    open fun visitArrayLiteral(arrayLiteral: FirArrayLiteral) {
        visitElement(arrayLiteral)
    }

    final override fun visitAugmentedArraySetCall(augmentedArraySetCall: FirAugmentedArraySetCall, data: Nothing?) {
        visitAugmentedArraySetCall(augmentedArraySetCall)
    }

    open fun visitAugmentedArraySetCall(augmentedArraySetCall: FirAugmentedArraySetCall) {
        visitElement(augmentedArraySetCall)
    }

    final override fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression, data: Nothing?) {
        visitClassReferenceExpression(classReferenceExpression)
    }

    open fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression) {
        visitElement(classReferenceExpression)
    }

    final override fun visitErrorExpression(errorExpression: FirErrorExpression, data: Nothing?) {
        visitErrorExpression(errorExpression)
    }

    open fun visitErrorExpression(errorExpression: FirErrorExpression) {
        visitElement(errorExpression)
    }

    final override fun visitErrorFunction(errorFunction: FirErrorFunction, data: Nothing?) {
        visitErrorFunction(errorFunction)
    }

    open fun visitErrorFunction(errorFunction: FirErrorFunction) {
        visitElement(errorFunction)
    }

    final override fun visitErrorProperty(errorProperty: FirErrorProperty, data: Nothing?) {
        visitErrorProperty(errorProperty)
    }

    open fun visitErrorProperty(errorProperty: FirErrorProperty) {
        visitElement(errorProperty)
    }

    final override fun visitErrorPrimaryConstructor(errorPrimaryConstructor: FirErrorPrimaryConstructor, data: Nothing?) {
        visitErrorPrimaryConstructor(errorPrimaryConstructor)
    }

    open fun visitErrorPrimaryConstructor(errorPrimaryConstructor: FirErrorPrimaryConstructor) {
        visitElement(errorPrimaryConstructor)
    }

    final override fun visitDanglingModifierList(danglingModifierList: FirDanglingModifierList, data: Nothing?) {
        visitDanglingModifierList(danglingModifierList)
    }

    open fun visitDanglingModifierList(danglingModifierList: FirDanglingModifierList) {
        visitElement(danglingModifierList)
    }

    final override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: Nothing?) {
        visitQualifiedAccessExpression(qualifiedAccessExpression)
    }

    open fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
        visitElement(qualifiedAccessExpression)
    }

    final override fun visitQualifiedErrorAccessExpression(qualifiedErrorAccessExpression: FirQualifiedErrorAccessExpression, data: Nothing?) {
        visitQualifiedErrorAccessExpression(qualifiedErrorAccessExpression)
    }

    open fun visitQualifiedErrorAccessExpression(qualifiedErrorAccessExpression: FirQualifiedErrorAccessExpression) {
        visitElement(qualifiedErrorAccessExpression)
    }

    final override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: Nothing?) {
        visitPropertyAccessExpression(propertyAccessExpression)
    }

    open fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
        visitElement(propertyAccessExpression)
    }

    final override fun visitFunctionCall(functionCall: FirFunctionCall, data: Nothing?) {
        visitFunctionCall(functionCall)
    }

    open fun visitFunctionCall(functionCall: FirFunctionCall) {
        visitElement(functionCall)
    }

    final override fun visitIntegerLiteralOperatorCall(integerLiteralOperatorCall: FirIntegerLiteralOperatorCall, data: Nothing?) {
        visitIntegerLiteralOperatorCall(integerLiteralOperatorCall)
    }

    open fun visitIntegerLiteralOperatorCall(integerLiteralOperatorCall: FirIntegerLiteralOperatorCall) {
        visitElement(integerLiteralOperatorCall)
    }

    final override fun visitImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall, data: Nothing?) {
        visitImplicitInvokeCall(implicitInvokeCall)
    }

    open fun visitImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall) {
        visitElement(implicitInvokeCall)
    }

    final override fun visitDelegatedConstructorCall(delegatedConstructorCall: FirDelegatedConstructorCall, data: Nothing?) {
        visitDelegatedConstructorCall(delegatedConstructorCall)
    }

    open fun visitDelegatedConstructorCall(delegatedConstructorCall: FirDelegatedConstructorCall) {
        visitElement(delegatedConstructorCall)
    }

    final override fun visitMultiDelegatedConstructorCall(multiDelegatedConstructorCall: FirMultiDelegatedConstructorCall, data: Nothing?) {
        visitMultiDelegatedConstructorCall(multiDelegatedConstructorCall)
    }

    open fun visitMultiDelegatedConstructorCall(multiDelegatedConstructorCall: FirMultiDelegatedConstructorCall) {
        visitElement(multiDelegatedConstructorCall)
    }

    final override fun visitComponentCall(componentCall: FirComponentCall, data: Nothing?) {
        visitComponentCall(componentCall)
    }

    open fun visitComponentCall(componentCall: FirComponentCall) {
        visitElement(componentCall)
    }

    final override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: Nothing?) {
        visitCallableReferenceAccess(callableReferenceAccess)
    }

    open fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess) {
        visitElement(callableReferenceAccess)
    }

    final override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: Nothing?) {
        visitThisReceiverExpression(thisReceiverExpression)
    }

    open fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression) {
        visitElement(thisReceiverExpression)
    }

    final override fun visitInaccessibleReceiverExpression(inaccessibleReceiverExpression: FirInaccessibleReceiverExpression, data: Nothing?) {
        visitInaccessibleReceiverExpression(inaccessibleReceiverExpression)
    }

    open fun visitInaccessibleReceiverExpression(inaccessibleReceiverExpression: FirInaccessibleReceiverExpression) {
        visitElement(inaccessibleReceiverExpression)
    }

    final override fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression, data: Nothing?) {
        visitSmartCastExpression(smartCastExpression)
    }

    open fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression) {
        visitElement(smartCastExpression)
    }

    final override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: Nothing?) {
        visitSafeCallExpression(safeCallExpression)
    }

    open fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression) {
        visitElement(safeCallExpression)
    }

    final override fun visitCheckedSafeCallSubject(checkedSafeCallSubject: FirCheckedSafeCallSubject, data: Nothing?) {
        visitCheckedSafeCallSubject(checkedSafeCallSubject)
    }

    open fun visitCheckedSafeCallSubject(checkedSafeCallSubject: FirCheckedSafeCallSubject) {
        visitElement(checkedSafeCallSubject)
    }

    final override fun visitGetClassCall(getClassCall: FirGetClassCall, data: Nothing?) {
        visitGetClassCall(getClassCall)
    }

    open fun visitGetClassCall(getClassCall: FirGetClassCall) {
        visitElement(getClassCall)
    }

    final override fun visitWrappedExpression(wrappedExpression: FirWrappedExpression, data: Nothing?) {
        visitWrappedExpression(wrappedExpression)
    }

    open fun visitWrappedExpression(wrappedExpression: FirWrappedExpression) {
        visitElement(wrappedExpression)
    }

    final override fun visitWrappedArgumentExpression(wrappedArgumentExpression: FirWrappedArgumentExpression, data: Nothing?) {
        visitWrappedArgumentExpression(wrappedArgumentExpression)
    }

    open fun visitWrappedArgumentExpression(wrappedArgumentExpression: FirWrappedArgumentExpression) {
        visitElement(wrappedArgumentExpression)
    }

    final override fun visitLambdaArgumentExpression(lambdaArgumentExpression: FirLambdaArgumentExpression, data: Nothing?) {
        visitLambdaArgumentExpression(lambdaArgumentExpression)
    }

    open fun visitLambdaArgumentExpression(lambdaArgumentExpression: FirLambdaArgumentExpression) {
        visitElement(lambdaArgumentExpression)
    }

    final override fun visitSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression, data: Nothing?) {
        visitSpreadArgumentExpression(spreadArgumentExpression)
    }

    open fun visitSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression) {
        visitElement(spreadArgumentExpression)
    }

    final override fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: Nothing?) {
        visitNamedArgumentExpression(namedArgumentExpression)
    }

    open fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression) {
        visitElement(namedArgumentExpression)
    }

    final override fun visitVarargArgumentsExpression(varargArgumentsExpression: FirVarargArgumentsExpression, data: Nothing?) {
        visitVarargArgumentsExpression(varargArgumentsExpression)
    }

    open fun visitVarargArgumentsExpression(varargArgumentsExpression: FirVarargArgumentsExpression) {
        visitElement(varargArgumentsExpression)
    }

    final override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: Nothing?) {
        visitResolvedQualifier(resolvedQualifier)
    }

    open fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
        visitElement(resolvedQualifier)
    }

    final override fun visitErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier, data: Nothing?) {
        visitErrorResolvedQualifier(errorResolvedQualifier)
    }

    open fun visitErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier) {
        visitElement(errorResolvedQualifier)
    }

    final override fun visitResolvedReifiedParameterReference(resolvedReifiedParameterReference: FirResolvedReifiedParameterReference, data: Nothing?) {
        visitResolvedReifiedParameterReference(resolvedReifiedParameterReference)
    }

    open fun visitResolvedReifiedParameterReference(resolvedReifiedParameterReference: FirResolvedReifiedParameterReference) {
        visitElement(resolvedReifiedParameterReference)
    }

    final override fun visitReturnExpression(returnExpression: FirReturnExpression, data: Nothing?) {
        visitReturnExpression(returnExpression)
    }

    open fun visitReturnExpression(returnExpression: FirReturnExpression) {
        visitElement(returnExpression)
    }

    final override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: Nothing?) {
        visitStringConcatenationCall(stringConcatenationCall)
    }

    open fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall) {
        visitElement(stringConcatenationCall)
    }

    final override fun visitThrowExpression(throwExpression: FirThrowExpression, data: Nothing?) {
        visitThrowExpression(throwExpression)
    }

    open fun visitThrowExpression(throwExpression: FirThrowExpression) {
        visitElement(throwExpression)
    }

    final override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: Nothing?) {
        visitVariableAssignment(variableAssignment)
    }

    open fun visitVariableAssignment(variableAssignment: FirVariableAssignment) {
        visitElement(variableAssignment)
    }

    final override fun visitWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression, data: Nothing?) {
        visitWhenSubjectExpression(whenSubjectExpression)
    }

    open fun visitWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression) {
        visitElement(whenSubjectExpression)
    }

    final override fun visitDesugaredAssignmentValueReferenceExpression(desugaredAssignmentValueReferenceExpression: FirDesugaredAssignmentValueReferenceExpression, data: Nothing?) {
        visitDesugaredAssignmentValueReferenceExpression(desugaredAssignmentValueReferenceExpression)
    }

    open fun visitDesugaredAssignmentValueReferenceExpression(desugaredAssignmentValueReferenceExpression: FirDesugaredAssignmentValueReferenceExpression) {
        visitElement(desugaredAssignmentValueReferenceExpression)
    }

    final override fun visitWrappedDelegateExpression(wrappedDelegateExpression: FirWrappedDelegateExpression, data: Nothing?) {
        visitWrappedDelegateExpression(wrappedDelegateExpression)
    }

    open fun visitWrappedDelegateExpression(wrappedDelegateExpression: FirWrappedDelegateExpression) {
        visitElement(wrappedDelegateExpression)
    }

    final override fun visitEnumEntryDeserializedAccessExpression(enumEntryDeserializedAccessExpression: FirEnumEntryDeserializedAccessExpression, data: Nothing?) {
        visitEnumEntryDeserializedAccessExpression(enumEntryDeserializedAccessExpression)
    }

    open fun visitEnumEntryDeserializedAccessExpression(enumEntryDeserializedAccessExpression: FirEnumEntryDeserializedAccessExpression) {
        visitElement(enumEntryDeserializedAccessExpression)
    }

    final override fun visitNamedReference(namedReference: FirNamedReference, data: Nothing?) {
        visitNamedReference(namedReference)
    }

    open fun visitNamedReference(namedReference: FirNamedReference) {
        visitElement(namedReference)
    }

    final override fun visitNamedReferenceWithCandidateBase(namedReferenceWithCandidateBase: FirNamedReferenceWithCandidateBase, data: Nothing?) {
        visitNamedReferenceWithCandidateBase(namedReferenceWithCandidateBase)
    }

    open fun visitNamedReferenceWithCandidateBase(namedReferenceWithCandidateBase: FirNamedReferenceWithCandidateBase) {
        visitElement(namedReferenceWithCandidateBase)
    }

    final override fun visitErrorNamedReference(errorNamedReference: FirErrorNamedReference, data: Nothing?) {
        visitErrorNamedReference(errorNamedReference)
    }

    open fun visitErrorNamedReference(errorNamedReference: FirErrorNamedReference) {
        visitElement(errorNamedReference)
    }

    final override fun visitFromMissingDependenciesNamedReference(fromMissingDependenciesNamedReference: FirFromMissingDependenciesNamedReference, data: Nothing?) {
        visitFromMissingDependenciesNamedReference(fromMissingDependenciesNamedReference)
    }

    open fun visitFromMissingDependenciesNamedReference(fromMissingDependenciesNamedReference: FirFromMissingDependenciesNamedReference) {
        visitElement(fromMissingDependenciesNamedReference)
    }

    final override fun visitSuperReference(superReference: FirSuperReference, data: Nothing?) {
        visitSuperReference(superReference)
    }

    open fun visitSuperReference(superReference: FirSuperReference) {
        visitElement(superReference)
    }

    final override fun visitThisReference(thisReference: FirThisReference, data: Nothing?) {
        visitThisReference(thisReference)
    }

    open fun visitThisReference(thisReference: FirThisReference) {
        visitElement(thisReference)
    }

    final override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference, data: Nothing?) {
        visitControlFlowGraphReference(controlFlowGraphReference)
    }

    open fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference) {
        visitElement(controlFlowGraphReference)
    }

    final override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference, data: Nothing?) {
        visitResolvedNamedReference(resolvedNamedReference)
    }

    open fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference) {
        visitElement(resolvedNamedReference)
    }

    final override fun visitResolvedErrorReference(resolvedErrorReference: FirResolvedErrorReference, data: Nothing?) {
        visitResolvedErrorReference(resolvedErrorReference)
    }

    open fun visitResolvedErrorReference(resolvedErrorReference: FirResolvedErrorReference) {
        visitElement(resolvedErrorReference)
    }

    final override fun visitDelegateFieldReference(delegateFieldReference: FirDelegateFieldReference, data: Nothing?) {
        visitDelegateFieldReference(delegateFieldReference)
    }

    open fun visitDelegateFieldReference(delegateFieldReference: FirDelegateFieldReference) {
        visitElement(delegateFieldReference)
    }

    final override fun visitBackingFieldReference(backingFieldReference: FirBackingFieldReference, data: Nothing?) {
        visitBackingFieldReference(backingFieldReference)
    }

    open fun visitBackingFieldReference(backingFieldReference: FirBackingFieldReference) {
        visitElement(backingFieldReference)
    }

    final override fun visitResolvedCallableReference(resolvedCallableReference: FirResolvedCallableReference, data: Nothing?) {
        visitResolvedCallableReference(resolvedCallableReference)
    }

    open fun visitResolvedCallableReference(resolvedCallableReference: FirResolvedCallableReference) {
        visitElement(resolvedCallableReference)
    }

    final override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: Nothing?) {
        visitResolvedTypeRef(resolvedTypeRef)
    }

    open fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        visitElement(resolvedTypeRef)
    }

    final override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: Nothing?) {
        visitErrorTypeRef(errorTypeRef)
    }

    open fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef) {
        visitElement(errorTypeRef)
    }

    final override fun visitTypeRefWithNullability(typeRefWithNullability: FirTypeRefWithNullability, data: Nothing?) {
        visitTypeRefWithNullability(typeRefWithNullability)
    }

    open fun visitTypeRefWithNullability(typeRefWithNullability: FirTypeRefWithNullability) {
        visitElement(typeRefWithNullability)
    }

    final override fun visitUserTypeRef(userTypeRef: FirUserTypeRef, data: Nothing?) {
        visitUserTypeRef(userTypeRef)
    }

    open fun visitUserTypeRef(userTypeRef: FirUserTypeRef) {
        visitElement(userTypeRef)
    }

    final override fun visitDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef, data: Nothing?) {
        visitDynamicTypeRef(dynamicTypeRef)
    }

    open fun visitDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef) {
        visitElement(dynamicTypeRef)
    }

    final override fun visitFunctionTypeRef(functionTypeRef: FirFunctionTypeRef, data: Nothing?) {
        visitFunctionTypeRef(functionTypeRef)
    }

    open fun visitFunctionTypeRef(functionTypeRef: FirFunctionTypeRef) {
        visitElement(functionTypeRef)
    }

    final override fun visitIntersectionTypeRef(intersectionTypeRef: FirIntersectionTypeRef, data: Nothing?) {
        visitIntersectionTypeRef(intersectionTypeRef)
    }

    open fun visitIntersectionTypeRef(intersectionTypeRef: FirIntersectionTypeRef) {
        visitElement(intersectionTypeRef)
    }

    final override fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: Nothing?) {
        visitImplicitTypeRef(implicitTypeRef)
    }

    open fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef) {
        visitElement(implicitTypeRef)
    }

    final override fun visitContractElementDeclaration(contractElementDeclaration: FirContractElementDeclaration, data: Nothing?) {
        visitContractElementDeclaration(contractElementDeclaration)
    }

    open fun visitContractElementDeclaration(contractElementDeclaration: FirContractElementDeclaration) {
        visitElement(contractElementDeclaration)
    }

    final override fun visitEffectDeclaration(effectDeclaration: FirEffectDeclaration, data: Nothing?) {
        visitEffectDeclaration(effectDeclaration)
    }

    open fun visitEffectDeclaration(effectDeclaration: FirEffectDeclaration) {
        visitElement(effectDeclaration)
    }

    final override fun visitContractDescription(contractDescription: FirContractDescription, data: Nothing?) {
        visitContractDescription(contractDescription)
    }

    open fun visitContractDescription(contractDescription: FirContractDescription) {
        visitElement(contractDescription)
    }

    final override fun visitLegacyRawContractDescription(legacyRawContractDescription: FirLegacyRawContractDescription, data: Nothing?) {
        visitLegacyRawContractDescription(legacyRawContractDescription)
    }

    open fun visitLegacyRawContractDescription(legacyRawContractDescription: FirLegacyRawContractDescription) {
        visitElement(legacyRawContractDescription)
    }

    final override fun visitRawContractDescription(rawContractDescription: FirRawContractDescription, data: Nothing?) {
        visitRawContractDescription(rawContractDescription)
    }

    open fun visitRawContractDescription(rawContractDescription: FirRawContractDescription) {
        visitElement(rawContractDescription)
    }

    final override fun visitResolvedContractDescription(resolvedContractDescription: FirResolvedContractDescription, data: Nothing?) {
        visitResolvedContractDescription(resolvedContractDescription)
    }

    open fun visitResolvedContractDescription(resolvedContractDescription: FirResolvedContractDescription) {
        visitElement(resolvedContractDescription)
    }
}
