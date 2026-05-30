package com.example.pakoresan;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class State implements Serializable {
    private static final long serialVersionUID = 1L;

    int N;
    int[][] grid; // -1 means fixed, 0 means nothing, 1 means blue, 2 means red
    int levelId;
    int elapsedTime = 0;
    boolean isSolved = false;

    List<FixedRock> fixedRocks = new ArrayList<>();

    public State(int levelId) {
        try {
            this.levelId = levelId;
            // precitaj lines
            BufferedReader br = new BufferedReader(new FileReader("pakoreshon"+levelId+".txt"));
            ArrayList<String> lines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (!line.isEmpty()) lines.add(line);
            }
            br.close();

            // inicializuj grid a fixed kamene
            this.N = lines.size();
            this.grid = new int[N][N];

            for (int i = 0; i < N; i++) {
                String line2 = lines.get(i);
                String[] parts = line2.split("\\s+");
                for (int j = 0; j < N; j++) {
                    if (parts[j].equals(".")) continue;
                    int val = Integer.parseInt(parts[j]);
                    this.grid[i][j] = -1; // nastav ze je fixed, neklikame tam
                    fixedRocks.add(new FixedRock(i, j,
                            Math.abs(val),
                            (val < 0) ? 1 : 2));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isGameSolved() {
        for (FixedRock fr : fixedRocks) {
            if (!isRockSolved(fr)) return false;
        }
        return true;
    }

    public boolean isRockSolved(FixedRock fr) {
        boolean[][] seen = new boolean[N][N]; // inicializovane na false
        return dfs(fr.row, fr.col, fr.color, seen) == fr.value;
    }

    // spocitaj pocet spojenych (susediach) kamenov rovnakej farby
    private int dfs(int row, int col, int color, boolean[][] seen) {
        if (row < 0 || row >= N || col < 0 || col >= N) return 0; // ak si zasiel za hranice vetva umrie
        if (seen[row][col]) return 0; // ak si tu uz bol nevracaj sa

        // ak ma inu farbu ako potrebujeme vetva umrie (a pripocitame 0 ofc)
        if (grid[row][col] != -1) { // nieje to fixed kamen
            if (grid[row][col] != color) return 0; // farba obycajneho kamena sa nezhoduje nema zmysel dalej z neho
        } else { // je to fixed kamen, zistime jeho farbu ak nieje zhodna s tou ktoru potrebujeme tak nema zmysel ist dalej
            for (FixedRock fr : fixedRocks) {
                if (fr.row == row && fr.col == col) {
                    if (fr.color != color) return 0;
                }
            }
        }

        // aktualny kamen je korektny (ma spravnu farbu), pripocitame ho a ideme od neho dalej do vsetkych smerov
        seen[row][col] = true; // zaznac tah
        int sum = 1; // ak presiel checkmi tak ma spravnu farbu (a bol susedny), cize tento aktualny kamen sa rata do suctu
        sum += dfs(row - 1, col, color, seen); // chod do vsetkych styroch smerov
        sum += dfs(row + 1, col, color, seen);
        sum += dfs(row, col - 1, color, seen);
        sum += dfs(row, col + 1, color, seen);
        return sum;
    }
}

class FixedRock implements Serializable {
    private static final long serialVersionUID = 2L;

    public int row;
    public int col;
    public int value;
    public int color; // 1 means blue, 2 means red

    public FixedRock(int row, int col, int value, int color) {
        this.row = row;
        this.col = col;
        this.value = value;
        this.color = color;
    }
}