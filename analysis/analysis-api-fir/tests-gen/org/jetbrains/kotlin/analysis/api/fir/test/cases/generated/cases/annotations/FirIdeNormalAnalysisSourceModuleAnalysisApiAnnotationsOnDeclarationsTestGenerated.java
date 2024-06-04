/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.cases.generated.cases.annotations;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analysis.api.fir.test.configurators.AnalysisApiFirTestConfiguratorFactory;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisSessionMode;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiMode;
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations.AbstractAnalysisApiAnnotationsOnDeclarationsTest;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.analysis.api.GenerateAnalysisApiTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("analysis/analysis-api/testData/annotations/annotationsOnDeclaration")
@TestDataPath("$PROJECT_ROOT")
public class FirIdeNormalAnalysisSourceModuleAnalysisApiAnnotationsOnDeclarationsTestGenerated extends AbstractAnalysisApiAnnotationsOnDeclarationsTest {
  @NotNull
  @Override
  public AnalysisApiTestConfigurator getConfigurator() {
    return AnalysisApiFirTestConfiguratorFactory.INSTANCE.createConfigurator(
      new AnalysisApiTestConfiguratorFactoryData(
        FrontendKind.Fir,
        TestModuleKind.Source,
        AnalysisSessionMode.Normal,
        AnalysisApiMode.Ide
      )
    );
  }

  @Test
  public void testAllFilesPresentInAnnotationsOnDeclaration() {
    KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/annotations/annotationsOnDeclaration"), Pattern.compile("^(.+)\\.kt$"), null, true);
  }

  @Test
  @TestMetadata("deprecated.kt")
  public void testDeprecated() {
    runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/deprecated.kt");
  }

  @Nested
  @TestMetadata("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct")
  @TestDataPath("$PROJECT_ROOT")
  public class Direct {
    @Test
    @TestMetadata("aliasedThrowsOnFunction.kt")
    public void testAliasedThrowsOnFunction() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct/aliasedThrowsOnFunction.kt");
    }

    @Test
    public void testAllFilesPresentInDirect() {
      KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct"), Pattern.compile("^(.+)\\.kt$"), null, true);
    }

    @Test
    @TestMetadata("annotatedType.kt")
    public void testAnnotatedType() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct/annotatedType.kt");
    }

    @Test
    @TestMetadata("arrayType.kt")
    public void testArrayType() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct/arrayType.kt");
    }

    @Test
    @TestMetadata("arrayType2.kt")
    public void testArrayType2() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct/arrayType2.kt");
    }

    @Test
    @TestMetadata("emptyJavaSpreadParameter.kt")
    public void testEmptyJavaSpreadParameter() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct/emptyJavaSpreadParameter.kt");
    }

    @Test
    @TestMetadata("listType.kt")
    public void testListType() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct/listType.kt");
    }

    @Test
    @TestMetadata("nestedAnnotation.kt")
    public void testNestedAnnotation() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct/nestedAnnotation.kt");
    }

    @Test
    @TestMetadata("onClass.kt")
    public void testOnClass() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct/onClass.kt");
    }

    @Test
    @TestMetadata("onFunction.kt")
    public void testOnFunction() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct/onFunction.kt");
    }

    @Test
    @TestMetadata("onFunction_unresolvedClassReference.kt")
    public void testOnFunction_unresolvedClassReference() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct/onFunction_unresolvedClassReference.kt");
    }

    @Test
    @TestMetadata("onLocalFunction.kt")
    public void testOnLocalFunction() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct/onLocalFunction.kt");
    }

    @Test
    @TestMetadata("onProperty.kt")
    public void testOnProperty() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct/onProperty.kt");
    }

    @Test
    @TestMetadata("onProperty_javaAnnotation_targets.kt")
    public void testOnProperty_javaAnnotation_targets() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct/onProperty_javaAnnotation_targets.kt");
    }

    @Test
    @TestMetadata("onTypeAlias.kt")
    public void testOnTypeAlias() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct/onTypeAlias.kt");
    }

    @Test
    @TestMetadata("primitiveArrayType.kt")
    public void testPrimitiveArrayType() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct/primitiveArrayType.kt");
    }

    @Test
    @TestMetadata("unsignedParameter.kt")
    public void testUnsignedParameter() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct/unsignedParameter.kt");
    }

    @Test
    @TestMetadata("varargComplexParameter.kt")
    public void testVarargComplexParameter() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct/varargComplexParameter.kt");
    }

    @Test
    @TestMetadata("varargNamedParameter.kt")
    public void testVarargNamedParameter() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct/varargNamedParameter.kt");
    }

    @Test
    @TestMetadata("varargParameter.kt")
    public void testVarargParameter() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct/varargParameter.kt");
    }

    @Test
    @TestMetadata("varargSpreadParameter.kt")
    public void testVarargSpreadParameter() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/direct/varargSpreadParameter.kt");
    }
  }

  @Nested
  @TestMetadata("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/useSite")
  @TestDataPath("$PROJECT_ROOT")
  public class UseSite {
    @Test
    public void testAllFilesPresentInUseSite() {
      KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/useSite"), Pattern.compile("^(.+)\\.kt$"), null, true);
    }

    @Test
    @TestMetadata("onGetter.kt")
    public void testOnGetter() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/useSite/onGetter.kt");
    }

    @Test
    @TestMetadata("onParam.kt")
    public void testOnParam() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/useSite/onParam.kt");
    }

    @Test
    @TestMetadata("onProperty.kt")
    public void testOnProperty() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/useSite/onProperty.kt");
    }

    @Test
    @TestMetadata("onSetter.kt")
    public void testOnSetter() {
      runTest("analysis/analysis-api/testData/annotations/annotationsOnDeclaration/useSite/onSetter.kt");
    }
  }
}
