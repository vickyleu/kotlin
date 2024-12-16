/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiled.light.classes

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ClassFileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl
import com.intellij.util.cls.ClsFormatException
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.asJava.builder.ClsWrapperStubPsiFactory
import org.jetbrains.kotlin.psi.KtClassOrObject
import java.io.IOException

internal class ClsJavaStubByVirtualFileCache {
    private class CachedJavaStub(val modificationStamp: Long, val javaClassStub: ClsClassImpl?)

    private val cache = ContainerUtil.createConcurrentWeakKeySoftValueMap<KtClsFile, CachedJavaStub>()

    private fun get(clsFile: KtClsFile): ClsClassImpl? {
        val cached = cache[clsFile]
        val actualModificationStamp = clsFile.modificationStamp
        if (cached != null) {
            if (cached.modificationStamp == actualModificationStamp) {
                return cached.javaClassStub
            } else {
                // This call is required to be able to use [putIfAbsent] below
                cache.remove(clsFile, cached)
            }
        }

        val virtualFile = clsFile.virtualFile ?: return null
        val javaClassStub = createJavaClassStub(clsFile, virtualFile)
        val newValue = CachedJavaStub(actualModificationStamp, javaClassStub)

        // [putIfAbsent] to prefer already cached value.
        // It should be fine to return the new value always, but let's leave it for consistency
        val result = cache.putIfAbsent(clsFile, newValue) ?: newValue
        return result.javaClassStub
    }

    private fun createJavaClassStub(clsFile: KtClsFile, virtualFile: VirtualFile): ClsClassImpl? {
        val javaFileStub = createStub(virtualFile) as? PsiJavaFileStubImpl ?: return null
        javaFileStub.psiFactory = ClsWrapperStubPsiFactory.INSTANCE

        val manager = PsiManager.getInstance(clsFile.project)
        val fakeFile = object : ClsFileImpl(ClassFileViewProvider(manager, virtualFile)) {
            override fun getNavigationElement(): PsiElement {
                val correspondingClassOrObject = clsFile.declarations.filterIsInstance<KtClassOrObject>().singleOrNull()
                if (correspondingClassOrObject != null) {
                    return correspondingClassOrObject.navigationElement.containingFile
                }

                return super.getNavigationElement()
            }

            override fun getStub() = javaFileStub

            override fun getMirror() = clsFile

            override fun isPhysical() = false
        }

        javaFileStub.psi = fakeFile
        return fakeFile.classes.single() as ClsClassImpl
    }

    private fun createStub(file: VirtualFile): PsiJavaFileStub? {
        if (file.extension != JavaClassFileType.INSTANCE.defaultExtension && file.fileType !== JavaClassFileType.INSTANCE) return null

        try {
            return ClsFileImpl.buildFileStub(file, file.contentsToByteArray(false))
        } catch (e: ClsFormatException) {
            LOG.warn("Failed to build java cls class for " + file.canonicalPath!!, e)
        } catch (e: IOException) {
            LOG.warn("Failed to build java cls class for " + file.canonicalPath!!, e)
        }

        return null
    }

    companion object {
        private val LOG = Logger.getInstance(ClsJavaStubByVirtualFileCache::class.java)

        fun getOrBuild(clsFile: KtClsFile): ClsClassImpl? {
            return clsFile.project.service<ClsJavaStubByVirtualFileCache>().get(clsFile)
        }
    }
}