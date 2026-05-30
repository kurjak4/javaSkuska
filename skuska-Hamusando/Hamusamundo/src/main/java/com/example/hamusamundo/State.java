package com.example.hamusamundo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;

public class State implements Serializable {
    private static final long serialVersionUID = 1L;
    int N;

    int[][] grid;
    int levelId;
    int elapsedTime = 0;
    int[] hintsRows;
    int[] hintsCols;
    boolean solved = false;

    // nacitanie vstupu a clenskzh premennych
    public State(int levelId) {
        try {
            this.levelId = levelId;
            BufferedReader br = new BufferedReader(new FileReader("hamusando" + levelId + ".txt"));
            N = Integer.parseInt(br.readLine().strip());
            grid = new int[N][N];
            hintsRows = new int[N];
            hintsCols = new int[N];

            // nacitaj hinty pre stlpce
            String[] parts = br.readLine().strip().split(",",-1); // -1 ak by bol na konci prazdny napriklad 1,2,,1, tak chces: "1","2","","1","". Bez tej -1 by ti to posledne "" mohlo vyhodit
            int i = 0;
            for (String s : parts) {
                if (!s.isEmpty()) {
                    hintsCols[i] = Integer.parseInt(s);
                } else {
                    hintsCols[i] = -1;
                }
                i++;
            }
            // nacitaj hinty pre riadky
            parts = br.readLine().strip().split(",");
            i = 0;
            for (String s : parts) {
                if (!s.isEmpty()) {
                    hintsRows[i] = Integer.parseInt(s);
                } else {
                    hintsRows[i] = -1;
                }
                i++;
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    public boolean checkSolved() {
        for (int i = 0; i < N; i++) {
            if (!checkRowSolved(i) || !checkColSolved(i)) return false;
        }
        return true;
    }

    public boolean checkRowSolved(int row) {
        int[] rowPole = grid[row];

        int pocetStvorcov = 0;
        for (int x : rowPole) {
            if (x == 2) pocetStvorcov++;
        }
        if (pocetStvorcov != 2) return false;

        // ak tam nieje cislo tak je jedno kolko je tam kruhov hlavne ze 2 stvorce aspon
        if (hintsRows[row] == -1) return true;

        int indexPrvehoStvorca = -1;
        int indexDruhehoStvorca = -1;
        for (int i = 0; i < N; i++) {
            if (rowPole[i] == 2) {
                if (indexPrvehoStvorca == -1) {
                    indexPrvehoStvorca = i;
                } else {
                    indexDruhehoStvorca = i;
                    break;
                }
            }
        }
        int pocetKruhov = 0;
        for (int i = indexPrvehoStvorca+1; i < indexDruhehoStvorca; i++) {
            if (rowPole[i] == 1) pocetKruhov++;
        }

        if (hintsRows[row] == pocetKruhov) return true;
        return false;
    }

    public boolean checkColSolved(int col) {
        int[] colPole = new int[N];
        int pocetStvorcov = 0;
        for (int i = 0; i < N; i++) {
            colPole[i] = grid[i][col];
            if (grid[i][col] == 2) pocetStvorcov++;
        }
        if (pocetStvorcov != 2) return false;
        if (hintsCols[col] == -1) return true;

        int indexPrvehoStvorca = -1;
        int indexDruhehoStvorca = -1;
        for (int i = 0; i < N; i++) {
            if (colPole[i] == 2) {
                if (indexPrvehoStvorca == -1) {
                    indexPrvehoStvorca = i;
                } else {
                    indexDruhehoStvorca = i;
                    break;
                }
            }
        }
        int pocetKruhov = 0;
        for (int i = indexPrvehoStvorca+1; i < indexDruhehoStvorca; i++) {
            if (colPole[i] == 1) pocetKruhov++;
        }

        if (hintsCols[col] == pocetKruhov) return true;
        return false;
    }



    // ------ Toto uz je cisto iba ten backtracking na zistenie ci existuje solution (can be useful later) -----
    // vies to vsetko vymazat a hra bude fungovat normalne v pohodicke
    public void solu() {
        int[][] copy = copyGrid();
        System.out.println(solve(copy, 0) ? "Game is still solveable" : "Game is no longer solveable");
    }

    private boolean solve(int[][] g, int pos) {
        // presli sme vsetky policka
        if (pos == N * N) {
            return isSolved(g);
        }

        int row = pos / N;
        int col = pos % N;

        // ak je tam uz nieco naklikane, nechaj to tak a chod dalej
        if (g[row][col] != 0) {
            return solve(g, pos + 1);
        }

        // skus nechat prazdne
        g[row][col] = 0;
        if (solve(g, pos + 1)) {
            return true;
        }

        // skus kruh
        g[row][col] = 1;
        if (solve(g, pos + 1)) {
            return true;
        }

        // skus stvorec
        g[row][col] = 2;
        if (noTooManySquares(g) && solve(g, pos + 1)) {
            return true;
        }

        // vrat naspat
        g[row][col] = 0;
        return false;
    }

    private boolean noTooManySquares(int[][] g) {
        for (int i = 0; i < N; i++) {
            int rowSquares = 0;
            int colSquares = 0;

            for (int j = 0; j < N; j++) {
                if (g[i][j] == 2) {
                    rowSquares++;
                }

                if (g[j][i] == 2) {
                    colSquares++;
                }
            }

            if (rowSquares > 2 || colSquares > 2) {
                return false;
            }
        }

        return true;
    }

    private boolean isSolved(int[][] g) {
        for (int i = 0; i < N; i++) {
            if (!rowSolved(g, i)) {
                return false;
            }
            if (!colSolved(g, i)) {
                return false;
            }
        }
        return true;
    }

    private boolean rowSolved(int[][] g, int row) {
        int squares = 0;

        for (int col = 0; col < N; col++) {
            if (g[row][col] == 2) {
                squares++;
            }
        }

        if (squares != 2) {
            return false;
        }

        if (hintsRows[row] == -1) {
            return true;
        }

        int first = -1;
        int second = -1;

        for (int col = 0; col < N; col++) {
            if (g[row][col] == 2) {
                if (first == -1) {
                    first = col;
                } else {
                    second = col;
                    break;
                }
            }
        }

        int circles = 0;

        for (int col = first + 1; col < second; col++) {
            if (g[row][col] == 1) {
                circles++;
            }
        }

        return circles == hintsRows[row];
    }

    private boolean colSolved(int[][] g, int col) {
        int squares = 0;

        for (int row = 0; row < N; row++) {
            if (g[row][col] == 2) {
                squares++;
            }
        }

        if (squares != 2) {
            return false;
        }

        if (hintsCols[col] == -1) {
            return true;
        }

        int first = -1;
        int second = -1;

        for (int row = 0; row < N; row++) {
            if (g[row][col] == 2) {
                if (first == -1) {
                    first = row;
                } else {
                    second = row;
                    break;
                }
            }
        }

        int circles = 0;

        for (int row = first + 1; row < second; row++) {
            if (g[row][col] == 1) {
                circles++;
            }
        }

        return circles == hintsCols[col];
    }

    private int[][] copyGrid() {
        int[][] copy = new int[N][N];

        for (int row = 0; row < N; row++) {
            for (int col = 0; col < N; col++) {
                copy[row][col] = grid[row][col];
            }
        }

        return copy;
    }
}
