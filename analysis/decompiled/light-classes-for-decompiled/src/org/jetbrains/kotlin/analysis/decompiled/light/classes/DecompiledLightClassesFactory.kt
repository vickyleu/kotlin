/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiled.light.classes

import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

object DecompiledLightClassesFactory {
    private val checkInconsistency: Boolean
        get() = Registry.`is`(
            /* key = */ "kotlin.decompiled.light.classes.check.inconsistency",
            /* defaultValue = */ false,
        )

    fun getLightClassForDecompiledClassOrObject(decompiledClassOrObject: KtClassOrObject): KtLightClassForDecompiledDeclaration? {
        if (decompiledClassOrObject is KtEnumEntry) {
            return null
        }

        val containingKtFile = decompiledClassOrObject.containingFile as? KtClsFile ?: return null
        val rootLightClassForDecompiledFile = createLightClassForDecompiledKotlinFile(containingKtFile) ?: return null

        return findCorrespondingLightClass(decompiledClassOrObject, rootLightClassForDecompiledFile)
    }

    private fun findCorrespondingLightClass(
        decompiledClassOrObject: KtClassOrObject,
        rootLightClassForDecompiledFile: KtLightClassForDecompiledDeclaration
    ): KtLightClassForDecompiledDeclaration? {
        val relativeFqName = getClassRelativeName(decompiledClassOrObject) ?: return null
        val iterator = relativeFqName.pathSegments().iterator()
        val base = iterator.next()

        // In case class files have been obfuscated (i.e., SomeClass belongs to a.class file), just ignore them
        if (rootLightClassForDecompiledFile.name != base.asString()) return null

        var current: KtLightClassForDecompiledDeclaration = rootLightClassForDecompiledFile
        while (iterator.hasNext()) {
            val name = iterator.next()
            val innerClass = current.findInnerClassByName(name.asString(), false)
            current = when {
                innerClass != null -> innerClass as KtLightClassForDecompiledDeclaration
                checkInconsistency -> {
                    throw KotlinExceptionWithAttachments("Could not find corresponding inner/nested class")
                        .withAttachment("relativeFqName.txt", relativeFqName)
                        .withAttachment("decompiledClassOrObjectFqName.txt", decompiledClassOrObject.fqName)
                        .withAttachment("decompiledFileName.txt", decompiledClassOrObject.containingKtFile.virtualFile.name)
                        .withPsiAttachment("decompiledClassOrObject.txt", decompiledClassOrObject)
                        .withAttachment("fileClass.txt", decompiledClassOrObject.containingFile::class)
                        .withPsiAttachment("file.txt", decompiledClassOrObject.containingFile)
                        .withPsiAttachment("root.txt", rootLightClassForDecompiledFile)
                        .withAttachment("currentName.txt", current.name)
                        .withPsiAttachment("current.txt", current)
                        .withAttachment("innerClasses.txt", current.innerClasses.map { psiClass -> psiClass.name })
                        .withAttachment("innerName.txt", name.asString())
                }

                else -> return null
            }
        }

        return current
    }

    private fun getClassRelativeName(decompiledClassOrObject: KtClassOrObject): FqName? {
        val name = decompiledClassOrObject.nameAsName ?: return null
        val parent = PsiTreeUtil.getParentOfType(
            decompiledClassOrObject,
            KtClassOrObject::class.java,
            true
        )
        if (parent == null) {
            assert(decompiledClassOrObject.isTopLevel())
            return FqName.topLevel(name)
        }
        return getClassRelativeName(parent)?.child(name)
    }

    fun createLightClassForDecompiledKotlinFile(file: KtClsFile): KtLightClassForDecompiledDeclaration? {
        return createLightClassForDecompiledKotlinFile(file) { kotlinClsFile, javaClsClass, classOrObject ->
            KtLightClassForDecompiledDeclaration(javaClsClass, javaClsClass.parent, kotlinClsFile, classOrObject)
        }
    }

    private fun <T> createLightClassForDecompiledKotlinFile(
        file: KtClsFile,
        builder: (kotlinClsFile: KtClsFile, javaClsClass: PsiClass, classOrObject: KtClassOrObject?) -> T,
    ): T? {
        val javaClsClass = createClsJavaClassFromVirtualFile(clsFile = file) ?: return null
        val classOrObject = file.declarations.filterIsInstance<KtClassOrObject>().singleOrNull()
        return builder(file, javaClsClass, classOrObject)
    }

    fun createLightFacadeForDecompiledKotlinFile(
        facadeClassFqName: FqName,
        files: List<KtFile>,
    ): KtLightClassForFacade? {
        assert(files.all(KtFile::isCompiled))
        val file = files.firstOrNull { it.javaFileFacadeFqName == facadeClassFqName } as? KtClsFile
            ?: error("Can't find the representative decompiled file for $facadeClassFqName in ${files.map { it.name }}")

        return createLightClassForDecompiledKotlinFile(file) { kotlinClsFile, javaClsClass, classOrObject ->
            KtLightClassForDecompiledFacade(javaClsClass, javaClsClass.parent, kotlinClsFile, classOrObject, files)
        }
    }

    fun createClsJavaClassFromVirtualFile(clsFile: KtClsFile): ClsClassImpl? {
        return ClsJavaStubBuilderService.getOrBuild(clsFile)
    }
}