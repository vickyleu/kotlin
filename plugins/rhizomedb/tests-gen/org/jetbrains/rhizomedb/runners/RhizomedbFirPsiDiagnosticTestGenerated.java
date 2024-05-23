/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.runners;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.rhizomedb.TestGeneratorKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("plugins/rhizomedb/testData/firMembers")
@TestDataPath("$PROJECT_ROOT")
public class RhizomedbFirPsiDiagnosticTestGenerated extends AbstractRhizomedbFirPsiDiagnosticTest {
  @Test
  public void testAllFilesPresentInFirMembers() {
    KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("plugins/rhizomedb/testData/firMembers"), Pattern.compile("^(.+)\\.kt$"), null, true);
  }

  @Nested
  @TestMetadata("plugins/rhizomedb/testData/firMembers/attributes")
  @TestDataPath("$PROJECT_ROOT")
  public class Attributes {
    @Test
    public void testAllFilesPresentInAttributes() {
      KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("plugins/rhizomedb/testData/firMembers/attributes"), Pattern.compile("^(.+)\\.kt$"), null, true);
    }

    @Test
    @TestMetadata("attributeWithoutEntityTypeGeneration.kt")
    public void testAttributeWithoutEntityTypeGeneration() {
      runTest("plugins/rhizomedb/testData/firMembers/attributes/attributeWithoutEntityTypeGeneration.kt");
    }

    @Test
    @TestMetadata("companionNotEntityType.kt")
    public void testCompanionNotEntityType() {
      runTest("plugins/rhizomedb/testData/firMembers/attributes/companionNotEntityType.kt");
    }

    @Test
    @TestMetadata("manyWithoutAttribute.kt")
    public void testManyWithoutAttribute() {
      runTest("plugins/rhizomedb/testData/firMembers/attributes/manyWithoutAttribute.kt");
    }

    @Test
    @TestMetadata("severalAnnotations.kt")
    public void testSeveralAnnotations() {
      runTest("plugins/rhizomedb/testData/firMembers/attributes/severalAnnotations.kt");
    }

    @Nested
    @TestMetadata("plugins/rhizomedb/testData/firMembers/attributes/ref")
    @TestDataPath("$PROJECT_ROOT")
    public class Ref {
      @Test
      public void testAllFilesPresentInRef() {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("plugins/rhizomedb/testData/firMembers/attributes/ref"), Pattern.compile("^(.+)\\.kt$"), null, true);
      }

      @Test
      @TestMetadata("manyAttribute.kt")
      public void testManyAttribute() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/ref/manyAttribute.kt");
      }

      @Test
      @TestMetadata("manyAttributeNotASet.kt")
      public void testManyAttributeNotASet() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/ref/manyAttributeNotASet.kt");
      }

      @Test
      @TestMetadata("nonEntityProperty.kt")
      public void testNonEntityProperty() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/ref/nonEntityProperty.kt");
      }

      @Test
      @TestMetadata("optionalAttribute.kt")
      public void testOptionalAttribute() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/ref/optionalAttribute.kt");
      }

      @Test
      @TestMetadata("refFlags.kt")
      public void testRefFlags() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/ref/refFlags.kt");
      }

      @Test
      @TestMetadata("requiredAttribute.kt")
      public void testRequiredAttribute() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/ref/requiredAttribute.kt");
      }

      @Test
      @TestMetadata("wrongIndexingFlags.kt")
      public void testWrongIndexingFlags() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/ref/wrongIndexingFlags.kt");
      }
    }

    @Nested
    @TestMetadata("plugins/rhizomedb/testData/firMembers/attributes/transient")
    @TestDataPath("$PROJECT_ROOT")
    public class Transient {
      @Test
      public void testAllFilesPresentInTransient() {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("plugins/rhizomedb/testData/firMembers/attributes/transient"), Pattern.compile("^(.+)\\.kt$"), null, true);
      }

      @Test
      @TestMetadata("entityProperty.kt")
      public void testEntityProperty() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/transient/entityProperty.kt");
      }

      @Test
      @TestMetadata("indexingFlags.kt")
      public void testIndexingFlags() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/transient/indexingFlags.kt");
      }

      @Test
      @TestMetadata("manyAttribute.kt")
      public void testManyAttribute() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/transient/manyAttribute.kt");
      }

      @Test
      @TestMetadata("manyAttributeNotASet.kt")
      public void testManyAttributeNotASet() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/transient/manyAttributeNotASet.kt");
      }

      @Test
      @TestMetadata("nonManySet.kt")
      public void testNonManySet() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/transient/nonManySet.kt");
      }

      @Test
      @TestMetadata("optionalAttribute.kt")
      public void testOptionalAttribute() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/transient/optionalAttribute.kt");
      }

      @Test
      @TestMetadata("requiredAttribute.kt")
      public void testRequiredAttribute() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/transient/requiredAttribute.kt");
      }

      @Test
      @TestMetadata("wrongIndexingFlags.kt")
      public void testWrongIndexingFlags() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/transient/wrongIndexingFlags.kt");
      }
    }

    @Nested
    @TestMetadata("plugins/rhizomedb/testData/firMembers/attributes/value")
    @TestDataPath("$PROJECT_ROOT")
    public class Value {
      @Test
      public void testAllFilesPresentInValue() {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("plugins/rhizomedb/testData/firMembers/attributes/value"), Pattern.compile("^(.+)\\.kt$"), null, true);
      }

      @Test
      @TestMetadata("entityProperty.kt")
      public void testEntityProperty() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/value/entityProperty.kt");
      }

      @Test
      @TestMetadata("indexingFlags.kt")
      public void testIndexingFlags() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/value/indexingFlags.kt");
      }

      @Test
      @TestMetadata("manyAttribute.kt")
      public void testManyAttribute() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/value/manyAttribute.kt");
      }

      @Test
      @TestMetadata("manyAttributeNotASet.kt")
      public void testManyAttributeNotASet() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/value/manyAttributeNotASet.kt");
      }

      @Test
      @TestMetadata("nonManySet.kt")
      public void testNonManySet() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/value/nonManySet.kt");
      }

      @Test
      @TestMetadata("optionalAttribute.kt")
      public void testOptionalAttribute() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/value/optionalAttribute.kt");
      }

      @Test
      @TestMetadata("requiredAttribute.kt")
      public void testRequiredAttribute() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/value/requiredAttribute.kt");
      }

      @Test
      @TestMetadata("wrongIndexingFlags.kt")
      public void testWrongIndexingFlags() {
        runTest("plugins/rhizomedb/testData/firMembers/attributes/value/wrongIndexingFlags.kt");
      }
    }
  }

  @Nested
  @TestMetadata("plugins/rhizomedb/testData/firMembers/entityType")
  @TestDataPath("$PROJECT_ROOT")
  public class EntityType {
    @Test
    public void testAllFilesPresentInEntityType() {
      KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("plugins/rhizomedb/testData/firMembers/entityType"), Pattern.compile("^(.+)\\.kt$"), null, true);
    }

    @Nested
    @TestMetadata("plugins/rhizomedb/testData/firMembers/entityType/withCompanion")
    @TestDataPath("$PROJECT_ROOT")
    public class WithCompanion {
      @Test
      @TestMetadata("abstractClass.kt")
      public void testAbstractClass() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withCompanion/abstractClass.kt");
      }

      @Test
      public void testAllFilesPresentInWithCompanion() {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("plugins/rhizomedb/testData/firMembers/entityType/withCompanion"), Pattern.compile("^(.+)\\.kt$"), null, true);
      }

      @Test
      @TestMetadata("companionExtendsClass.kt")
      public void testCompanionExtendsClass() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withCompanion/companionExtendsClass.kt");
      }

      @Test
      @TestMetadata("companionExtendsEntityType.kt")
      public void testCompanionExtendsEntityType() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withCompanion/companionExtendsEntityType.kt");
      }

      @Test
      @TestMetadata("companionIsEntityTypeEventually.kt")
      public void testCompanionIsEntityTypeEventually() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withCompanion/companionIsEntityTypeEventually.kt");
      }

      @Test
      @TestMetadata("enumClass.kt")
      public void testEnumClass() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withCompanion/enumClass.kt");
      }

      @Test
      @TestMetadata("generate.kt")
      public void testGenerate() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withCompanion/generate.kt");
      }

      @Test
      @TestMetadata("generateWithAbstractMixin.kt")
      public void testGenerateWithAbstractMixin() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withCompanion/generateWithAbstractMixin.kt");
      }

      @Test
      @TestMetadata("generateWithMixin.kt")
      public void testGenerateWithMixin() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withCompanion/generateWithMixin.kt");
      }

      @Test
      @TestMetadata("generateWithNonMixin.kt")
      public void testGenerateWithNonMixin() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withCompanion/generateWithNonMixin.kt");
      }

      @Test
      @TestMetadata("interface.kt")
      public void testInterface() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withCompanion/interface.kt");
      }

      @Test
      @TestMetadata("isEntityEventually.kt")
      public void testIsEntityEventually() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withCompanion/isEntityEventually.kt");
      }

      @Test
      @TestMetadata("noAnnotation.kt")
      public void testNoAnnotation() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withCompanion/noAnnotation.kt");
      }

      @Test
      @TestMetadata("noEidConstructor.kt")
      public void testNoEidConstructor() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withCompanion/noEidConstructor.kt");
      }

      @Test
      @TestMetadata("notEntity.kt")
      public void testNotEntity() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withCompanion/notEntity.kt");
      }

      @Test
      @TestMetadata("sealedClass.kt")
      public void testSealedClass() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withCompanion/sealedClass.kt");
      }

      @Test
      @TestMetadata("sealedInterface.kt")
      public void testSealedInterface() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withCompanion/sealedInterface.kt");
      }
    }

    @Nested
    @TestMetadata("plugins/rhizomedb/testData/firMembers/entityType/withoutCompanion")
    @TestDataPath("$PROJECT_ROOT")
    public class WithoutCompanion {
      @Test
      @TestMetadata("abstractClass.kt")
      public void testAbstractClass() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withoutCompanion/abstractClass.kt");
      }

      @Test
      public void testAllFilesPresentInWithoutCompanion() {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("plugins/rhizomedb/testData/firMembers/entityType/withoutCompanion"), Pattern.compile("^(.+)\\.kt$"), null, true);
      }

      @Test
      @TestMetadata("enumClass.kt")
      public void testEnumClass() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withoutCompanion/enumClass.kt");
      }

      @Test
      @TestMetadata("generate.kt")
      public void testGenerate() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withoutCompanion/generate.kt");
      }

      @Test
      @TestMetadata("generateWithAbstractMixin.kt")
      public void testGenerateWithAbstractMixin() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withoutCompanion/generateWithAbstractMixin.kt");
      }

      @Test
      @TestMetadata("generateWithMixin.kt")
      public void testGenerateWithMixin() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withoutCompanion/generateWithMixin.kt");
      }

      @Test
      @TestMetadata("generateWithNonMixin.kt")
      public void testGenerateWithNonMixin() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withoutCompanion/generateWithNonMixin.kt");
      }

      @Test
      @TestMetadata("interface.kt")
      public void testInterface() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withoutCompanion/interface.kt");
      }

      @Test
      @TestMetadata("isEntityEventually.kt")
      public void testIsEntityEventually() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withoutCompanion/isEntityEventually.kt");
      }

      @Test
      @TestMetadata("noAnnotation.kt")
      public void testNoAnnotation() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withoutCompanion/noAnnotation.kt");
      }

      @Test
      @TestMetadata("noEidConstructor.kt")
      public void testNoEidConstructor() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withoutCompanion/noEidConstructor.kt");
      }

      @Test
      @TestMetadata("notEntity.kt")
      public void testNotEntity() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withoutCompanion/notEntity.kt");
      }

      @Test
      @TestMetadata("object.kt")
      public void testObject() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withoutCompanion/object.kt");
      }

      @Test
      @TestMetadata("sealedClass.kt")
      public void testSealedClass() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withoutCompanion/sealedClass.kt");
      }

      @Test
      @TestMetadata("sealedInterface.kt")
      public void testSealedInterface() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withoutCompanion/sealedInterface.kt");
      }

      @Test
      @TestMetadata("typealias.kt")
      public void testTypealias() {
        runTest("plugins/rhizomedb/testData/firMembers/entityType/withoutCompanion/typealias.kt");
      }
    }
  }
}
