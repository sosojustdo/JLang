package polyllvm.extension;

import polyglot.ast.Node;
import polyglot.ast.Special;
import polyglot.util.InternalCompilerError;
import polyglot.util.SerialVersionUID;
import polyllvm.ast.PolyLLVMExt;
import polyllvm.ast.PolyLLVMNodeFactory;
import polyllvm.ast.PseudoLLVM.Expressions.LLVMVariable;
import polyllvm.ast.PseudoLLVM.Expressions.LLVMVariable_c.VarType;
import polyllvm.ast.PseudoLLVM.LLVMTypes.LLVMTypeNode;
import polyllvm.ast.PseudoLLVM.Statements.LLVMConversion;
import polyllvm.ast.PseudoLLVM.Statements.LLVMInstruction;
import polyllvm.util.PolyLLVMConstants;
import polyllvm.util.PolyLLVMFreshGen;
import polyllvm.util.PolyLLVMTypeUtils;
import polyllvm.visit.PseudoLLVMTranslator;

public class PolyLLVMSpecialExt extends PolyLLVMExt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node translatePseudoLLVM(PseudoLLVMTranslator v) {
        Special n = (Special) node();
        PolyLLVMNodeFactory nf = v.nodeFactory();

        if (n.qualifier() != null) {
            throw new InternalCompilerError("Qualifier on this not supported yet (Java spec 15.8.4)");
        }

        if (n.kind() == Special.THIS) {
            LLVMTypeNode thisType =
                    PolyLLVMTypeUtils.polyLLVMTypeNode(nf, n.type());
            v.addTranslation(n,
                             nf.LLVMVariable(PolyLLVMConstants.THISSTRING,
                                             thisType,
                                             VarType.LOCAL));
        }
        else if (n.kind() == Special.SUPER) {
            LLVMTypeNode thisType =
                    PolyLLVMTypeUtils.polyLLVMTypeNode(nf,
                                                       v.getCurrentClass()
                                                        .type());
            LLVMTypeNode superType =
                    PolyLLVMTypeUtils.polyLLVMTypeNode(nf, n.type());
            LLVMVariable thisVariable =
                    nf.LLVMVariable(PolyLLVMConstants.THISSTRING,
                                    thisType,
                                    VarType.LOCAL);
            LLVMVariable result = PolyLLVMFreshGen.freshLocalVar(nf, superType);
            LLVMInstruction cast =
                    nf.LLVMConversion(LLVMConversion.BITCAST,
                                      thisType,
                                      thisVariable,
                                      superType)
                      .result(result);
            v.addTranslation(n, nf.LLVMESeq(cast, result));
        }

        return super.translatePseudoLLVM(v);
    }

}
