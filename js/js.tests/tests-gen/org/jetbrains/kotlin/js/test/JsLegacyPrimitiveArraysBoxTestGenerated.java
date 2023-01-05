/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.GenerateJsTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/codegen/box/arrays")
@TestDataPath("$PROJECT_ROOT")
public class JsLegacyPrimitiveArraysBoxTestGenerated extends AbstractJsLegacyPrimitiveArraysBoxTest {
    @Test
    public void testAllFilesPresentInArrays() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/codegen/box/arrays"), Pattern.compile("^(.+)\\.kt$"), null, TargetBackend.JS, true);
    }

    @Test
    @TestMetadata("arrayConstructorWithNonInlineLambda.kt")
    public void testArrayConstructorWithNonInlineLambda() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/arrayConstructorWithNonInlineLambda.kt");
    }

    @Test
    @TestMetadata("arrayConstructorsSimple.kt")
    public void testArrayConstructorsSimple() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/arrayConstructorsSimple.kt");
    }

    @Test
    @TestMetadata("arrayGetAssignMultiIndex.kt")
    public void testArrayGetAssignMultiIndex() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/arrayGetAssignMultiIndex.kt");
    }

    @Test
    @TestMetadata("arrayGetMultiIndex.kt")
    public void testArrayGetMultiIndex() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/arrayGetMultiIndex.kt");
    }

    @Test
    @TestMetadata("arrayInstanceOf.kt")
    public void testArrayInstanceOf() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/arrayInstanceOf.kt");
    }

    @Test
    @TestMetadata("arrayPlusAssign.kt")
    public void testArrayPlusAssign() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/arrayPlusAssign.kt");
    }

    @Test
    @TestMetadata("arraysAreCloneable.kt")
    public void testArraysAreCloneable() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/arraysAreCloneable.kt");
    }

    @Test
    @TestMetadata("cloneArray.kt")
    public void testCloneArray() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/cloneArray.kt");
    }

    @Test
    @TestMetadata("clonePrimitiveArrays.kt")
    public void testClonePrimitiveArrays() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/clonePrimitiveArrays.kt");
    }

    @Test
    @TestMetadata("collectionAssignGetMultiIndex.kt")
    public void testCollectionAssignGetMultiIndex() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/collectionAssignGetMultiIndex.kt");
    }

    @Test
    @TestMetadata("collectionGetMultiIndex.kt")
    public void testCollectionGetMultiIndex() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/collectionGetMultiIndex.kt");
    }

    @Test
    @TestMetadata("constantArrayOfAny.kt")
    public void testConstantArrayOfAny() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/constantArrayOfAny.kt");
    }

    @Test
    @TestMetadata("forEachBooleanArray.kt")
    public void testForEachBooleanArray() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/forEachBooleanArray.kt");
    }

    @Test
    @TestMetadata("forEachByteArray.kt")
    public void testForEachByteArray() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/forEachByteArray.kt");
    }

    @Test
    @TestMetadata("forEachCharArray.kt")
    public void testForEachCharArray() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/forEachCharArray.kt");
    }

    @Test
    @TestMetadata("forEachDoubleArray.kt")
    public void testForEachDoubleArray() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/forEachDoubleArray.kt");
    }

    @Test
    @TestMetadata("forEachFloatArray.kt")
    public void testForEachFloatArray() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/forEachFloatArray.kt");
    }

    @Test
    @TestMetadata("forEachIntArray.kt")
    public void testForEachIntArray() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/forEachIntArray.kt");
    }

    @Test
    @TestMetadata("forEachLongArray.kt")
    public void testForEachLongArray() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/forEachLongArray.kt");
    }

    @Test
    @TestMetadata("forEachShortArray.kt")
    public void testForEachShortArray() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/forEachShortArray.kt");
    }

    @Test
    @TestMetadata("genericArrayInObjectLiteralConstructor.kt")
    public void testGenericArrayInObjectLiteralConstructor() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/genericArrayInObjectLiteralConstructor.kt");
    }

    @Test
    @TestMetadata("hashMap.kt")
    public void testHashMap() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/hashMap.kt");
    }

    @Test
    @TestMetadata("inProjectionAsParameter.kt")
    public void testInProjectionAsParameter() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/inProjectionAsParameter.kt");
    }

    @Test
    @TestMetadata("inProjectionOfArray.kt")
    public void testInProjectionOfArray() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/inProjectionOfArray.kt");
    }

    @Test
    @TestMetadata("inProjectionOfList.kt")
    public void testInProjectionOfList() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/inProjectionOfList.kt");
    }

    @Test
    @TestMetadata("indices.kt")
    public void testIndices() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/indices.kt");
    }

    @Test
    @TestMetadata("indicesChar.kt")
    public void testIndicesChar() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/indicesChar.kt");
    }

    @Test
    @TestMetadata("inlineInitializer.kt")
    public void testInlineInitializer() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/inlineInitializer.kt");
    }

    @Test
    @TestMetadata("iterator.kt")
    public void testIterator() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/iterator.kt");
    }

    @Test
    @TestMetadata("iteratorBooleanArray.kt")
    public void testIteratorBooleanArray() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/iteratorBooleanArray.kt");
    }

    @Test
    @TestMetadata("iteratorByteArray.kt")
    public void testIteratorByteArray() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/iteratorByteArray.kt");
    }

    @Test
    @TestMetadata("iteratorByteArrayNextByte.kt")
    public void testIteratorByteArrayNextByte() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/iteratorByteArrayNextByte.kt");
    }

    @Test
    @TestMetadata("iteratorCharArray.kt")
    public void testIteratorCharArray() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/iteratorCharArray.kt");
    }

    @Test
    @TestMetadata("iteratorDoubleArray.kt")
    public void testIteratorDoubleArray() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/iteratorDoubleArray.kt");
    }

    @Test
    @TestMetadata("iteratorFloatArray.kt")
    public void testIteratorFloatArray() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/iteratorFloatArray.kt");
    }

    @Test
    @TestMetadata("iteratorIntArray.kt")
    public void testIteratorIntArray() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/iteratorIntArray.kt");
    }

    @Test
    @TestMetadata("iteratorLongArray.kt")
    public void testIteratorLongArray() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/iteratorLongArray.kt");
    }

    @Test
    @TestMetadata("iteratorLongArrayNextLong.kt")
    public void testIteratorLongArrayNextLong() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/iteratorLongArrayNextLong.kt");
    }

    @Test
    @TestMetadata("iteratorShortArray.kt")
    public void testIteratorShortArray() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/iteratorShortArray.kt");
    }

    @Test
    @TestMetadata("kt1291.kt")
    public void testKt1291() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/kt1291.kt");
    }

    @Test
    @TestMetadata("kt238.kt")
    public void testKt238() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/kt238.kt");
    }

    @Test
    @TestMetadata("kt2997.kt")
    public void testKt2997() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/kt2997.kt");
    }

    @Test
    @TestMetadata("kt33.kt")
    public void testKt33() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/kt33.kt");
    }

    @Test
    @TestMetadata("kt34291_16dimensions.kt")
    public void testKt34291_16dimensions() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/kt34291_16dimensions.kt");
    }

    @Test
    @TestMetadata("kt3771.kt")
    public void testKt3771() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/kt3771.kt");
    }

    @Test
    @TestMetadata("kt4118.kt")
    public void testKt4118() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/kt4118.kt");
    }

    @Test
    @TestMetadata("kt42932.kt")
    public void testKt42932() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/kt42932.kt");
    }

    @Test
    @TestMetadata("kt4348.kt")
    public void testKt4348() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/kt4348.kt");
    }

    @Test
    @TestMetadata("kt4357.kt")
    public void testKt4357() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/kt4357.kt");
    }

    @Test
    @TestMetadata("kt47483.kt")
    public void testKt47483() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/kt47483.kt");
    }

    @Test
    @TestMetadata("kt503.kt")
    public void testKt503() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/kt503.kt");
    }

    @Test
    @TestMetadata("kt594.kt")
    public void testKt594() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/kt594.kt");
    }

    @Test
    @TestMetadata("kt7009.kt")
    public void testKt7009() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/kt7009.kt");
    }

    @Test
    @TestMetadata("kt7288.kt")
    public void testKt7288() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/kt7288.kt");
    }

    @Test
    @TestMetadata("kt7338.kt")
    public void testKt7338() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/kt7338.kt");
    }

    @Test
    @TestMetadata("kt779.kt")
    public void testKt779() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/kt779.kt");
    }

    @Test
    @TestMetadata("kt945.kt")
    public void testKt945() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/kt945.kt");
    }

    @Test
    @TestMetadata("kt950.kt")
    public void testKt950() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/kt950.kt");
    }

    @Test
    @TestMetadata("longAsIndex.kt")
    public void testLongAsIndex() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/longAsIndex.kt");
    }

    @Test
    @TestMetadata("multiArrayConstructors.kt")
    public void testMultiArrayConstructors() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/multiArrayConstructors.kt");
    }

    @Test
    @TestMetadata("nonLocalReturnArrayConstructor.kt")
    public void testNonLocalReturnArrayConstructor() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/nonLocalReturnArrayConstructor.kt");
    }

    @Test
    @TestMetadata("nonNullArray.kt")
    public void testNonNullArray() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/nonNullArray.kt");
    }

    @Test
    @TestMetadata("primitiveArrays.kt")
    public void testPrimitiveArrays() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/primitiveArrays.kt");
    }

    @Test
    @TestMetadata("stdlib.kt")
    public void testStdlib() throws Exception {
        runTest("compiler/testData/codegen/box/arrays/stdlib.kt");
    }

    @Nested
    @TestMetadata("compiler/testData/codegen/box/arrays/arraysOfInlineClass")
    @TestDataPath("$PROJECT_ROOT")
    public class ArraysOfInlineClass {
        @Test
        @TestMetadata("accessArrayOfInlineClass.kt")
        public void testAccessArrayOfInlineClass() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/arraysOfInlineClass/accessArrayOfInlineClass.kt");
        }

        @Test
        @TestMetadata("accessArrayOfUnsigned.kt")
        public void testAccessArrayOfUnsigned() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/arraysOfInlineClass/accessArrayOfUnsigned.kt");
        }

        @Test
        public void testAllFilesPresentInArraysOfInlineClass() throws Exception {
            KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/codegen/box/arrays/arraysOfInlineClass"), Pattern.compile("^(.+)\\.kt$"), null, TargetBackend.JS, true);
        }

        @Test
        @TestMetadata("arrayOfInlineClassOfArrayOfInlineClass.kt")
        public void testArrayOfInlineClassOfArrayOfInlineClass() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/arraysOfInlineClass/arrayOfInlineClassOfArrayOfInlineClass.kt");
        }
    }

    @Nested
    @TestMetadata("compiler/testData/codegen/box/arrays/forInReversed")
    @TestDataPath("$PROJECT_ROOT")
    public class ForInReversed {
        @Test
        public void testAllFilesPresentInForInReversed() throws Exception {
            KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/codegen/box/arrays/forInReversed"), Pattern.compile("^(.+)\\.kt$"), null, TargetBackend.JS, true);
        }

        @Test
        @TestMetadata("reversedArrayOriginalUpdatedInLoopBody.kt")
        public void testReversedArrayOriginalUpdatedInLoopBody() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/forInReversed/reversedArrayOriginalUpdatedInLoopBody.kt");
        }

        @Test
        @TestMetadata("reversedArrayReversedArrayOriginalUpdatedInLoopBody.kt")
        public void testReversedArrayReversedArrayOriginalUpdatedInLoopBody() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/forInReversed/reversedArrayReversedArrayOriginalUpdatedInLoopBody.kt");
        }

        @Test
        @TestMetadata("reversedOriginalUpdatedInLoopBody.kt")
        public void testReversedOriginalUpdatedInLoopBody() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/forInReversed/reversedOriginalUpdatedInLoopBody.kt");
        }

        @Test
        @TestMetadata("reversedReversedOriginalUpdatedInLoopBody.kt")
        public void testReversedReversedOriginalUpdatedInLoopBody() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/forInReversed/reversedReversedOriginalUpdatedInLoopBody.kt");
        }
    }

    @Nested
    @TestMetadata("compiler/testData/codegen/box/arrays/forInUnsignedArray")
    @TestDataPath("$PROJECT_ROOT")
    public class ForInUnsignedArray {
        @Test
        public void testAllFilesPresentInForInUnsignedArray() throws Exception {
            KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/codegen/box/arrays/forInUnsignedArray"), Pattern.compile("^(.+)\\.kt$"), null, TargetBackend.JS, true);
        }

        @Test
        @TestMetadata("forInUnsignedArray.kt")
        public void testForInUnsignedArray() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/forInUnsignedArray/forInUnsignedArray.kt");
        }

        @Test
        @TestMetadata("forInUnsignedArrayIndices.kt")
        public void testForInUnsignedArrayIndices() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/forInUnsignedArray/forInUnsignedArrayIndices.kt");
        }

        @Test
        @TestMetadata("forInUnsignedArrayIndicesReversed.kt")
        public void testForInUnsignedArrayIndicesReversed() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/forInUnsignedArray/forInUnsignedArrayIndicesReversed.kt");
        }

        @Test
        @TestMetadata("forInUnsignedArrayReversed.kt")
        public void testForInUnsignedArrayReversed() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/forInUnsignedArray/forInUnsignedArrayReversed.kt");
        }

        @Test
        @TestMetadata("forInUnsignedArrayWithIndex.kt")
        public void testForInUnsignedArrayWithIndex() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/forInUnsignedArray/forInUnsignedArrayWithIndex.kt");
        }

        @Test
        @TestMetadata("forInUnsignedArrayWithIndexNoElementVar.kt")
        public void testForInUnsignedArrayWithIndexNoElementVar() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/forInUnsignedArray/forInUnsignedArrayWithIndexNoElementVar.kt");
        }

        @Test
        @TestMetadata("forInUnsignedArrayWithIndexNoIndexVar.kt")
        public void testForInUnsignedArrayWithIndexNoIndexVar() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/forInUnsignedArray/forInUnsignedArrayWithIndexNoIndexVar.kt");
        }

        @Test
        @TestMetadata("forInUnsignedArrayWithIndexReversed.kt")
        public void testForInUnsignedArrayWithIndexReversed() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/forInUnsignedArray/forInUnsignedArrayWithIndexReversed.kt");
        }
    }

    @Nested
    @TestMetadata("compiler/testData/codegen/box/arrays/multiDecl")
    @TestDataPath("$PROJECT_ROOT")
    public class MultiDecl {
        @Test
        public void testAllFilesPresentInMultiDecl() throws Exception {
            KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/codegen/box/arrays/multiDecl"), Pattern.compile("^(.+)\\.kt$"), null, TargetBackend.JS, true);
        }

        @Test
        @TestMetadata("kt15560.kt")
        public void testKt15560() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/multiDecl/kt15560.kt");
        }

        @Test
        @TestMetadata("kt15568.kt")
        public void testKt15568() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/multiDecl/kt15568.kt");
        }

        @Test
        @TestMetadata("kt15575.kt")
        public void testKt15575() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/multiDecl/kt15575.kt");
        }

        @Test
        @TestMetadata("MultiDeclFor.kt")
        public void testMultiDeclFor() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/multiDecl/MultiDeclFor.kt");
        }

        @Test
        @TestMetadata("MultiDeclForComponentExtensions.kt")
        public void testMultiDeclForComponentExtensions() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/multiDecl/MultiDeclForComponentExtensions.kt");
        }

        @Test
        @TestMetadata("MultiDeclForComponentMemberExtensions.kt")
        public void testMultiDeclForComponentMemberExtensions() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/multiDecl/MultiDeclForComponentMemberExtensions.kt");
        }

        @Test
        @TestMetadata("MultiDeclForComponentMemberExtensionsInExtensionFunction.kt")
        public void testMultiDeclForComponentMemberExtensionsInExtensionFunction() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/multiDecl/MultiDeclForComponentMemberExtensionsInExtensionFunction.kt");
        }

        @Test
        @TestMetadata("MultiDeclForValCaptured.kt")
        public void testMultiDeclForValCaptured() throws Exception {
            runTest("compiler/testData/codegen/box/arrays/multiDecl/MultiDeclForValCaptured.kt");
        }

        @Nested
        @TestMetadata("compiler/testData/codegen/box/arrays/multiDecl/int")
        @TestDataPath("$PROJECT_ROOT")
        public class Int {
            @Test
            public void testAllFilesPresentInInt() throws Exception {
                KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/codegen/box/arrays/multiDecl/int"), Pattern.compile("^(.+)\\.kt$"), null, TargetBackend.JS, true);
            }

            @Test
            @TestMetadata("MultiDeclForComponentExtensions.kt")
            public void testMultiDeclForComponentExtensions() throws Exception {
                runTest("compiler/testData/codegen/box/arrays/multiDecl/int/MultiDeclForComponentExtensions.kt");
            }

            @Test
            @TestMetadata("MultiDeclForComponentExtensionsValCaptured.kt")
            public void testMultiDeclForComponentExtensionsValCaptured() throws Exception {
                runTest("compiler/testData/codegen/box/arrays/multiDecl/int/MultiDeclForComponentExtensionsValCaptured.kt");
            }

            @Test
            @TestMetadata("MultiDeclForComponentMemberExtensions.kt")
            public void testMultiDeclForComponentMemberExtensions() throws Exception {
                runTest("compiler/testData/codegen/box/arrays/multiDecl/int/MultiDeclForComponentMemberExtensions.kt");
            }

            @Test
            @TestMetadata("MultiDeclForComponentMemberExtensionsInExtensionFunction.kt")
            public void testMultiDeclForComponentMemberExtensionsInExtensionFunction() throws Exception {
                runTest("compiler/testData/codegen/box/arrays/multiDecl/int/MultiDeclForComponentMemberExtensionsInExtensionFunction.kt");
            }
        }

        @Nested
        @TestMetadata("compiler/testData/codegen/box/arrays/multiDecl/long")
        @TestDataPath("$PROJECT_ROOT")
        public class Long {
            @Test
            public void testAllFilesPresentInLong() throws Exception {
                KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/codegen/box/arrays/multiDecl/long"), Pattern.compile("^(.+)\\.kt$"), null, TargetBackend.JS, true);
            }

            @Test
            @TestMetadata("MultiDeclForComponentExtensions.kt")
            public void testMultiDeclForComponentExtensions() throws Exception {
                runTest("compiler/testData/codegen/box/arrays/multiDecl/long/MultiDeclForComponentExtensions.kt");
            }

            @Test
            @TestMetadata("MultiDeclForComponentExtensionsValCaptured.kt")
            public void testMultiDeclForComponentExtensionsValCaptured() throws Exception {
                runTest("compiler/testData/codegen/box/arrays/multiDecl/long/MultiDeclForComponentExtensionsValCaptured.kt");
            }

            @Test
            @TestMetadata("MultiDeclForComponentMemberExtensions.kt")
            public void testMultiDeclForComponentMemberExtensions() throws Exception {
                runTest("compiler/testData/codegen/box/arrays/multiDecl/long/MultiDeclForComponentMemberExtensions.kt");
            }

            @Test
            @TestMetadata("MultiDeclForComponentMemberExtensionsInExtensionFunction.kt")
            public void testMultiDeclForComponentMemberExtensionsInExtensionFunction() throws Exception {
                runTest("compiler/testData/codegen/box/arrays/multiDecl/long/MultiDeclForComponentMemberExtensionsInExtensionFunction.kt");
            }
        }
    }

    @Nested
    @TestMetadata("compiler/testData/codegen/box/arrays/vArrays")
    @TestDataPath("$PROJECT_ROOT")
    public class VArrays {
        @Test
        public void testAllFilesPresentInVArrays() throws Exception {
            KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/codegen/box/arrays/vArrays"), Pattern.compile("^(.+)\\.kt$"), null, TargetBackend.JS, true);
        }
    }
}
