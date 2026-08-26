package com.github.wangdong20.kotlinscriptcompiler.typechecker;

import com.github.wangdong20.kotlinscriptcompiler.parser.Program;
import com.github.wangdong20.kotlinscriptcompiler.parser.expressions.*;
import com.github.wangdong20.kotlinscriptcompiler.parser.statements.*;
import com.github.wangdong20.kotlinscriptcompiler.parser.type.*;

import java.util.*;

public class Typechecker {

    private static Map<Pair<Variable, List<Type>>, FunctionDeclareStmt> funcMap;
    private static Type returnTypeFromFunc;
    private static int returnEvaluate = 0;  // Evaluate return, if return needed in Function declaration, it is -1, if return not needed it is 0, if return need in if statement, it is -2 for both true false branch.
    private static boolean alreadyReturn;   // Already return in block statements, no need to check remain statements in block statement.

    private static Type typeOf(final Map<Variable, Pair<Type, Boolean>> gamma, final Exp e) throws IllTypedException {
        if(e instanceof IntExp) {
            return BasicType.TYPE_INT;
        } else if(e instanceof BooleanExp) {
            return BasicType.TYPE_BOOLEAN;
        } else if(e instanceof StringExp exp) {
            if(exp.getInterpolationExp() != null) {
                List<Exp> exps = new ArrayList<>(exp.getInterpolationExp().values());
                Type type;
                for (Exp exp : exps) {
                    type = typeOf(gamma, exp);
                    if (!(type instanceof BasicType)) {
                        throw new IllTypedException("Only basic type can be the type in string interpolation expression");
                    }
                }
            }
            return BasicType.TYPE_STRING;
        } else if(e instanceof AdditiveExp exp) {
            final Type leftType = typeOf(gamma, exp.getLeft());
            final Type rightType = typeOf(gamma, exp.getRight());
            if (leftType == BasicType.TYPE_INT && rightType == BasicType.TYPE_INT) {
                return BasicType.TYPE_INT;
            }
            final AdditiveOp op = exp.getOp();
            if (op == AdditiveOp.EXP_PLUS) {
                if (leftType == BasicType.TYPE_STRING && rightType == BasicType.TYPE_INT) {
                    return BasicType.TYPE_STRING;
                } else if (leftType == BasicType.TYPE_STRING && rightType == BasicType.TYPE_STRING) {
                    return BasicType.TYPE_STRING;
                }
            }
            throw new IllTypedException("Only Int + Int, Int - Int, String + Int, String + String accept!");
        } else if(e instanceof MultiplicativeExp exp) {
            final Type leftType = typeOf(gamma, exp.getLeft());
            final Type rightType = typeOf(gamma, exp.getRight());
            if (leftType == BasicType.TYPE_INT && rightType == BasicType.TYPE_INT) {
                return BasicType.TYPE_INT;
            } else {
                throw new IllTypedException("Only Int * Int and Int / Int accept!");
            }
        } else if(e instanceof ComparableExp exp) {
            final Type leftType = typeOf(gamma, exp.getLeft());
            final Type rightType = typeOf(gamma, exp.getRight());
            if (leftType == BasicType.TYPE_INT && rightType == BasicType.TYPE_INT) {
                return BasicType.TYPE_BOOLEAN;
            } else if(exp.getOp() == ComparableOp.OP_EQUAL_EQUAL && leftType == BasicType.TYPE_BOOLEAN && rightType == BasicType.TYPE_BOOLEAN) {
                return BasicType.TYPE_BOOLEAN;
            }
            else {
                throw new IllTypedException("Only Int can compare with Int!");
            }
        } else if(e instanceof BiLogicalExp exp) {
            final Type leftType = typeOf(gamma, exp.getLeft());
            final Type rightType = typeOf(gamma, exp.getRight());
            if (leftType == BasicType.TYPE_BOOLEAN && rightType == BasicType.TYPE_BOOLEAN) {
                return BasicType.TYPE_BOOLEAN;
            } else {
                throw new IllTypedException("Only Boolean && Boolean and Boolean || Boolean supported!");
            }
        } else if(e instanceof VariableExp exp) {
            if(gamma.containsKey(e)) {
                return gamma.get(e).getFirst();
            } else {
                throw new IllTypedException("Not in scope " + exp.getName());
            }
        } else if(e instanceof ArrayExp exp) {
            LambdaExp lambdaExp = exp.getLambdaExp();
            Exp size = exp.getSize();
            if(size != null) {
                Type sizeType = typeOf(gamma, size);
                if(sizeType != BasicType.TYPE_INT) {
                    throw new IllTypedException("ArrayExp must initial with value of IntType");
                }
            } else {
                throw new IllTypedException("ArrayExp must have size initialed.");
            }
            if(lambdaExp.getParameterList().size() == 1) {  // ArrayExp only support Array(Int, {i - > exp})
                VariableExp[] variables = new VariableExp[1];
                Type[] types = new Type[1];
                final Map<Variable, Pair<Type, Boolean>> newGama = newCopy(gamma);
                lambdaExp.getParameterList().keySet().toArray(variables);
                lambdaExp.getParameterList().values().toArray(types);
                if(types[0] == null) {
                    newGama.put(variables[0], new Pair<>(BasicType.TYPE_INT, false));
                } else {
                    if(types[0] == BasicType.TYPE_INT) {
                        newGama.put(variables[0], new Pair<>(types[0], false));
                    } else {
                        throw new IllTypedException("Expected parameter type of Int!");
                    }
                }
                Type returnType = typeOf (newGama, exp.getLambdaExp().getReturnExp());
                if(returnType instanceof BasicType type) {
                    return new TypeArray(type);
                } else {
                    throw new IllTypedException("Unsupported generic type: " + returnType);
                }
            } else {
                throw new IllTypedException("Parameter size should be 1");
            }

        } else if(e instanceof ArrayOfExp ofExp) {
            if(ofExp.getExpList().size() > 0) {
                Type type = typeOf(gamma, ofExp.getExpList().get(0));
                boolean isAny = false;
                for (Exp exp : ofExp.getExpList()) {
                    if(type != typeOf(gamma, exp)) {
                        isAny = true;
                    }
                }
                if(isAny) {
                    return new TypeArray(BasicType.TYPE_ANY);
                } else {
                    if(type instanceof BasicType basicType)
                        return new TypeArray(basicType);
                    else
                        throw new IllTypedException("Unsupported generic type: " + type);
                }
            } else {
                throw new IllTypedException("arrayOf(exp*) should have at least one expression in parameter");
            }
        } else if(e instanceof ArrayWithIndexExp exp) {
            if(gamma.containsKey(exp.getVariableExp())) {
                if(typeOf(gamma, exp.getIndexExp()) != BasicType.TYPE_INT) {
                    throw new IllTypedException("Array Index should be Int type");
                }
                if(gamma.get(exp.getVariableExp()).getFirst() instanceof TypeArray) {
                    return ((TypeArray) gamma.get(exp.getVariableExp()).getFirst()).getBasicType();
                } else {
                    throw new IllTypedException(exp.getVariableExp().getName() + " is not a array");
                }
            } else {
                throw new IllTypedException("Not in scope " + exp.getVariableExp().getName());
            }
        } else if(e instanceof FunctionInstanceExp instanceExp) {
            List<Type> parameters = new ArrayList<>();
            Type type;
            for(Exp exp : instanceExp.getParameterList()) {
                // No same parameter.
                type = typeOf(gamma, exp);
                parameters.add(type);
            }
            Pair<Variable, List<Type>> key = new Pair<>(instanceExp.getFuncName(), parameters);
            if(!funcMap.containsKey(key)) {
                if(gamma.containsKey(instanceExp.getFuncName())) {
                    if(gamma.get(instanceExp.getFuncName()).getFirst() instanceof TypeHighOrderFunction) {
                        TypeHighOrderFunction highOrderFunction = (TypeHighOrderFunction) gamma.get(instanceExp.getFuncName()).getFirst();
                        if(!highOrderFunction.getParameterList().equals(parameters)) {
                            throw new IllTypedException("Function instance " + instanceExp.getFuncName().getName() + "("
                                + parameters + ") does not match with the Function declaration " + instanceExp.getFuncName().getName() + "("
                                + highOrderFunction.getParameterList() + ")");
                        }
                        return highOrderFunction.getReturnType();
                    } else {
                        throw new IllTypedException("Function " + instanceExp.getFuncName().getName() + "("
                                + parameters + ")" + " undefined");
                    }
                } else {
                    throw new IllTypedException("Function " + instanceExp.getFuncName().getName() + "("
                            + parameters + ")" + " undefined");
                }
            } else {
                return funcMap.get(key).getReturnType();
            }
        } else if(e instanceof LambdaExp exp) {
            LinkedHashMap<VariableExp, Type> parameterList = exp.getParameterList();

            if(parameterList.size() > 0) {
                VariableExp[] variableExps = new VariableExp[parameterList.size()];
                Type[] types = new Type[parameterList.size()];
                final Map<Variable, Pair<Type, Boolean>> newGama = newCopy(gamma);
                parameterList.keySet().toArray(variableExps);
                parameterList.values().toArray(types);

                for(int i = 0; i < variableExps.length; i++) {
                    newGama.put(variableExps[i], new Pair<>(types[i], false));
                }
                Type returnType = typeOf(newGama, exp.getReturnExp());
                List<Type> parameterTypes = Arrays.asList(types);
                return new TypeHighOrderFunction(parameterTypes, returnType);
            } else {
                Type returnType = typeOf(gamma, exp.getReturnExp());
                return new TypeHighOrderFunction(new ArrayList<>(), returnType);
            }
        } else if(e instanceof MutableListExp exp) {
            LambdaExp lambdaExp = exp.getLambdaExp();
            Exp size = exp.getSize();
            if(size != null) {
                Type sizeType = typeOf(gamma, size);
                if(sizeType != BasicType.TYPE_INT) {
                    throw new IllTypedException("MutableListExp must initial with value of IntType");
                }
            } else {
                throw new IllTypedException("MutableListExp must have size initialed.");
            }
            if(lambdaExp.getParameterList().size() == 1) {  // MutableListExp only support MutableList(Int, {i - > exp})
                VariableExp[] variables = new VariableExp[1];
                Type[] types = new Type[1];
                final Map<Variable, Pair<Type, Boolean>> newGama = newCopy(gamma);
                lambdaExp.getParameterList().keySet().toArray(variables);
                lambdaExp.getParameterList().values().toArray(types);
                if(types[0] == null) {
                    newGama.put(variables[0], new Pair<>(BasicType.TYPE_INT, false));
                } else {
                    if(types[0] == BasicType.TYPE_INT) {
                        newGama.put(variables[0], new Pair<>(types[0], false));
                    } else {
                        throw new IllTypedException("Expected parameter type of Int!");
                    }
                }
                Type returnType = typeOf (newGama, exp.getLambdaExp().getReturnExp());
                if(returnType instanceof BasicType type) {
                    return new TypeArray(type);
                } else {
                    throw new IllTypedException("Unsupported generic type: " + returnType);
                }
            } else {
                throw new IllTypedException("Parameter size should be 1");
            }
        } else if(e instanceof MutableListOfExp ofExp) {
            if(ofExp.getExpList().size() > 0) {
                Type type = typeOf(gamma, ofExp.getExpList().get(0));
                boolean isAny = false;
                for (Exp exp : ofExp.getExpList()) {
                    if(type != typeOf(gamma, exp)) {
                        isAny = true;
                    }
                }
                if(isAny) {
                    return new TypeMutableList(BasicType.TYPE_ANY);
                } else {
                    if(type instanceof BasicType basicType)
                        return new TypeMutableList(basicType);
                    else
                        throw new IllTypedException("Unsupported generic type: " + type);
                }
            } else {
                throw new IllTypedException("mutableListOf(exp*) should have at least one expression in parameter");
            }
        } else if(e instanceof NotExp exp) {
            Type type = typeOf(gamma, exp.getValue());
            if(type != BasicType.TYPE_BOOLEAN) {
                throw new IllTypedException("Only !Boolean accept");
            }
            return BasicType.TYPE_BOOLEAN;
        } else if(e instanceof RangeExp exp) {
            Type start = typeOf(gamma, exp.getStart());
            Type end = typeOf(gamma, exp.getEnd());
            if(start != BasicType.TYPE_INT || end != BasicType.TYPE_INT) {
                throw new IllTypedException("Range expression only support Int..Int");
            }
            return new TypeArray(BasicType.TYPE_INT);   // we also count range exp as array type
        } else if(e instanceof SelfOperationExp exp) {
            Type type = typeOf(gamma, (Exp)exp.getVariableExp());
            if(type != BasicType.TYPE_INT) {
                throw new IllTypedException("Only Int support ++, -- operation");
            }
            return BasicType.TYPE_INT;
        } else {
            assert(false);
            throw new IllTypedException("Unknown type!");
        }
    }

