package com.github.wangdong20.kotlinscriptcompiler.parser.expressions;

import java.util.LinkedHashMap;

public class StringExp implements Exp {
    private String strWithoutInterpolation;
    private LinkedHashMap<Integer, Exp> interpolationExp;     // Integer is the interpolation expression position in String

    public StringExp(String strWithoutInterpolation, LinkedHashMap<Integer, Exp> interpolationExp) {
        this.strWithoutInterpolation = strWithoutInterpolation;
        this.interpolationExp = interpolationExp;
    }

    public String getStrWithoutInterpolation() {
        return strWithoutInterpolation;
    }

    public LinkedHashMap<Integer, Exp> getInterpolationExp() {
        return interpolationExp;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof StringExp exp) {
            if(exp.getStrWithoutInterpolation().equals(strWithoutInterpolation)) {
                if((exp.getInterpolationExp() == null && interpolationExp == null) ||
                        exp.getInterpolationExp().equals(interpolationExp))
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "StringExp{" +
                "strWithoutInterpolation='" + strWithoutInterpolation + '\'' +
                ", interpolationExp=" + interpolationExp +
                '}';
    }
}
