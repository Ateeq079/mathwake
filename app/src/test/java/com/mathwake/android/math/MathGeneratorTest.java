package com.mathwake.android.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.mathwake.android.model.AlarmDifficulty;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MathGeneratorTest {

    @Test
    public void everyGeneratedProblem_answerMatchesExpression() {
        for (AlarmDifficulty difficulty : AlarmDifficulty.values()) {
            for (int i = 0; i < 500; i++) {
                MathProblem problem = MathGenerator.generate(difficulty);
                assertEquals("difficulty tag should match", difficulty, problem.getDifficulty());
                assertTrue("expression should pose a question",
                        problem.getExpression().contains("?"));
                int expected = evaluate(problem.getExpression());
                assertEquals("answer must equal the evaluated expression: " + problem.getExpression(),
                        expected, problem.getAnswer());
            }
        }
    }

    // --- A tiny arithmetic evaluator for the generator's expression format. ---

    private int evaluate(String rawExpression) {
        String expr = rawExpression.replace("= ?", "").replace(" ", "").replace("x", "*");
        // Expand "N^2" into the literal square so the parser only deals with + - * ( ).
        Matcher matcher = Pattern.compile("(\\d+)\\^2").matcher(expr);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            int base = Integer.parseInt(matcher.group(1));
            matcher.appendReplacement(sb, Integer.toString(base * base));
        }
        matcher.appendTail(sb);
        return new Parser(sb.toString()).parse();
    }

    /** Recursive-descent parser: expr = term (('+'|'-') term)*, term = factor ('*' factor)*. */
    private static final class Parser {
        private final String s;
        private int pos = 0;

        Parser(String s) {
            this.s = s;
        }

        int parse() {
            return expr();
        }

        private int expr() {
            int value = term();
            while (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) {
                char op = s.charAt(pos++);
                int rhs = term();
                value = op == '+' ? value + rhs : value - rhs;
            }
            return value;
        }

        private int term() {
            int value = factor();
            while (pos < s.length() && s.charAt(pos) == '*') {
                pos++;
                value *= factor();
            }
            return value;
        }

        private int factor() {
            if (s.charAt(pos) == '(') {
                pos++; // '('
                int value = expr();
                pos++; // ')'
                return value;
            }
            int start = pos;
            while (pos < s.length() && Character.isDigit(s.charAt(pos))) {
                pos++;
            }
            return Integer.parseInt(s.substring(start, pos));
        }
    }
}
