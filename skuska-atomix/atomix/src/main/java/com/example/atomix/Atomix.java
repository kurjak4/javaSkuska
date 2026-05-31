package com.example.atomix;

// load save, time klasika
// detekcia vyhry -> posun na novy level po nejakom delay  (funkcionalita next)
// grid reprezentuje layout
// ovladanie ako parkovisko, klik + sipka odchytenie mouse aj klik eventu
// skalovatelnost, citanie suboru

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Atomix extends Application {
    State state = new State(1);
    Label lbTime, lbWon;
    MojCanvas canvas;
    Timeline timeline;

    @Override
    public void start(Stage primaryStage) {
        HBox top = new HBox(10, new Label("Time:"), lbTime = new Label("0"));
        top.setAlignment(Pos.CENTER);

        canvas = new MojCanvas();
        Pane center = new Pane(canvas);
        canvas.widthProperty().bind(center.widthProperty());
        canvas.heightProperty().bind(center.heightProperty());

        Button btnSave = new Button("Save");
        Button btnLoad = new Button("Load");
        Button btnQuit = new Button("Quit");
        HBox bottom = new HBox(10, btnSave, btnLoad, btnQuit, lbWon = new Label());
        bottom.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(center);
        root.setBottom(bottom);

        Scene scene = new Scene(root, 600, 650, Color.GRAY);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Atomix");
        primaryStage.show();

        btnSave.setOnAction(e -> save());
        btnLoad.setOnAction(e -> load());
        btnQuit.setOnAction(e -> primaryStage.close());

        // spusti odpocet na zaciatku potom sa bude volat z next
        setTimeline();

        canvas.paint();
    }

    void setTimeline() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            state.timeLeft--;
            lbTime.setText("" + state.timeLeft);
            if (state.timeLeft <= 0) {
                timeline.stop();
                lbWon.setText("CAS VYPRSAL!");
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    void save() {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("save.dat"));
        ) {
            oos.writeObject(state);
            System.out.println("GAME SAVED");
        } catch (IOException e) { e.printStackTrace(); }
    }

    void load() {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("save.dat"))
        ) { // načita novy stav zo suboru, zohľadnime aj zmeny v GUI a možeš pokračovať v hre kde si skončil
            state = (State) ois.readObject();
            lbTime.setText("" + state.timeLeft);
            canvas.paint();
            System.out.println("GAME LOADED");
        } catch (IOException | ClassNotFoundException e) { e.printStackTrace(); }
    }

    void next() {
        if (state.levelId +1 > 3) {
            System.out.println("This is the last level");
            return;
        }
        int newLevelId = state.levelId +1;
        state = new State(newLevelId);
        lbWon.setText("");
        setTimeline();
        canvas.selectedAtomRow = -1;
        canvas.selectedAtomCol = -1;
        canvas.paint();
        System.out.println("SWITCHED TO NEXT LEVEL");
    }


    class MojCanvas extends Canvas {
        int selectedAtomRow = -1;
        int selectedAtomCol = -1;
        Map<Character, Image> img = new HashMap(Map.of(
           '<', new Image("file:images/HL.png"),
           '>', new Image("file:images/HR.png"),
           '^', new Image("file:images/HT.png"),
           'v', new Image("file:images/HB.png"),
           '-', new Image("file:images/OLR.png"),
           '|', new Image("file:images/OTB.png")
        ));

        MojCanvas() {
            setFocusTraversable(true);
            widthProperty().addListener(e -> paint());
            heightProperty().addListener(e -> paint());
            // MYS
            setOnMouseClicked(event -> {
                int col = getCol(event.getX());
                int row = getRow(event.getY());
                if (col < 0 || col >= state.COLS) return;
                if (row < 0 || row >= state.ROWS) return;

                char clicked = state.grid[row][col];
                // neklikol si na atom
                if (clicked == 'p' || clicked == 's' || clicked == 'u') {
                    selectedAtomRow = -1;
                    selectedAtomCol = -1;
                } else {
                    // klikol si na atom
                    selectedAtomRow = row;
                    selectedAtomCol = col;
                }
                paint();
                event.consume();
            });
            // KLAVESNICA
            setOnKeyPressed(e -> {
                if (selectedAtomCol == -1 || selectedAtomRow == -1) return;
                // Atom ostane selected kym nekliknes vedla, alebo na iny atom
                // move detekuje vyhru
                switch (e.getCode()) {
                    case RIGHT -> move( 1, 0);
                    case LEFT -> move(-1, 0);
                    case UP -> move(0, -1);
                    case DOWN -> move( 0, 1);
                    default -> System.out.println("Select an atom, then se arrow keys");
                }
                paint();
                e.consume();
            });
        }

        public void paint() {
            GraphicsContext gc = getGraphicsContext2D();
            gc.clearRect(0, 0, getWidth(), getHeight());

            // vykresli steny a blbosti
            for (int row = 0; row < state.ROWS; row++) {
                for (int col = 0; col < state.COLS; col++) {
                    double px = getPixelX(col);
                    double py = getPixelY(row);
                    char cc = state.grid[row][col];

                    if (cc == 'p') {
                        gc.setFill(Color.GREEN);
                        gc.fillRect(px, py, cellW(), cellH());
                    }
                    else if (cc == 'u') {
                        gc.setFill(Color.BEIGE);
                        gc.fillRect(px, py, cellW(), cellH());
                    }
                    else if (cc == 's') {
                        gc.setFill(Color.BLUE);
                        gc.fillRect(px, py, cellW(), cellH());
                    }
                }
            }
            // vykresli atomy
            for (int row = 0; row < state.ROWS; row++) {
                for (int col = 0; col < state.COLS; col++) {
                    char cc = state.grid[row][col];
                    double px = getPixelX(col);
                    double py = getPixelY(row);
                    if (cc != 'u' && cc != 'p' && cc != 's') {
                        gc.setFill(Color.BEIGE);
                        gc.fillRect(px, py, cellW(), cellH()); // je to png-cko pozadie bude vidno
                        gc.drawImage(img.get(cc), px, py, cellW(), cellH());
                    }
                }
            }
            // vykresli oznaceny atom
            if (selectedAtomRow != -1 && selectedAtomCol != -1) {
                gc.setFill(Color.BLACK);
                gc.setLineWidth(3);
                gc.strokeRect(getPixelX(selectedAtomCol), getPixelY(selectedAtomRow), cellW(), cellH());
                gc.setLineWidth(1);
            }
        }

        void move(int dx, int dy) {
            if (selectedAtomRow == -1 || selectedAtomCol == -1) return;
            // invariant: JE SELECNUTY NEJAKY ATOM (teda su nastavene selRow, selCol)

            // vypocitaj kam by sa atom presunul kym nenarazi
            int newRow = selectedAtomRow + dy;
            int newCol = selectedAtomCol + dx;
            while (newRow >= 0 && newRow < state.ROWS &&
                   newCol >= 0 && newCol < state.COLS) {
                if (state.grid[newRow][newCol] != 'u') break;
                newRow += dy;
                newCol += dx;
            }
            // na pozicii newRow newCol je objekt na ktory atom narazil, o krok spat je jeho nova pozicia
            newRow -= dy;
            newCol -= dx;

            // presun atom
            if (newRow == selectedAtomRow && newCol == selectedAtomCol) { // atom ostal tam kde bol
                ;
            } else { // presunul sa inde
                state.grid[newRow][newCol] = state.grid[selectedAtomRow][selectedAtomCol];
                state.grid[selectedAtomRow][selectedAtomCol] = 'u';
                selectedAtomRow = newRow;
                selectedAtomCol = newCol;
            }
            if (state.hotovo()) {
                lbWon.setText("VYHRAL SI!");
                timeline.stop();
                // po 3 sekundach spusti novy level
                new Timeline(new KeyFrame(Duration.seconds(3), e -> next())).play();
            }
        }

        // transformácie
        private double cellW() { return getWidth()  / state.COLS; }
        private double cellH() { return getHeight() / state.ROWS; }
        private int getCol(double px) { return (int)(px / cellW()); }
        private int getRow(double py) { return (int)(py / cellH()); }
        private double getPixelX(int col) { return col * cellW(); }
        private double getPixelY(int row) { return row * cellH(); }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
