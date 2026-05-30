package com.example.skuskaparkovisko;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.*;
import java.util.Stack;


// ============================================================
// HLAVNÁ TRIEDA
// ============================================================
public class Parkovisko extends Application {
    State state = new State(3); // stav hry (oddelena logika od grafiky)
    Label lbScore, lbTime, lbMoves;  // ukazuju čas a skore
    MojCanvas canvas; // pamatame si referenciu na cavas
    Timeline timeline; // Timeline — pre odpočet času
    int selectedVehicleId = -1;

    Stack<byte[]> undoStack = new Stack<>();

    static Color[] palette = {
            null, // index 0 = prázdne
            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
            Color.PURPLE, Color.ORANGE, Color.CYAN, Color.BROWN,
            Color.PINK, Color.DARKGREEN, Color.NAVY, Color.MAROON
    };

    @Override
    public void start(Stage primaryStage) {
        // TOP panel skore a prejdeny čas
        HBox top = new HBox(10,
                new Label("Score:"), lbScore = new Label("0"),
                new Label("Time:"),  lbTime  = new Label("0"),
                new Label("Moves: "), lbMoves = new Label("0")
        );

        // --- CANVAS ---
        // vložime hlavny canvas do noveho Pane ktory bude v strede borderPaneu
        // obaľovaci Pane Center sa bude prisposobovať veľkosti center oblasti borderPane-u
        // potom nastavime na naš hlavny canvas bind() tak aby si canvas menil širku a vyšku podľa svojho obalovacieho pane
        // takže keď resize-neš scenu -> resizene sa borderPane -> resize center -> resize hlavny canvas
        canvas = new MojCanvas();
        Pane center = new Pane(canvas);
        canvas.widthProperty().bind(center.widthProperty());
        canvas.heightProperty().bind(center.heightProperty());

        // --- BOTTOM panel ---
        // vložime tlačidla do horizontalneho pane nech su vedľa seba na spodu hry
        Button btnSave = new Button("Save");
        Button btnLoad = new Button("Load");
        Button btnUndo = new Button("Undo");
        Button btnQuit = new Button("Quit");
        Button btnNext = new Button("Next");
        Button btnPrev = new Button("Prev");
        HBox bottom = new HBox(10, btnSave, btnLoad, btnUndo, btnQuit, btnNext, btnPrev);

        // --- LAYOUT ---
        // povkladame jednotlive kompomenty do hlavneho root Pane
        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(center);
        root.setBottom(bottom);

        // --- SCENE ---
        // vytvorime scenu, defaultne jej nastavime sirku X vysku na 600x650
        // vo vysledku jedno lebo ho možeme resize-ovať ako chceme a canvas bude vedieť aku ma širku vyšku dynamicky vždy
        Scene scene = new Scene(root, 600, 650, Color.GRAY);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Parkovisko");
        primaryStage.show();

        // --- BUTTON HANDLERS ---
        // každy button bude robiť presne to čo mu prikaže metoda
        btnSave.setOnAction(e -> save());
        btnLoad.setOnAction(e -> load());
        btnQuit.setOnAction(e -> primaryStage.close());
        btnNext.setOnAction(e -> next());
        btnPrev.setOnAction(e -> prev());

        // --- TIMELINE — každú sekundu tikne ---
        // timeline je globalny parameter
        // updatuje elapsedTime v stave hry
        // nastavime nech ide donekonečna a spustime
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            state.elapsedTime++;
            lbTime.setText("" + state.elapsedTime);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // prvé prekreslenie
        canvas.paint();
    }

