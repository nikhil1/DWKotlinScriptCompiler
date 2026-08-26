package com.github.wangdong20.kotlinscriptcompiler.parser.statements;

import com.github.wangdong20.kotlinscriptcompiler.parser.expressions.Exp;
import com.github.wangdong20.kotlinscriptcompiler.parser.expressions.VariableExp;
import com.github.wangdong20.kotlinscriptcompiler.parser.type.Type;

import java.util.LinkedHashMap;

public class FunctionDeclareStmt implements Stmt {
    private final VariableExp funcName;
    private final Type returnType;
    private final LinkedHashMap<Exp, Type> parameterList;
    private final BlockStmt blockStmt;

    public FunctionDeclareStmt(VariableExp funcName, Type returnType, LinkedHashMap<Exp, Type> parameterList, BlockStmt blockStmt) {
        this.funcName = funcName;
        this.returnType = returnType;
        this.parameterList = parameterList;
        this.blockStmt = blockStmt;
    }

    public VariableExp getFuncName() {
        return funcName;
    }

    public Type getReturnType() {
        return returnType;
    }

    public LinkedHashMap<Exp, Type> getParameterList() {
        return parameterList;
    }

    public BlockStmt getBlockStmt() {
        return blockStmt;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof FunctionDeclareStmt stmt) {
            if(stmt.getReturnType().equals(returnType)
                && stmt.getFuncName().equals(funcName)) {
                if ((stmt.getParameterList() == null && parameterList == null)
                        || stmt.getParameterList().equals(parameterList)){
                    if((stmt.getBlockStmt() == null && blockStmt == null)
                            || stmt.getBlockStmt().equals(blockStmt)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "FunctionDeclareStmt{" +
                "funcName=" + funcName +
                ", returnType=" + returnType +
                ", parameterList=" + parameterList +
                ", blockStmt=" + blockStmt +
                '}';
    }
}
