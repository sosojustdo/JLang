//Copyright (C) 2018 Cornell University

// Provides reflection-like support, especially for use by JNI.
// JLang emits static information for each class, and registers
// that information by calling RegisterJavaClass upon loading a class.
// The other functions defined here provide access to
// class information in an organized way.
//
// For example, this interface can be used to get Java field offsets,
// Java method pointers, Java class names.
#pragma once

#include "jni.h"
#include <stdint.h>
#include <utility>

#define IS_STATIC_METHOD(minfo) ((minfo)->offset == -1)
#define IS_CONSTRUCTOR(minfo) ((minfo)->offset == -2)
#define IS_INTERFACE_METHOD(minfo) ((minfo)->inft_id == NULL && minfo->intf_id_hash == 0)

extern "C" {
// These structs are generated statically for each class, and
// exist for the lifetime of the program.
// The layout must precisely mirror the layout defined in JLang.

struct JavaTypeInfo {
    jclass* type_ptr;
    jclass(*init_type_class)(void);
};

// Concrete representation for the opaque type jfieldID.
struct JavaFieldInfo {
    char* name;
    int32_t offset;
    int32_t modifiers;
    struct JavaTypeInfo* type_info_ptr;
    char* sig;
};
//This is also a representation for the jfieldID type, but
//represented differently since static fields are implemented as global pointers.
struct JavaStaticFieldInfo {
    char* name;
    char* sig;
    void* ptr;
    int32_t modifiers;
    struct JavaTypeInfo* type_info_ptr;
};

// Concrete representation for the opaque type jmethodID.
struct JavaMethodInfo {
    char* name;       // Name (without signature).
    char* sig;        // JNI-specified signature encoding.
    int32_t offset;   // Offset into dispatch vector. -1 for static methods.
    void* fnPtr;      // Used for CallNonvirtual and CallStatic.
    void* trampoline; // Trampoline for casting the fnPtr to the correct type.
    void* intf_id;    // For interface methods, the interface id.
    int32_t intf_id_hash; // A precomputed hash of the intf_id.
    int32_t modifiers;
    jclass* returnType;
    int32_t numArgTypes;
    jclass** argTypes;  // array of arg types
};

struct JavaClassInfo {
    char* name;
    jclass* super_ptr;
    void* cdv; //is a DispatchVector*
    int32_t obj_size;

    jboolean isIntf;

    int32_t num_intfs;
    jclass** intfs;

    int32_t num_fields;
    JavaFieldInfo* fields;

    int32_t num_static_fields;
    JavaStaticFieldInfo* static_fields;

    int32_t num_methods;
    JavaMethodInfo* methods;

};

// Called by the runtime at most once per class to register
// the class information declared above.
void
RegisterJavaClass(jclass cls, const JavaClassInfo* data);

void InternStringLit(jstring str);

} // extern "C"

const JavaClassInfo*
GetJavaClassInfo(jclass cls);

//Assumes non-null valid C-string for name
//name is in java.lang.String format
const jclass
GetJavaClassFromName(const char* name);

//Assumes non-null valid C-string for name
//name is in java/lang/String format
const jclass
GetJavaClassFromPathName(const char* name);

const JavaFieldInfo*
GetJavaFieldInfo(jclass cls, const char* name);

const JavaStaticFieldInfo*
GetJavaStaticFieldInfo(jclass cls, const char* name, const char* sig);

const std::pair<JavaMethodInfo*,int32_t>
GetJavaMethodInfo(jclass cls, const char* name, const char* sig);

const std::pair<JavaMethodInfo*, int32_t>
GetJavaStaticMethodInfo(jclass cls, const char* name, const char* sig);

jclass
LoadJavaClassFromLib(const char* name);

bool isArrayClass(jclass cls);

bool isPrimitiveClass(jclass cls);

jclass GetComponentClass(jclass cls);

int arrayRepSize(jclass cls);

extern jclass Polyglot_native_int;
extern jclass Polyglot_native_byte;
extern jclass Polyglot_native_short;
extern jclass Polyglot_native_long;
extern jclass Polyglot_native_float;
extern jclass Polyglot_native_double;
extern jclass Polyglot_native_char;
extern jclass Polyglot_native_boolean;
extern jclass Polyglot_native_void;

extern jclass* Polyglot_native_int_class_type_info;
extern jclass* Polyglot_native_byte_class_type_info;
extern jclass* Polyglot_native_short_class_type_info;
extern jclass* Polyglot_native_long_class_type_info;
extern jclass* Polyglot_native_float_class_type_info;
extern jclass* Polyglot_native_double_class_type_info;
extern jclass* Polyglot_native_char_class_type_info;
extern jclass* Polyglot_native_boolean_class_type_info;
extern jclass* Polyglot_native_void_class_type_info;