    // --- SAVE ---
    void save() {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("save.dat"));
        ) {
            oos.writeObject(state);
            System.out.println("GAME SAVED");
        } catch (IOException e) { e.printStackTrace(); }
    }
    // --- LOAD ---
    void load() {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("save.dat"))
        ) { // načita novy stav zo suboru, zohľadnime aj zmeny v GUI a možeš pokračovať v hre kde si skončil
            state = (State) ois.readObject();
            lbScore.setText("" + state.score);
            lbTime.setText("" + state.elapsedTime);
            lbMoves.setText("" + state.moves);
            canvas.paint();
            System.out.println("GAME LOADED");
        } catch (IOException | ClassNotFoundException e) { e.printStackTrace(); }
    }
    void next() {
        if (state.levelId +1 > 5) {
            System.out.println("This is the last level");
            return;
        }
        int newLevelId = state.levelId +1;
        // v novom state je aj novy elapsed time takze toho sa netreba obavat
        state = new State(newLevelId);
        lbScore.setText("" + state.score);
        lbTime.setText("" + state.elapsedTime);
        lbMoves.setText("" + state.moves);
        canvas.paint();
        System.out.println("SWITCHED TO NEXT LEVEL");
    }
    void prev() {
        if (state.levelId -1 < 1) {
            System.out.println("This is the first level");
            return;
        }
        int newLevelId = state.levelId -1;
        state = new State(newLevelId);
        lbScore.setText("" + state.score);
        lbTime.setText("" + state.elapsedTime);
        lbMoves.setText("" + state.moves);
        canvas.paint();
        System.out.println("SWITCHED TO PREVIOUS LEVEL");
    }


    // Vnorena trieda Canvas
    class MojCanvas extends Canvas {
        MojCanvas() {
            setFocusTraversable(true);

            // pri zmene veľkosti automaticky prekreslí, lebo počuva na zmene svojej veľkosti
            widthProperty().addListener(e -> paint());
            heightProperty().addListener(e -> paint());

            setOnMouseClicked(event -> {
                int col = getCol(event.getX());
                int row = getRow(event.getY());
                if (col < 0 || col >= State.COLS) return; // ochrana
                if (row < 0 || row >= State.ROWS) return;

                // --- logika hry ---
                int id = state.grid[row][col];
                if (id != 0) selectedVehicleId = id;
                else selectedVehicleId = -1;
                paint();
                event.consume();
            });
            setOnKeyPressed(e -> {
                if (selectedVehicleId == -1) return;
                //System.out.println("Pressed key: " + e.getCode() + "\ton car with Id: " + selectedVehicleId);

                Vehicle v = state.vehicles[selectedVehicleId];
                switch (e.getCode()) {
                    case RIGHT -> move(v, 1, 0);
                    case LEFT -> move(v, -1, 0);
                    case UP -> move(v, 0, -1);
                    case DOWN -> move(v, 0, 1);
                }
                paint();
                e.consume();
            });
        }

        // transformácie
        private double cellW() { return getWidth()  / State.COLS; }
        private double cellH() { return getHeight() / State.ROWS; }
        private int getCol(double px) { return (int)(px / cellW()); }
        private int getRow(double py) { return (int)(py / cellH()); }
        private double getPixelX(int col) { return col * cellW(); }
        private double getPixelY(int row) { return row * cellH(); }

        // nakresli všetky auta
        void paint() {
            // vymaž stare platno, volanim getWidth/Height a nie nejakych hardcoded konštant to funguje pre dynamicky meniace-sa platno
            GraphicsContext gc = getGraphicsContext2D();
            gc.clearRect(0, 0, getWidth(), getHeight());

            // nakreslime všetky vehicles
            for (Vehicle v : state.vehicles) {
                if (v == null) continue;
                // lavy horny roh auta
                double px = getPixelX(v.col);
                double py = getPixelY(v.row);
                Color c = palette[v.id];
                gc.setStroke(c);
                gc.setFill(c);
                if (v.id == selectedVehicleId) {
                    gc.setStroke(Color.BLACK);
                    gc.setLineWidth(10);
                    gc.strokeRect(px + 2, py + 2,
                            v.isOrientedVert ? cellW()-4 : cellW()*v.length-4,
                            v.isOrientedVert ? cellH()*v.length-4 : cellH()-4);
                    gc.setLineWidth(1); // reset
                }

                if (v.isOrientedVert) {
                    gc.strokeRect(px, py, cellW(), cellH()*v.length);
                    gc.fillRect(px, py, cellW(), cellH()*v.length);
                } else {
                    gc.strokeRect(px, py, cellW()*v.length, cellH());
                    gc.fillRect(px, py, cellW()*v.length, cellH());
                }
                if (v.isOur) {
                    gc.setFill(Color.BLACK);
                    for (int i = 0; i < v.length; i++) {
                        double ox = px + (v.isOrientedVert ? 0 : i) * cellW();
                        double oy = py + (v.isOrientedVert ? i : 0) * cellH();
                        gc.fillOval(ox + cellW()*0.2, oy + cellH()*0.2,
                                cellW()*0.6, cellH()*0.6);
                    }
                }
            }

            // nakresli target polička
            Vehicle ourV = state.vehicles[state.ourCarId];
            int targetCol, targetRow;
            if (ourV.isOrientedVert) {
                targetCol = ourV.col;
                targetRow = state.target;
            } else {
                targetCol = state.target;
                targetRow = ourV.row;
            }
            double px = getPixelX(targetCol);
            double py = getPixelY(targetRow);
            int length = ourV.length;
            gc.setStroke(Color.BLACK);
            for (int i = 0; i < length; i++) {
                double ox = px + (ourV.isOrientedVert ? 0 : i) * cellW();
                double oy = py + (ourV.isOrientedVert ? i : 0) * cellH();
                gc.strokeOval(ox + cellW()*0.2, oy + cellH()*0.2,
                                cellW()*0.6, cellH()*0.6);
            }
        }

        void move(Vehicle v, int dx, int dy) {
            if (!v.isOrientedVert && dy != 0) return; // horizontálne auto sa nehýbe hore/dole
            if (v.isOrientedVert  && dx != 0) return; // vertikálne auto sa nehýbe doľava/doprava

            // predpokladame že niesu štvorcove auta
            // ak je orientovany dodola a chce ist po xovej osi tak nemože
            // ak je orientovany doprava a chce ist po yovej osi tak nemože
            int newCol = v.col + dx;
            int newRow = v.row +dy;

            if (newCol < 0 || newRow < 0) return;
            if (!v.isOrientedVert && newCol + v.length > State.COLS) return;
            if (v.isOrientedVert  && newRow + v.length > State.ROWS) return;

            // skontroluj kolíziu cez grid
            // pohyb doprava — skontroluj pravý okraj
            if (dx == 1 && state.grid[v.row][v.col + v.length] != 0) return;
            // pohyb doľava — skontroluj ľavý okraj
            if (dx == -1 && state.grid[v.row][newCol] != 0) return;
            // pohyb dolu — skontroluj spodný okraj
            if (dy == 1 && state.grid[v.row + v.length][v.col] != 0) return;
            // pohyb hore — skontroluj horný okraj
            if (dy == -1 && state.grid[newRow][v.col] != 0) return;

            // aktualizuj grid — vymaž staré políčka
            for (int i = 0; i < v.length; i++) {
                if (v.isOrientedVert) state.grid[v.row + i][v.col] = 0;
                else                  state.grid[v.row][v.col + i] = 0;
            }

            // posuň vehicle
            v.col = newCol;
            v.row = newRow;
            state.moves++;
            lbMoves.setText("" + state.moves);
            // aktualizuj grid — zapíš nové políčka
            for (int i = 0; i < v.length; i++) {
                if (v.isOrientedVert) state.grid[v.row + i][v.col] = v.id;
                else                  state.grid[v.row][v.col + i] = v.id;
            }

            // zisti či si nevyhral
            if (v.isOur) {
                if (!v.isOrientedVert && v.col == state.target ||
                     v.isOrientedVert && v.row == state.target) {
                    System.out.println("VYHRAL SI!");
                    // zastav timer
                    PauseTransition pause = new PauseTransition(Duration.seconds(5));
                    pause.setOnFinished(e -> next());
                    pause.play();
                    // next(); tymto si niesom isty
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}