    private static Map<Variable, Pair<Type, Boolean>> typecheckStmt(final Map<Variable, Pair<Type, Boolean>> gamma, boolean continueBreakOk, boolean returnOk, Stmt s) throws IllTypedException {
        if(s instanceof VariableDeclareStmt stmt) {
            if(gamma.containsKey(stmt.getVariableExp())) {
                throw new IllTypedException("Redefined variable " + stmt.getVariableExp().getName());
            } else {
                if(stmt.getType() != null) {
                    if(stmt.isReadOnly()) {
                        throw new IllTypedException("This variable must either have a type annotation or be initialized");
                    }
                    final Map<Variable, Pair<Type, Boolean>> copy = newCopy(gamma);
                    copy.put(stmt.getVariableExp(), new Pair<>(stmt.getType(), stmt.isReadOnly()));
                    return copy;
                } else {
                    throw new IllTypedException("This variable must either have a type annotation or be initialized");
                }
            }
        } else if(s instanceof AssignStmt stmt) {
            if(stmt.isNew()) {      // It means var, val a new variable.
                if(gamma.containsKey(stmt.getVariable())) {
                    throw new IllTypedException(stmt.getVariable() + " redefined!");
                }
                if (stmt.getType() != null) {
                    Type expectedType = stmt.getType();
                    if (typeOf(gamma, stmt.getExpression()).equals(expectedType)) {
                        final Map<Variable, Pair<Type, Boolean>> copy = newCopy(gamma);
                        copy.put(stmt.getVariable(), new Pair<>(expectedType, stmt.isReadOnly()));
                        return copy;
                    } else {
                        throw new IllTypedException(expectedType + "expected!");
                    }
                } else {    // Type inference
                    Type type = typeOf(gamma, stmt.getExpression());
                    final Map<Variable, Pair<Type, Boolean>> copy = newCopy(gamma);
                    copy.put(stmt.getVariable(), new Pair<>(type, stmt.isReadOnly()));
                    return copy;
                }
            } else {    // we need to check gamma contain the variable or not in this case
                if(gamma.containsKey(stmt.getVariable())) {
                    if(gamma.get(stmt.getVariable()).getSecond()) { // Read only variable
                        throw new IllTypedException(stmt.getVariable() + " is read only variable!");
                    } else {
                        Type expectedType = typeOf(gamma, (Exp)stmt.getVariable());
                        if(!typeOf(gamma, stmt.getExpression()).equals(expectedType)) {
                            throw new IllTypedException(expectedType + " expected");
                        }
                        return gamma;
                    }
                } else if(stmt.getVariable() instanceof ArrayWithIndexExp) {
                    Type expected = typeOf(gamma, (Exp)stmt.getVariable());
                    if(gamma.get(((ArrayWithIndexExp) stmt.getVariable()).getVariableExp()).getSecond()) {
                        throw new IllTypedException(((ArrayWithIndexExp) stmt.getVariable()).getVariableExp() + " is read only variable!");
                    }
                    if(typeOf(gamma, stmt.getExpression()).equals(expected)) {
                        return gamma;
                    } else {
                        throw new IllTypedException(expected + " expected for expression");
                    }
                } else {
                    throw new IllTypedException(stmt.getVariable() + " undefined!");
                }
            }
        } else if(s instanceof CompoundAssignStmt stmt) {
            if(gamma.containsKey(stmt.getVariable())) {
                if(gamma.get(stmt.getVariable()).getSecond()) {
                    throw new IllTypedException("Read only variable cannot be assigned a new value!");
                }
                Type expected = typeOf(gamma, stmt.getExpression());
                Variable variable = stmt.getVariable();
                CompoundAssignOp op = stmt.getOp();
                if (op == CompoundAssignOp.EXP_DIVIDE_EQUAL || op == CompoundAssignOp.EXP_MULTIPLY_EQUAL
                        || op == CompoundAssignOp.EXP_MINUS_EQUAL) {
                    if(expected == BasicType.TYPE_INT && gamma.get(variable).getFirst() == BasicType.TYPE_INT) {
                        return gamma;
                    } else {
                        throw new IllTypedException("-=, *=, /= only support integer operation!");
                    }
                } else {
                    if((expected == BasicType.TYPE_INT && gamma.get(variable).getFirst() == BasicType.TYPE_INT)
                            || (expected == BasicType.TYPE_STRING && gamma.get(variable).getFirst() == BasicType.TYPE_STRING)
                            || (expected == BasicType.TYPE_INT && gamma.get(variable).getFirst() == BasicType.TYPE_STRING)) {
                        return gamma;
                    } else {
                        throw new IllTypedException("Only Int += Int, String += Int, String += String supported!");
                    }
                }
            } else if(stmt.getVariable() instanceof ArrayWithIndexExp) {
                Type expected = typeOf(gamma, (Exp)stmt.getVariable());
                if(gamma.get(((ArrayWithIndexExp) stmt.getVariable()).getVariableExp()).getSecond()) {
                    throw new IllTypedException(((ArrayWithIndexExp) stmt.getVariable()).getVariableExp() + " is read only variable!");
                }
                if(typeOf(gamma, stmt.getExpression()).equals(expected)) {
                    return gamma;
                } else {
                    throw new IllTypedException(expected + " expected for expression");
                }
            } else {
                throw new IllTypedException(stmt.getVariable() + " undefined!");
            }
        } else if(s instanceof ForStmt asFor) {
            final Map<Variable, Pair<Type, Boolean>> newGama = newCopy(gamma);
            if(asFor.getArrayExp() != null) {
                Type type = typeOf(newGama, asFor.getArrayExp());
                if(type instanceof TypeArray || type instanceof TypeMutableList) {  // Type inference for array or list
                    if(type instanceof TypeArray array) {
                        newGama.put(asFor.getIteratorExp(), new Pair<>(array.getBasicType(), false));
                    } else {
                        newGama.put(asFor.getIteratorExp(), new Pair<>(((TypeMutableList) type).getBasicType(), false));
                    }
                } else {
                    throw new IllTypedException(asFor.getArrayExp() + " is not a collection");
                }
            } else {
                if(asFor.getStepExp() != null) {    // Only range expression can have step expression
                    Type type = typeOf(newGama, asFor.getStepExp());
                    if(type != BasicType.TYPE_INT) {
                        throw new IllTypedException("Expression after step should be Int type");
                    }
                }
                newGama.put(asFor.getIteratorExp(), new Pair<>(BasicType.TYPE_INT, false));
            }

            typecheckBlockStmts(newGama, true, returnOk, asFor.getBlockStmt());
            return gamma;
        } else if(s instanceof WhileStmt asWhile) {
            Type type = typeOf(gamma, asWhile.getCondition());
            if(type == BasicType.TYPE_BOOLEAN) {
                typecheckBlockStmts(gamma, true, returnOk, asWhile.getBlockStmt());
                return gamma;
            } else {
                throw new IllTypedException("while condition should be boolean type");
            }
        } else if(s instanceof BlockStmt stmt) {
            typecheckBlockStmts(gamma, continueBreakOk, returnOk, stmt);
            return gamma;
        } else if(s instanceof ControlLoopStmt) {
            if(!continueBreakOk) {
                throw new IllTypedException("break or continue should be in loop scope");
            } else {
                return gamma;
            }
        }
        else if(s instanceof FunctionDeclareStmt asFunDeclare) {
            LinkedHashMap<Exp, Type> parameters = asFunDeclare.getParameterList();
            VariableExp[] variableExps = new VariableExp[parameters.size()];
            Type[] types = new Type[parameters.size()];
            final Map<Variable, Pair<Type, Boolean>> newGama = newCopy(gamma);
            parameters.keySet().toArray(variableExps);
            parameters.values().toArray(types);

            for(int i = 0; i < variableExps.length; i++) {
                newGama.put(variableExps[i], new Pair<>(types[i], false));
            }
            returnTypeFromFunc = asFunDeclare.getReturnType();
            if(returnTypeFromFunc != BasicType.TYPE_UNIT) {
                returnEvaluate = -1;
            }
            typecheckBlockStmts(newGama, continueBreakOk, true, asFunDeclare.getBlockStmt());
            if(returnEvaluate < 0) {
                throw new IllTypedException("Missing return " + returnTypeFromFunc + " in Function Declaration " + asFunDeclare.getFuncName()
                    + "(" + Arrays.toString(types) + ")" + " : " + returnTypeFromFunc);
            }
            returnTypeFromFunc = null;
            return gamma;
        } else if(s instanceof ReturnStmt stmt) {
            if (!returnOk) {
                throw new IllTypedException("return statement should only be in the body of function declare statement");
            }

            if(stmt.getReturnExp() != null) {
                Type returnType = typeOf(gamma, stmt.getReturnExp());
                if (returnTypeFromFunc == null || returnTypeFromFunc != returnType) {
                    throw new IllTypedException("return type should be the same as return type in function declaration.");
                }
            } else {    // no exp after return
                if(returnTypeFromFunc != BasicType.TYPE_UNIT) {
                    throw new IllTypedException("the function is not void function, should return something.");
                }
            }
            if(returnEvaluate < 0) {
                returnEvaluate++;
            }
            alreadyReturn = true;
            return gamma;
        } else if(s instanceof FunctionInstanceStmt asFunInstance) {
            typeOf(gamma, asFunInstance.getFunctionInstanceExp());
            return gamma;
        } else if(s instanceof PrintStmt || s instanceof PrintlnStmt) {
            if(s instanceof PrintStmt stmt) {
                if(stmt.getValue() != null) {
                    if (!(typeOf(gamma, stmt.getValue()) instanceof BasicType)) {
                        throw new IllTypedException("Only basic type expression allowed in print(ln) statement");
                    }
                }
            } else {
                if(((PrintlnStmt) s).getValue() != null) {
                    if (!(typeOf(gamma, ((PrintlnStmt) s).getValue()) instanceof BasicType)) {
                        throw new IllTypedException("Only basic type expression allowed in print(ln) statement");
                    }
                }
            }
            return gamma;
        } else if(s instanceof IfStmt stmt) {
            Type conditionType = typeOf(gamma, stmt.getCondition());
            if(conditionType != BasicType.TYPE_BOOLEAN) {
                throw new IllTypedException("if condition should be boolean type.");
            } else {
                int temp = returnEvaluate;
                if(returnEvaluate < 0) {
                    returnEvaluate--;
                }
                typecheckBlockStmts(gamma, continueBreakOk, returnOk, stmt.getTrueBranch());
                typecheckBlockStmts(gamma, continueBreakOk, returnOk, stmt.getFalseBranch());
                if(returnEvaluate - temp < 1) {     // It means returnEvaluate does not add by 2, it means not all two branch in if has return
                    returnEvaluate = temp;
                }
                return gamma;
            }
        } else if(s instanceof SelfOperationStmt stmt) {
            Type variableType = typeOf(gamma, stmt.getSelfOperationExp());
            if(variableType != BasicType.TYPE_INT) {
                throw new IllTypedException("Only Int support ++, --");
            } else {
                return gamma;
            }
        } else {
            assert(false);
            throw new IllTypedException("Unknown statement");
        }
    }

