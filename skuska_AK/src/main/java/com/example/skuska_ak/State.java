package com.example.skuska_ak;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class State implements Serializable {
    private static final long serialVersionUID = 1L;
    int elapsedTime = 0;
    int moves = 0;
    int levelId;

    // 0 je volne policko
    // -1 je stena
    // farby su 1+ (aj steny pri checku kolizie sa to zohladni)
    // predpokladam ze vstupy su koretkne

    Block[] blocks;
    int[][] grid;
    int ROWS;
    int COLS;

    public State(int levelId) {
        this.levelId = levelId;
        try {
            BufferedReader br = new BufferedReader(new FileReader("blocks" + levelId + ".txt"));
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty()) break;
                lines.add(line);
            }
            String lineTemp = lines.getFirst();

            this.ROWS = lines.size();
            this.COLS = lineTemp.split("\\s+").length;
            grid = new int[ROWS][COLS];

            for (int row = 0; row < ROWS; row++) {
                String line2 = lines.get(row);
                String[] parts = line2.split("\\s+");
                for (int col = 0; col < COLS; col++) {
                    grid[row][col] = Integer.parseInt(parts[col]);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        blocks = new Block[13]; // najviac 13 blokov
        for (int row = 1; row < ROWS-1; row++) {
            for (int col = 1; col < COLS-1; col++) {
                int id = grid[row][col];
                if (id == 0) continue;

                if (blocks[id] == null) { // novy blok
                    blocks[id] = new Block();
                    blocks[id].id = id;
                    blocks[id].positions = new ArrayList<>();
                    blocks[id].positions.add(new int[]{row, col});
                } else { // blok uz sme videli
                    blocks[id].positions.add(new int[]{row, col});
                }
            }
        }

        // bloky su nacitane najdi obdlziky
        int count = 0;
        for (Block b : blocks) {
            if (b == null) continue;
            boolean isRect = true;
            for (int i = 0; i < b.positions.size(); i++) {
                int row = b.positions.get(i)[0];
                int col = b.positions.get(i)[1];

                for (int[] coord : b.positions) {
                    int r = coord[0];
                    int c = coord[1];
                    if (r != row && c != col) {
                        isRect = false;
                        break;
                    }  // tento urcite nieje obdlznik
                }
                if (!isRect) break;
            }
            if (isRect) count++;
        }
        System.out.println("Pocet obdlznikov = " + count);
    }
}

class Block implements Serializable {
    private static final long serialVersionUID = 2L;
    int id; // id je zaroven aj jeho farba
    List<int[]> positions; // vsetky policka ktore obsadzuje
}

