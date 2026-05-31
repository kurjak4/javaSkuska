package com.example.pexeso;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class State  implements Serializable {
    private static final long serialVersionUID = 1L;
    final int N = 8; // nemenime (2,4,6 je este ok)

    int[][] grid = new int[N][N]; // obsahuje iba obrazky
    boolean[][] shownCards = new boolean[N][N]; // 0 means hidden, 1 means shown
    boolean showHideBtn = true;

    int elapsedTime = 0;


    int firstRow = -1, firstCol = -1; // zapamatanie si prveho kliku
    boolean[][] matched = new boolean[N][N]; // true na karty co uz boli najdene
    int playerOnTurn = 1;
    int player1points = 0;
    int player2points = 0;
    final int PLAYERTIMEOUT = 15;
    boolean playerWon = false;


    public State() {
        // vyber nahodne obrazky co budu tvorit par (pre N > 8 to moze byt problem)
        int pairsToMake = N*N/2;
        List<Integer> cisla = new ArrayList<>();
        for (int i = 1; i < 40; i++) cisla.add(i);
        Collections.shuffle(cisla);
        List<Integer> obrazky = cisla.subList(0, pairsToMake);

        // vygeneruj pole kde bude kazda nahodna rozna karta 2krat
        List<Integer> karty = new ArrayList<>();
        for (int i = 0; i < pairsToMake; i++) {
            karty.add(obrazky.get(i));
            karty.add(obrazky.get(i));
        }
        Collections.shuffle(karty); // zamiešaj

        // naplň grid
        int k = 0;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                grid[i][j] = karty.get(k++);
            }
        }
    }

    public boolean playerWon() {
        for (boolean[] row : matched) {
            for (boolean val : row) {
                if (!val) return false;
            }
        }
        return true;
    }
}
