package com.example.skuska_ak;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Pos;
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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;


public class Bloky extends Application {
    State state = new State(6);
    Label lbTime, lbMoves;
    MojCanvas canvas = new MojCanvas();
    Timeline timeline;
    int selectedBlockId = -1;

    @Override
    public void start(Stage primaryStage) {
        HBox top = new HBox(10,
                new Label("Time:"),  lbTime  = new Label("0"),
                new Label("Moves: "), lbMoves = new Label("0"));
        top.setAlignment(Pos.CENTER);

        canvas = new MojCanvas();
        Pane center = new Pane(canvas);
        canvas.widthProperty().bind(center.widthProperty());
        canvas.heightProperty().bind(center.heightProperty());

        Button btnSave = new Button("Save");
        Button btnLoad = new Button("Load");
        Button btnQuit = new Button("Quit");
        Button btnNext = new Button("Next");
        Button btnPrev = new Button("Prev");
        HBox bottom = new HBox(10, btnSave, btnLoad, btnQuit, btnNext, btnPrev);
        bottom.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(center);
        root.setBottom(bottom);

        Scene scene = new Scene(root, 600, 650, Color.GRAY);
        primaryStage.setScene(scene);
        primaryStage.setTitle("BLOKY");
        primaryStage.show();

        btnSave.setOnAction(e -> save());
        btnLoad.setOnAction(e -> load());
        btnQuit.setOnAction(e -> primaryStage.close());
        btnNext.setOnAction(e -> next());
        btnPrev.setOnAction(e -> prev());

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            state.elapsedTime++;
            lbTime.setText("" + state.elapsedTime);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // prvé prekreslenie
        canvas.paint();
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
            lbTime.setText("" + state.elapsedTime);
            lbMoves.setText("" + state.moves);
            canvas.paint();
            System.out.println("GAME LOADED");
        } catch (IOException | ClassNotFoundException e) { e.printStackTrace(); }
    }
    void next() {
        if (state.levelId +1 > 6) {
            System.out.println("This is the last level");
            return;
        }
        int newLevelId = state.levelId +1;
        // v novom state je aj novy elapsed time takze toho sa netreba obavat
        state = new State(newLevelId);
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
        lbTime.setText("" + state.elapsedTime);
        lbMoves.setText("" + state.moves);
        canvas.paint();
        System.out.println("SWITCHED TO PREVIOUS LEVEL");
    }

    class MojCanvas extends Canvas {
        Color[] palette = {
                null, // index 0 = prázdne
                Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
                Color.PURPLE, Color.ORANGE, Color.CYAN, Color.BROWN,
                Color.PINK, Color.DARKGREEN, Color.NAVY, Color.MAROON
        };

        MojCanvas() {
            setFocusTraversable(true);
            widthProperty().addListener(e -> paint());
            heightProperty().addListener(e -> paint());

            setOnMouseClicked(event -> {
                int col = getCol(event.getX());
                int row = getRow(event.getY());
                if (col < 0 || col >= state.COLS) return; // ochrana
                if (row < 0 || row >= state.ROWS) return;

                // Vyber block
                int id = state.grid[row][col];
                if (id == 0 || id == -1) { // klikol na prazdne policko, alebo na stenu ignoruj
                    selectedBlockId = -1;
                } else {
                    selectedBlockId = id; // klikol na nejaky block
                }

                paint(); // v painte sa oznaci ako clicked
                event.consume();
            });
            setOnKeyPressed(e -> {
                if (selectedBlockId == -1) {
                    System.out.println("CLick on a block then use arrow keys to move!");
                    return;
                }

                Block b = state.blocks[selectedBlockId];
                switch (e.getCode()) { // jednotlive posuny
                    case RIGHT -> move(b, 1, 0);
                    case LEFT -> move(b, -1, 0);
                    case UP -> move(b, 0, -1);
                    case DOWN -> move(b, 0, 1);
                }
                paint();
                e.consume();
            });
        }

        void move(Block b, int dx, int dy) {
            // bloky este nevedia vychadzat von
            // System.out.println("Hybem sa");
            List<int[]> newCords = new ArrayList<>();
            boolean canMove = false;
            for (int[] coord : b.positions) {
                int row = coord[0];
                int col = coord[1];

                int newCol = col +dx;
                int newRow = row +dy;

                if (newCol < 0 || newRow < 0) return;
                if (newRow >= state.ROWS || newCol >= state.COLS) return;

                // skontroluj do coho by narazil keby sa posunul na newCol a newRow
                if (state.grid[newRow][newCol] != 0) {
                    if (state.grid[newRow][newCol] == -1) return; // narazil by na stenu

                    if (state.grid[newRow][newCol] != b.id) return; // narazil by na iny blok
                    else { // narazil by na sameho seba
                        // ak ide o okrajove policko tak vychadza z plochy
                        if (newRow == 0 || newRow == state.ROWS -1 || newCol == 0 || newCol == state.COLS-1) {
                            // proste ho tam nepridaj
                            continue;
                        } else {
                            newCords.add(new int[]{newRow, newCol});
                        }
                    }
                } else {
                    // ak je vsetko OK pridaj nove suradnice (ked narazi sam na seba alebo 0, nevadi)
                    newCords.add(new int[]{newRow, newCol});
                }

            }
            // vsetko preslo uspesne update grid
            // vymaz stare pozicie
            for (int[] coord : b.positions) {
                state.grid[coord[0]][coord[1]] = 0;
            }
            // vloz nove pozicie
            b.positions = newCords;
            for (int[] coord : b.positions) {
                state.grid[coord[0]][coord[1]] = b.id;
            }

            state.moves++;
            lbMoves.setText("" + state.moves);

            // checkWin
            boolean won = true;
            for (int row = 1; row < state.ROWS-1; row++) {
                for (int col = 1; col < state.COLS-1; col++) {
                    if (state.grid[row][col] != 0) {
                        won = false;
                        break;
                    }
                }
            }
            if (won) {
                System.out.println("Vyhral si!!!");
            }

        }

        void paint() {
            GraphicsContext gc = getGraphicsContext2D();
            gc.clearRect(0, 0, getWidth(), getHeight());

            // najprv SIMPLE grid to color
            for (int row = 0; row < state.ROWS; row++) {
                for (int col = 0; col < state.COLS; col++) {
                    double px = getPixelX(col);
                    double py = getPixelY(row);

                    if (state.grid[row][col] == -1) {
                        gc.setFill(Color.BLACK);
                        gc.fillRect(px, py, cellW(), cellH());
                    } else if (state.grid[row][col] == 0) {
                        gc.setStroke(Color.BLACK);
                        gc.setLineWidth(5);
                        gc.strokeRect(px, py, cellW(), cellH());
                    } else {
                        gc.setFill(palette[state.grid[row][col]]);
                        gc.fillRect(px, py, cellW(), cellH());
                    }
                }
            }

            // ak mas vybraty nejaky block tak ho vyznac
            if (selectedBlockId != -1) {
                Block b = state.blocks[selectedBlockId];
                for (int[] coords : b.positions) {
                    int row = coords[0];
                    int col = coords[1];
                    double px = getPixelX(col);
                    double py = getPixelY(row);
                    gc.setStroke(Color.BLACK);
                    gc.setLineWidth(3);
                    gc.strokeRect(px, py, cellW(), cellH());
                }
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
