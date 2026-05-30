package com.example.pakoresan;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
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
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.util.Stack;

public class Pakoresan extends Application {
    State state = new State(1); // level 1
    Label lbTime;
    MojCanvas canvas;
    Timeline timeline;
    Stack<State> history = new Stack<>();
    boolean levelTransitionRunning = false;


    @Override
    public void start(Stage primaryStage){
        // top panel
        HBox top = new HBox(10, new Label("Time:"), lbTime = new Label("0"));
        top.setAlignment(Pos.CENTER);
        top.setStyle("-fx-background-color: yellow;");

        // ceter panel
        canvas = new MojCanvas();
        Pane center = new Pane(canvas);
        canvas.widthProperty().bind(center.widthProperty());
        canvas.heightProperty().bind(center.heightProperty());

        // bottom panel
        Button btnSave = new Button("Save");
        Button btnLoad = new Button("Load");
        Button btnUndo = new Button("Undo");
        Button btnQuit = new Button("Quit");
        HBox bottom = new HBox(10, btnSave, btnLoad, btnUndo, btnQuit);
        bottom.setAlignment(Pos.CENTER);
        bottom.setStyle("-fx-background-color: yellow;");

        // layout
        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(center);
        root.setBottom(bottom);

        Scene scene = new Scene(root, 600, 650, Color.BEIGE);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Pakoresan");
        primaryStage.show();

        // Button Handlers
        btnSave.setOnAction(e -> save());
        btnLoad.setOnAction(e -> load());
        btnQuit.setOnAction(e -> primaryStage.close());
        btnUndo.setOnAction(e-> undo()); // TODO: fix that history can be saved

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
    // Button handlers
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
    void next() {
        if (state.levelId +1 > 3) {
            System.out.println("This is the last level");
            return;
        }
        int newLevelId = state.levelId +1;
        state = new State(newLevelId); // v novom state je aj novy elapsed time takze toho sa netreba obavat

        history.clear();
        lbTime.setText("" + state.elapsedTime);
        canvas.paint();

        System.out.println("SWITCHED TO NEXT LEVEL");
    }

    void startNextLevel() {
        if (levelTransitionRunning) return;
        levelTransitionRunning = true;
        System.out.println("GRATULUJEM VYHRAL SI!!");
        System.out.println("SWITCHING TO NEXT LEVEL (if possible)");
        PauseTransition pause = new PauseTransition(Duration.seconds(10));
        pause.setOnFinished(e -> {
            levelTransitionRunning = false;
            next();
        });
        pause.play();
    }

    class MojCanvas extends Canvas {
        MojCanvas() {
            setFocusTraversable(true);
            widthProperty().addListener(e -> paint());
            heightProperty().addListener(e-> paint());

            setOnMouseClicked( event -> {
                int row = getRow(event.getY());
                int col = getCol(event.getX());
                if (col < 0 || col >= state.N) return;
                if (row < 0 || row >= state.N) return;
                if (state.grid[row][col] == -1) return; // na fixed kamene neklikame

                // uloz state PRED zmenami, abz si vedel robit undo
                history.push(deepCopy(state));

                // logika hry:
                state.grid[row][col] = (state.grid[row][col] +1) % 3;
                state.isSolved = state.isGameSolved(); // skontroluj ci hra nieje vyriesena
                paint();

                if (state.isSolved) {
                    startNextLevel();
                }

                event.consume();
            });
        }

        void paint() {
            GraphicsContext gc = getGraphicsContext2D();
            gc.setFill(state.isSolved ? Color.GREEN : Color.BEIGE); // nastav pozadie podla toho ci je hra vyriesena
            gc.fillRect(0, 0, getWidth(), getHeight());

            // nakresli vsetky obycajne kamene
            gc.setStroke(Color.BLACK);
            for (int row = 0; row < state.N; row++) {
                for (int col = 0; col < state.N; col++) {
                    if (state.grid[row][col] == -1) continue; // fixed kamene kreslime osobitne

                    double px = getPixelX(col);
                    double py = getPixelY(row);

                    gc.setLineWidth(5);
                    gc.strokeOval(px + 10, py + 10, cellW() - 20, cellH() - 20);
                    if (state.grid[row][col] == 0) gc.setFill(Color.WHITE);
                    else if (state.grid[row][col] == 1) gc.setFill(Color.BLUE);
                    else if (state.grid[row][col] == 2) gc.setFill(Color.RED);
                    gc.fillOval(px + 10, py + 10, cellW() - 20, cellH() - 20);
                }
            }

            // nakresli fixed kamene
            for (FixedRock fr : state.fixedRocks) {
                double px = getPixelX(fr.col);
                double py = getPixelY(fr.row);
                gc.setLineWidth(state.isRockSolved(fr) ? 20 : 5); // nastavit lineWidth podla toho ci je dany kamen korektny ci nie
                gc.strokeOval(px + 10, py + 10, cellW() - 20, cellH() - 20);
                gc.setFill(fr.color == 1 ? Color.BLUE : Color.RED);
                gc.fillOval(px + 10, py + 10, cellW() - 20, cellH() - 20);

                // nakresli cislo
                gc.setFill(Color.BLACK);
                gc.setFont(Font.font(cellH()*0.5));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setTextBaseline(VPos.CENTER);
                gc.fillText("" + fr.value, px + cellW() / 2, py + cellH() / 2);
                // ak potom by si inde kreslil text a nechces ho na stred, tak musis to odnastavit predtym lebo gc si to pamata
                // gc.setTextAlign(TextAlignment.LEFT); gc.setTextBaseline(VPos.BASELINE);
            }
        }

        // transformacie
        private double cellW() { return getWidth() / state.N;}
        private double cellH() { return getHeight() / state.N;}
        private int getCol(double px) { return (int)(px / cellW()); }
        private int getRow(double py) { return (int)(py / cellH()); }
        private double getPixelX(int col) { return col * cellW(); }
        private double getPixelY(int row) { return row * cellH(); }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
