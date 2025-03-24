import Main
import Testing
//
// @Test
// func testInterface() throws {
//     let expected = SomeFoo()
//
//     let functionResult = identity(obj: expected)
//     #expect(functionResult === expected)
//
//     property = expected
//     let propertyResult = property
//     #expect(propertyResult === expected)
// }
//
// func testNullableInterface() throws {
//     let expected = SomeFoo()
//
//     let nullableFunctionResult = nullableIdentity(value: expected)
//     #expect(nullableFunctionResult === expected)
//
//     nullableProperty = expected
//     let propertyResult = nullableProperty
//     #expect(propertyResult === expected)
// }
//
// func testListOfInterfaces() throws {
//     let expected = [SomeFoo()]
//     let functionResult = listIdentity(value: expected)
//     #expect(functionResult == expected)
//
//     listProperty = expected
//     let propertyResult = listProperty
//     #expect(propertyResult == expected)
// }
//
// func testListOfNullableInterfaces() throws {
//     let expected = [Optional(SomeFoo())]
//     let functionResult = nullablesListIdentity(value: expected)
//     #expect(functionResult == expected)
//
//     nullablesListProperty = expected
//     let propertyResult = nullablesListProperty
//     #expect(propertyResult == expected)
// }
//
// @Test
// func testInterfaceMembers() throws {
//     let instance = SomeFoo()
//
//     let expected = SomeFoo()
//     let functionResult = instance.identity(obj: expected)
//     #expect(functionResult === expected)
//     #expect(functionResult !== instance, "These should not be same")
//
//     instance.property = expected
//     let propertyResult = instance.property
//     #expect(propertyResult === expected)
//     #expect(propertyResult !== instance, "These should not be same")
// }
//
// @Test
// func testInterfaceMembersOfExistential() throws {
//     let instance: any Foo = SomeFoo()
//
//     let expected = SomeFoo()
//     let functionResult = instance.identity(obj: expected)
//     try #require(functionResult === expected)
//     try #require(functionResult !== instance)
//
//     instance.property = expected
//     let propertyResult = instance.property
//     try #require(propertyResult === expected)
//     try #require(propertyResult !== instance)
// }

@Test
func testShouldWrapPrivateTypesIntoKotlinExistentials() throws {
    let expected = value

    let actualFunctionResult = identity(baz: expected)

    try #require(actualFunctionResult === expected)
}
