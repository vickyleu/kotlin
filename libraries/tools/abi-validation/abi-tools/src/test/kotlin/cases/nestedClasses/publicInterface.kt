/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package cases.nestedClasses

public interface PublicInterface {
    public object ObjPublic
    private object ObjPrivate

    public class NestedPublic
    private class NestedPrivate

    public interface NestedPublicInterface
    private interface NestedPrivateInterface

}