    private static Map<Variable, Pair<Type, Boolean>> typecheckBlockStmts(Map<Variable, Pair<Type, Boolean>> gamma, boolean continueBreakOK, boolean returnOk, final BlockStmt blockStmt) throws IllTypedException {
        alreadyReturn = false;
        if(blockStmt != null) {
            Stmt s;
            for (int i = 0; i < blockStmt.getStmtList().size(); i++) {
                s = blockStmt.getStmtList().get(i);
                if (s instanceof FunctionDeclareStmt) {
                    throw new IllTypedException("Function declaration is not allowed in block");
                }
                gamma = typecheckStmt(gamma, continueBreakOK, returnOk, s);
                if(alreadyReturn) {
                    if(i < blockStmt.getStmtList().size() - 1) {
                        throw new IllTypedException("Statements after return cannot be reached in current block");
                    }
                    break;
                }
            }
            alreadyReturn = false;
            return gamma;
        } else {
            return gamma;
        }
    }

    private static Map<Variable, Pair<Type, Boolean>> newCopy(final Map<Variable, Pair<Type, Boolean>> gamma) {
        return new HashMap<>(gamma);
    }

    public static void typecheckProgram(final Program program) throws IllTypedException {
        List<Stmt> stmtList = program.getStmtList();
        Map<Variable, Pair<Type, Boolean>> gamma = new HashMap<>();
        if(funcMap == null || funcMap.size() > 0) {
            funcMap = new HashMap<>();
        }
        returnTypeFromFunc = null;
        returnEvaluate = 0;

        for(Stmt s : stmtList) {
            if(s instanceof FunctionDeclareStmt asFunDeclare) {
                List<Type> parameters = new ArrayList<>(asFunDeclare.getParameterList().values());
                if(!funcMap.containsKey(new Pair<>(asFunDeclare.getFuncName(), parameters))) {
                    funcMap.put(new Pair<>(asFunDeclare.getFuncName(), parameters), asFunDeclare);
                } else {
                    throw new IllTypedException("Function " + asFunDeclare.getFuncName().getName()
                        + "(" + parameters + ")" + " redefined");
                }
            }
        }

        for(Stmt s : stmtList) {
            gamma = typecheckStmt(gamma, false, false, s);
        }
    }
}
