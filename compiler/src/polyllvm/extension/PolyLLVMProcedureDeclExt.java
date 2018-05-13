package polyllvm.extension;

import polyglot.ast.Formal;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.ProcedureDecl;
import polyglot.types.*;
import polyglot.util.SerialVersionUID;
import polyllvm.ast.PolyLLVMExt;
import polyllvm.visit.LLVMTranslator;

import java.lang.Override;

import static org.bytedeco.javacpp.LLVM.*;

public class PolyLLVMProcedureDeclExt extends PolyLLVMExt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private static boolean noImplementation(ProcedureInstance pi) {
        return pi.flags().contains(Flags.NATIVE) || pi.flags().contains(Flags.ABSTRACT);
    }

    @Override
    public Node overrideTranslateLLVM(Node parent, LLVMTranslator v) {
        LLVMBasicBlockRef prevBlock = LLVMGetInsertBlock(v.builder);

        ProcedureDecl n = (ProcedureDecl) node();
        TypeSystem ts = v.ts;
        ProcedureInstance pi = n.procedureInstance();
        if (noImplementation(pi))
            return super.overrideTranslateLLVM(parent, v); // Ignore native and abstract methods.

        LLVMTypeRef funcType = v.utils.toLL(pi);
        LLVMValueRef funcRef = v.utils.getFunction(v.mangler.mangleProcName(pi), funcType);
        v.debugInfo.funcDebugInfo(n, funcRef);
        v.pushFn(funcRef);
        v.addTranslation(n, funcRef);

        // Note that the entry block is reserved exclusively for alloca instructions
        // and parameter initialization. Children translations will insert alloca instructions
        // into this block as needed.
        LLVMBasicBlockRef entry = LLVMAppendBasicBlockInContext(v.context, funcRef, "entry");
        LLVMBasicBlockRef body = LLVMAppendBasicBlockInContext(v.context, funcRef, "body");
        LLVMPositionBuilderAtEnd(v.builder, entry);

        // Initialize formals.
        for (int i = 0; i < n.formals().size(); ++i) {
            Formal formal = n.formals().get(i);
            LocalInstance li = formal.localInstance().orig();
            LLVMTypeRef typeRef = v.utils.toLL(formal.declType());
            LLVMValueRef alloca = v.utils.buildAlloca(formal.name(), typeRef);

            v.addTranslation(li, alloca);
            v.debugInfo.createParamVariable(v, formal, i, alloca);

            int idx = i + (pi.flags().isStatic() ? 0 : 1);
            LLVMBuildStore(v.builder, LLVMGetParam(funcRef, idx), alloca);
        }

        // Register as entry point if applicable.
        boolean isEntryPoint = n.name().equals("main")
                && n.flags().isPublic()
                && n.flags().isStatic()
                && n.formals().size() == 1
                && n.formals().iterator().next().declType().equals(ts.arrayOf(ts.String()));
        if (isEntryPoint) {
            String className = n.procedureInstance().container().toClass().fullName();
            v.addEntryPoint(funcRef, className);
        }

        // Recurse to children.
        LLVMPositionBuilderAtEnd(v.builder, body);
        n = (ProcedureDecl) lang().visitChildren(n, v);

        // Add void return if necessary.
        Type retType = n instanceof MethodDecl
                ? ((MethodDecl) n).returnType().type()
                : ts.Void();
        if (!v.utils.blockTerminated()) {
            if (retType.isVoid()) {
                LLVMBuildRetVoid(v.builder);
            } else {
                LLVMBuildUnreachable(v.builder);
            }
        }

        // We build this branch at the end since child translations need to be able
        // to insert into the entry block before its terminator. (LLVMPositionBuilderBefore
        // is inconvenient because it changes the debug location.)
        LLVMPositionBuilderAtEnd(v.builder, entry);
        LLVMBuildBr(v.builder, body);

        v.popFn();
        v.debugInfo.popScope();

        LLVMPositionBuilderAtEnd(v.builder, prevBlock);
        return n;
    }
}
