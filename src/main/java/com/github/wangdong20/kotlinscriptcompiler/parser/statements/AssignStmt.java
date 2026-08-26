package com.github.wangdong20.kotlinscriptcompiler.parser.statements;

import com.github.wangdong20.kotlinscriptcompiler.parser.expressions.Exp;
import com.github.wangdong20.kotlinscriptcompiler.parser.expressions.Variable;
import com.github.wangdong20.kotlinscriptcompiler.parser.expressions.VariableExp;
import com.github.wangdong20.kotlinscriptcompiler.parser.type.Type;

public class AssignStmt implements Stmt {
    private final Exp expression;
    private final Variable variable;
    private final Type type;
    private boolean readOnly = false;
    private boolean isNew = false;

    public Exp getExpression() {
        return expression;
    }

    public Variable getVariable() {
        return variable;
    }

    public Type getType() {
        return type;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isNew() {
        return isNew;
    }

    public AssignStmt(Exp expression, Variable variable, Type type, boolean readOnly, boolean isNew) {
        this.expression = expression;
        this.variable = variable;
        this.type = type;
        this.readOnly = readOnly;
        this.isNew = isNew;
    }

    public AssignStmt(Exp expression, Variable variable, boolean readOnly, boolean isNew) {
        this.expression = expression;
        this.variable = variable;
        this.type = null;
        this.readOnly = readOnly;
        this.isNew = isNew;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof AssignStmt stmt) {
            if(stmt.getVariable().equals(variable) && stmt.getExpression().equals(expression) &&
                    stmt.isReadOnly() == readOnly && stmt.isNew() == isNew) {
                if((stmt.getType() == null && type == null) || stmt.getType().equals(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "AssignStmt{" +
                "expression=" + expression +
                ", variable=" + variable +
                ", type=" + type +
                ", readOnly=" + readOnly +
                ", isNew=" + isNew +
                '}';
    }
}
