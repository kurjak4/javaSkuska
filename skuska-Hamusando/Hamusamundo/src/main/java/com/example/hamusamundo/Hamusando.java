package com.example.hamusamundo;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.util.Stack;

public class Hamusando extends Application {
    State state = new State(3); // level 1
    Label lbTime;
    MojCanvas canvas;
    Timeline timeline;

    Stack<State> history = new Stack<>();

    @Override
    public void start(Stage primaryStage) {
        HBox top = new HBox(10,
                new Label("Time:"), lbTime = new Label("0")
                );

        // CANVAS
        canvas = new MojCanvas();
        Pane center = new Pane(canvas);
        canvas.widthProperty().bind(center.widthProperty());
        canvas.heightProperty().bind(center.heightProperty());

        // BOTTOM PANEL
        Button btnSave = new Button("Save");
        Button btnLoad = new Button("Load");
        Button btnUndo = new Button("Undo");
        Button btnQuit = new Button("Quit");
        Button btnSolu = new Button("isSol");
        HBox bottom = new HBox(10, btnSave, btnLoad, btnUndo, btnQuit, btnSolu);

        // LAYOUT
        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(center);
        root.setBottom(bottom);

        // vytvorime scenu, defaultne jej nastavime sirku X vysku na 600x650
        Scene scene = new Scene(root, 600, 650, Color.GRAY);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Hamusando");
        primaryStage.show();

        // Button Handlers
        btnSave.setOnAction(e -> save());
        btnLoad.setOnAction(e -> load());
        btnQuit.setOnAction(e -> primaryStage.close());
        btnUndo.setOnAction(e-> undo());
        btnSolu.setOnAction(e -> state.solu());

        // Timeline
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            state.elapsedTime++;
            lbTime.setText("" + state.elapsedTime);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // prvé prekreslenie
        canvas.paint();
    }
    // button handlers
    void save() {
        System.out.println("SAVING CONFIG");
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("save.dat"));
        ) {
            oos.writeObject(state);
            System.out.println("GAME SAVED");
        } catch (IOException e) { e.printStackTrace(); }
    }
    void load() {
        System.out.println("LOADING CONFIG");
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("save.dat"))
        ) { // načita novy stav zo suboru, zohľadnime aj zmeny v GUI a možeš pokračovať v hre kde si skončil
            state = (State) ois.readObject();
            lbTime.setText("" + state.elapsedTime);
            canvas.paint();
            System.out.println("GAME LOADED");
        } catch (IOException | ClassNotFoundException e) { e.printStackTrace(); }
    }
    void undo() {
        if (!history.isEmpty()) {
            // musim zabezpecit ze cas ostane rovnaky
            int currentTime = state.elapsedTime;
            state = history.pop();
            state.elapsedTime = currentTime;
            canvas.paint();
        } else {
            System.out.println("No moves made yet. Cant UNDO");
        }
    }
    State deepCopy(State original) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            new ObjectOutputStream(bos).writeObject(original);
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            return (State) new ObjectInputStream(bis).readObject();
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    // Vnorena trieda Canvas
    class MojCanvas extends Canvas {
        MojCanvas() {
            setFocusTraversable(true);
            widthProperty().addListener(e -> paint());
            heightProperty().addListener(e -> paint());

            setOnMouseClicked( event -> {
                int row = getRow(event.getY());
                int col = getCol(event.getX());
                if (col < 0 || col >= state.N) return;
                if (row < 0 || row >= state.N) return;

                // uloz state PRED zmenami, abz si vedel robit undo
                history.push(deepCopy(state));

                // logika hry:
                state.grid[row][col] = (state.grid[row][col] +1) % 3;

                //skontroluj ci je hra vyriesena, ak hej tak to zohladni v state
                if (state.checkSolved()) {
                    state.solved = true;
                    System.out.println("GRATULUJEM, VYHRAL SI!");
                }

                paint();
                event.consume();
            } );
        }

        void paint() {
            GraphicsContext gc = getGraphicsContext2D();
            // pozadie celej hracej plochy
            gc.setFill(state.solved ? Color.LIMEGREEN : Color.BEIGE);
            gc.fillRect(0, 0, getWidth(), getHeight());

            // nakresli hinty pre riadky
            // cellw / 2 bude fixna vec, lebo pri riadkoch chces aby boli v jednom stlpci pod sebou v strede
            // y-ove suradnice: hinty chceme aby boli zarovnane s riadkami takze ides normalne od 0
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font(cellH() * 0.5));
            for (int row = 0; row < state.N; row++) {
                if (state.hintsRows[row] != -1) {
                    // check ci zadal spravne, ak hej, urob text cerveny
                    if (state.checkRowSolved(row)) gc.setFill(Color.RED);
                    gc.fillText("" + state.hintsRows[row], cellW() / 2, getPixelY(row) + cellH() / 2);
                    gc.setFill(Color.BLACK);
                }
            }

            for (int col = 0; col < state.N; col++) {
                if (state.hintsCols[col] != -1) {
                    if (state.checkColSolved(col)) gc.setFill(Color.RED);
                    gc.fillText("" + state.hintsCols[col], getPixelX(col) + cellW()/2, cellH() / 2);
                    gc.setFill(Color.BLACK);
                }
            }
            // nakresli obrys gridu
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(10);
            gc.strokeRect(cellW(), cellH(), state.N*cellW(), state.N*cellH());
            gc.setFill(Color.WHITE);
            gc.fillRect(cellW()+5, cellH()+5, state.N*cellW()-15, state.N*cellH()-10); // vnutorny grid chceme biely

            // nakresli grid + obsah
            for (int row = 0; row < state.N; row++) {
                for (int col = 0; col < state.N; col++) {
                    double px = getPixelX(col);
                    double py = getPixelY(row);
                    gc.setLineWidth(1);
                    gc.strokeRect(px, py, cellW(), cellH());
                    // kruh
                    if (state.grid[row][col] == 1) {
                        gc.setFill(Color.PINK);
                        gc.fillOval(px + 10, py + 10, cellW() - 20, cellH() - 20);
                        gc.setStroke(Color.BLACK);
                        gc.setLineWidth(5);
                        gc.strokeOval(px + 10, py + 10, cellW() - 20, cellH() - 20);
                    }
                    // štvorec
                    else if (state.grid[row][col] == 2) {
                        gc.setFill(Color.GREEN);
                        gc.fillRect(px + 10, py + 10, cellW() - 20, cellH() - 20);
                        gc.setStroke(Color.BLACK);
                        gc.setLineWidth(5);
                        gc.strokeRect(px + 10, py + 10, cellW() - 20, cellH() - 20);
                    }
                }
            }
        }

        // transformácie upravene tak aby zohladnovali policka hintov
        private double cellW() { return getWidth()  / (state.N +1); } // delis N+1 lebo musis urobit miesto pre hinty
        private double cellH() { return getHeight() / (state.N +1); }
        // x-ova suradnica predelena cellW ti da konkretne policko
        // aby sme to mapovali v gride v state, tak odratavame jednotku
        // cize ak si klikol na pixel v row 0 tak si klikol na hint takze ti to vrati -1
        // ak si klikol na pixel v row 1 tak si klikol na stlpec 0 v gride preto vrati 0
        private int getCol(double px) { return (int)(px / cellW()) -1; }
        private int getRow(double py) { return (int)(py / cellH()) -1; }
        // pixely musime mapovat rovnako
        // ak zadas col -1 tak to znamena ze chces xovu suradnicu pixelu kde zacina hint
        private double getPixelX(int col) { return (col+1) * cellW(); }
        private double getPixelY(int row) { return (row+1) * cellH(); }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
