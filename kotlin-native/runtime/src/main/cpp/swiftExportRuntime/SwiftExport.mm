/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SwiftExport.hpp"
#include "Types.h"
#include "ObjCExport.h"
#include <iostream>

#if KONAN_OBJC_INTEROP

#include "KAssert.h"
#include "Memory.h"
#include "WritableTypeInfo.hpp"
#include "std_support/Atomic.hpp"

using namespace kotlin;

extern "C" Class kotlin_wrap_into_existential(Class);

namespace {

__attribute__((optnone))
NO_INLINE Class computeBestFittingClass(const TypeInfo* typeInfo) noexcept {
    std::cerr << "START4" << std::endl;
    std::cerr << "Computing best-fitting ObjC class for type " << typeInfo << std::endl;
    auto& objCExport = kotlin::objCExport(typeInfo);
    std_support::atomic_ref<Class> clazz(objCExport.objCClass);
    Class bestFitting = clazz.load(std::memory_order_relaxed);
    if (bestFitting != nil) {
        std::cerr << "Best-fitting class for type " << typeInfo << " is existing " << class_getName(bestFitting) << std::endl;
        // There's already a stored Class, just return it.
        // We first read it as relaxed, but successful read requires acquire barrier.
        std::atomic_thread_fence(std::memory_order_acquire);
        return bestFitting;
    }

    // Try to get `Class` from name stored in `typeAdapter`.
    if (auto* typeAdapter = objCExport.typeAdapter) {
        if (auto* className = typeAdapter->objCName) {
            bestFitting = objc_getClass(className);
            std::cerr << "Best-fitting class for type " << typeInfo << " is named " << class_getName(bestFitting) << std::endl;
            RuntimeAssert(bestFitting != nil, "Could not find class named %s stored for Kotlin type %p", className, typeInfo);
        }
    }

    // If no best-fitting class found in this type, reuse one from superclass.
    if (bestFitting == nil) {
        auto* superTypeInfo = typeInfo->superType_;
        RuntimeAssert(superTypeInfo != nullptr, "Type %p has no super type", typeInfo);

        if (superTypeInfo == theAnyTypeInfo) { // If this is a root class, we default to existential
            std::cerr << "Best-fitting class for type " << typeInfo << " is existential" << std::endl;
            Class marker = Kotlin_ObjCExport_GetOrCreateClass(typeInfo);
            bestFitting = kotlin_wrap_into_existential(marker); // provided by KotlinRuntimeSupport.swift
        } else { // Otherwise, we default to a parent's wrapper
            std::cerr << "Best-fitting class for type " << typeInfo << " is parent's wrapper" << std::endl;
            bestFitting = computeBestFittingClass(superTypeInfo);
        }
    }
    RuntimeAssert(bestFitting != nil, "No type in %p hierarchy has best-fitting ObjC class", typeInfo);

    // Now cache `bestFitting` directly in `typeInfo`.
    // But don't rewrite it if it's not `nil`, and check that it's the same `Class`.
//     Class expected = nil;
//     if (!clazz.compare_exchange_strong(expected, bestFitting, std::memory_order_acq_rel)) {
//         RuntimeAssert(expected == bestFitting, "Trying to store class %p for Kotlin type %p, but it already has %p", bestFitting, typeInfo, expected);
//         return expected;
//     }
    return bestFitting;
}

} // namespace

Class swiftExportRuntime::bestFittingObjCClassFor(const TypeInfo* typeInfo) noexcept {
    std::cerr << "START2" << std::endl;
//     RuntimeAssert(compiler::swiftExport(), "Only available in Swift Export");
//     AssertThreadState(ThreadState::kNative); // May take some time.
    std::cerr << "START3" << std::endl;
    return computeBestFittingClass(typeInfo);
}

#endif
