//Copyright (C) 2018 Cornell University

package jlang.extension;

import polyglot.ast.Conditional;
import polyglot.ast.Expr;
import polyglot.ast.Node;

import java.util.function.BiConsumer;

import jlang.ast.JLangExt;
import jlang.visit.LLVMTranslator;

import static org.bytedeco.javacpp.LLVM.*;

public class JLangConditionalExt extends JLangExt {

    @Override
    public Node overrideTranslateLLVM(Node parent, LLVMTranslator v) {
        Conditional n = (Conditional) node();

        LLVMValueRef conditionalTemp = v.utils.buildAlloca("cond.temp", v.utils.toLL(n.type()));

        LLVMBasicBlockRef ifEnd = v.utils.buildBlock("cond.end");
        LLVMBasicBlockRef ifTrue = v.utils.buildBlock("cond.true");
        LLVMBasicBlockRef ifFalse = v.utils.buildBlock("cond.false");

        lang().translateLLVMConditional(n.cond(), v, ifTrue, ifFalse);

        BiConsumer<LLVMBasicBlockRef, Expr> emitBlock = (block, expr) -> {
            LLVMPositionBuilderAtEnd(v.builder, block);
            v.visitEdge(n, expr);
            LLVMBuildStore(v.builder, v.getTranslation(expr), conditionalTemp);
            v.utils.branchUnlessTerminated(ifEnd);
        };

        emitBlock.accept(ifTrue, n.consequent());
        emitBlock.accept(ifFalse, n.alternative());
        LLVMPositionBuilderAtEnd(v.builder, ifEnd);

        v.addTranslation(n, LLVMBuildLoad(v.builder, conditionalTemp, "cond.load.temp"));

        return n;
    }
}
