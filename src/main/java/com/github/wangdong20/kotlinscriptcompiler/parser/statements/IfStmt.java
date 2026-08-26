package com.github.wangdong20.kotlinscriptcompiler.parser.statements;

import com.github.wangdong20.kotlinscriptcompiler.parser.expressions.Exp;

import java.util.List;

public class IfStmt implements Stmt {
    private final Exp condition;
    private final BlockStmt trueBranch;
    private final BlockStmt falseBranch;

    public IfStmt(Exp condition, BlockStmt trueBranch, BlockStmt falseBranch) {
        this.condition = condition;
        this.trueBranch = trueBranch;
        this.falseBranch = falseBranch;
    }

    public IfStmt(Exp condition, BlockStmt trueBranch) {
        this.condition = condition;
        this.trueBranch = trueBranch;
        this.falseBranch = null;
    }

    public Exp getCondition() {
        return condition;
    }

    public BlockStmt getTrueBranch() {
        return trueBranch;
    }

    public BlockStmt getFalseBranch() {
        return falseBranch;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof IfStmt stmt) {
            if(stmt.getCondition().equals(condition) &&
                    stmt.getTrueBranch().equals(trueBranch)) {
                if((stmt.getFalseBranch() == null && falseBranch == null) ||
                        stmt.getFalseBranch().equals(falseBranch)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "IfStmt{" +
                "condition=" + condition +
                ", trueBranch=" + trueBranch +
                ", falseBranch=" + falseBranch +
                '}';
    }
}
