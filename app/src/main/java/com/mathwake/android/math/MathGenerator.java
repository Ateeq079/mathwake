package com.mathwake.android.math;

import com.mathwake.android.model.AlarmDifficulty;

import java.util.Random;

public final class MathGenerator {
    private static final Random RANDOM = new Random(System.currentTimeMillis());

    private MathGenerator() {
    }

    public static MathProblem generate(AlarmDifficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return easy();
            case HARD:
                return hard();
            case MEDIUM:
            default:
                return medium();
        }
    }

    private static MathProblem easy() {
        switch (RANDOM.nextInt(3)) {
            case 0: {
                int a = nextInt(1, 50);
                int b = nextInt(1, 50);
                return new MathProblem(a + " + " + b + " = ?", a + b, AlarmDifficulty.EASY);
            }
            case 1: {
                int a = nextInt(20, 70);
                int b = nextInt(1, 20);
                return new MathProblem(a + " - " + b + " = ?", a - b, AlarmDifficulty.EASY);
            }
            default: {
                int a = nextInt(2, 10);
                int b = nextInt(2, 10);
                return new MathProblem(a + " x " + b + " = ?", a * b, AlarmDifficulty.EASY);
            }
        }
    }

    private static MathProblem medium() {
        switch (RANDOM.nextInt(3)) {
            case 0: {
                int a = nextInt(5, 24);
                int b = nextInt(2, 11);
                int c = nextInt(2, 11);
                return new MathProblem(a + " + " + b + " x " + c + " = ?", a + b * c, AlarmDifficulty.MEDIUM);
            }
            case 1: {
                int a = nextInt(10, 109);
                int b = nextInt(10, 109);
                int c = nextInt(5, 54);
                return new MathProblem(a + " + " + b + " + " + c + " = ?", a + b + c, AlarmDifficulty.MEDIUM);
            }
            default: {
                int a = nextInt(100, 300);
                int b = nextInt(10, 110);
                return new MathProblem(a + " - " + b + " = ?", a - b, AlarmDifficulty.MEDIUM);
            }
        }
    }

    private static MathProblem hard() {
        switch (RANDOM.nextInt(3)) {
            case 0: {
                int a = nextInt(5, 24);
                int b = nextInt(5, 24);
                int c = nextInt(3, 12);
                return new MathProblem("(" + a + " + " + b + ") x " + c + " = ?", (a + b) * c, AlarmDifficulty.HARD);
            }
            case 1: {
                int a = nextInt(3, 12);
                int b = nextInt(10, 60);
                return new MathProblem(a + "^2 + " + b + " = ?", a * a + b, AlarmDifficulty.HARD);
            }
            default: {
                int a = nextInt(3, 14);
                int b = nextInt(3, 14);
                int c = nextInt(2, 9);
                int d = nextInt(2, 9);
                return new MathProblem(a + " x " + b + " - " + c + " x " + d + " = ?", a * b - c * d, AlarmDifficulty.HARD);
            }
        }
    }

    private static int nextInt(int minInclusive, int maxInclusive) {
        return minInclusive + RANDOM.nextInt(maxInclusive - minInclusive + 1);
    }
}
