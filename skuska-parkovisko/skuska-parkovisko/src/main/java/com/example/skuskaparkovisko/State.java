package com.example.skuskaparkovisko;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;


// state si pamätá grid a všetky autá, skóre a prejdený čas
public class State implements Serializable {
    private static final long serialVersionUID = 1L;

    // v tomto pripade je COLS == ROWS lebo hra je štvorcova
    static int COLS, ROWS;
    Vehicle[] vehicles;
    int ourCarId; // id našho vozidla, doležita informacia
    int target; // toto je zaciatok targetu (to či je to stlpec alebo riadok zaleži od orientacie vozidla), jeho dlžka zaleži od dlžky vozidla
    int moves = 0;
    int levelId;

    int[][] grid;
    int score = 0;
    int elapsedTime = 0;

    public State(int levelId) {
        // Načitame počiatočnu konfiguraciu, predpokladam že vstup je určite korektny, nejdem to ošetrovať
        try {
            this.levelId = levelId;
            BufferedReader br = new BufferedReader(new FileReader("level"+levelId+".txt"));
            // nastav rozmery matice (gridu). matica zodpoveda presne poličkam (row,col) = grid[row][col]
            // pri gridPane pozor, lebo tam to je opačne v Gridpane ked davas add(cell, col,row) = grid[row][col]
            COLS = ROWS = Integer.parseInt(br.readLine());
            grid = new int[ROWS][COLS];

            // načitaj informacie o našom aute a kde ma byť
            String[] parts = br.readLine().strip().split(" ");
            this.ourCarId = Integer.parseInt(parts[0]); // id našho auta
            this.target = Integer.parseInt(parts[1]) -1; // stlpec, alebo riadok kde sa ma nachadzať auto

            // naplnime grid informaciami zo suboru
            for (int i = 0; i < ROWS; i++) {
                String line = br.readLine().strip();
                if (line.isEmpty()) break;
                String[] parts2 = line.split(" ");
                for (int j = 0; j < COLS; j++) {
                    this.grid[i][j] = Integer.parseInt(parts2[j]);
                }
            }

            // načitaj vehicles
            vehicles = new Vehicle[13]; // najviac 12 vehicles
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    int id = grid[row][col];
                    if (id == 0) continue; // prazdne poličko

                    // toto auto sme ešte nevideli
                    if (vehicles[id] == null) {
                        vehicles[id] = new Vehicle();
                        vehicles[id].id = id;
                        vehicles[id].row = row;
                        vehicles[id].col = col;
                        vehicles[id].length = 1;
                        if (id == this.ourCarId) vehicles[id].isOur = true; // ak je to naše auto čo chceme zaparkovať
                    } else { // auto už existuje
                        vehicles[id].length++; // predlžilo sa
                        if (col > vehicles[id].col) { // auto sa rozširuje vodorovne
                            vehicles[id].isOrientedVert = false;
                            continue;
                        }
                        if (row > vehicles[id].row) { // auto sa rozširuje dodola (vertikalne)
                            vehicles[id].isOrientedVert = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// Vehicle trieda reprezentuje jedno vozidlo
class Vehicle implements Serializable {
    private static final long serialVersionUID = 2L;
    public int id; // index auta
    public Boolean isOur = false; // či je toto auto čo chceme zaparkova´t
    public int row, col; // jeho grid pozicia, kde začina
    public int length; // (v poličkach) predpokladam že vždy je aspoň jeden z rozmerov rovný 1, teda stačí iba length
    public boolean isOrientedVert = false; // či je vozidlo orientovane vertikalne (da sa hybať iba hore dole) v opačnom pripade sa da hybať iba dolava doprava
}
