package com.mathwake.android.math;

import com.mathwake.android.model.AlarmDifficulty;

public class MathProblem {
    private final String expression;
    private final int answer;
    private final AlarmDifficulty difficulty;

    public MathProblem(String expression, int answer, AlarmDifficulty difficulty) {
        this.expression = expression;
        this.answer = answer;
        this.difficulty = difficulty;
    }

    public String getExpression() {
        return expression;
    }

    public int getAnswer() {
        return answer;
    }

    public AlarmDifficulty getDifficulty() {
        return difficulty;
    }
}
