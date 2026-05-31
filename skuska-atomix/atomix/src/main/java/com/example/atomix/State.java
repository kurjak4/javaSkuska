package com.example.atomix;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;

public class State implements Serializable {
    private static final long serialVersionUID = 1L;
    int COLS, ROWS;
    int levelId;

    char[][] grid;
    int timeLeft;

    public State(int levelId) {
        try {
            this.levelId = levelId;
            BufferedReader br = new BufferedReader(new FileReader("level"+levelId+".txt"));
            String[] parts1 = br.readLine().strip().split(" ");
            ROWS = Integer.parseInt(parts1[0]);
            COLS = Integer.parseInt(parts1[1]);
            grid = new char[ROWS][COLS];
            for (int i = 0; i < ROWS; i++) {
                String line = br.readLine().strip();
                for (int j = 0; j < COLS; j++) {
                    grid[i][j] = line.charAt(j);
                }
            }
            timeLeft = Integer.parseInt(br.readLine().strip()); // cas je extrahovany zo suboru

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hotovo() {
        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < COLS - 2; col++)
                if (grid[row][col] == '>' && grid[row][col+1] == '-' && grid[row][col+2] == '<')
                    return true;
        for (int row = 0; row < ROWS - 2; row++)
            for (int col = 0; col < COLS; col++)
                if (grid[row][col] == 'v' && grid[row+1][col] == '|' && grid[row+2][col] == '^')
                    return true;
        return false;
    }
}
