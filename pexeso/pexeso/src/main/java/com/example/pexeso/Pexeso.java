package com.example.pexeso;

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
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;

public class Pexeso extends Application {
    State state = new State();
    Label lbTime, lbPlayer, lbScore;
    MojCanvas canvas;
    Timeline timeoutTimeline;
    int timeLeft = state.PLAYERTIMEOUT;
    Image[] images = new Image[40]; // index 0 bude null
    Button btnShow;

    @Override
    public void start(Stage primaryStage) {
        HBox top = new HBox(10,
                new Label("Hrac:"), lbPlayer = new Label("0"),
                new Label("Time:"), lbTime = new Label("0"),
                new Label("Score:"), lbScore = new Label("0:0")
        );
        top.setAlignment(Pos.CENTER);

        canvas = new MojCanvas();
        Pane center = new Pane(canvas);
        canvas.widthProperty().bind(center.widthProperty());
        canvas.heightProperty().bind(center.heightProperty());

        // bottom panel
        Button btnSave = new Button("Save");
        Button btnLoad = new Button("Load");
        Button btnQuit = new Button("Quit");
        btnShow = new Button("Show");

        HBox bottom = new HBox(10, btnSave, btnLoad, btnShow, btnQuit);
        bottom.setAlignment(Pos.CENTER);

        // layout
        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(center);
        root.setBottom(bottom);

        Scene scene = new Scene(root, 600, 650, Color.BEIGE);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Pexeso");
        primaryStage.show();

        btnSave.setOnAction(e -> save());
        btnLoad.setOnAction(e -> load());
        btnQuit.setOnAction(e -> primaryStage.close());
        btnShow.setOnAction(e-> show());

        canvas.paint();
        resetTimeout();
    }
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
            lbPlayer.setText("" + state.playerOnTurn);
            lbScore.setText(state.player1points + ":" + state.player2points);
            canvas.paint();
            System.out.println("GAME LOADED");
        } catch (IOException | ClassNotFoundException e) { e.printStackTrace(); }
    }
    void show() {
        if (state.showHideBtn) { // zobraz vsetky karty
            for (int row = 0; row < state.N; row++) {
                for (int col = 0; col < state.N; col++) {
                    state.shownCards[row][col] = true;
                }
            }
            canvas.paint();
        } else { // schovaj vsetky unmatched
            for (int row = 0; row < state.N; row++) {
                for (int col = 0; col < state.N; col++) {
                    if (!state.matched[row][col])
                        state.shownCards[row][col] = false;
                }
            }
            canvas.paint();
        }

        state.showHideBtn = !state.showHideBtn;
        this.btnShow.setText(state.showHideBtn ? "Show" : "Hide");
    }
    void resetTimeout() {
        if (timeoutTimeline != null) timeoutTimeline.stop();
        timeLeft = state.PLAYERTIMEOUT;

        // vytvori novy timeline, ktory ma nastavene ze bezi donekonecna
        // kazdu sekundu spusti dany event
        // akonahle vsak prejde 10 sekund tak sa sam resetuje (takze nebeyi actually donekonecna)
        timeoutTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e-> {
            lbTime.setText("" + timeLeft);
            timeLeft--;
            if (timeLeft <= 0) {
                // hracovi co je na rade vyprsal cas, ide druhy
                state.playerOnTurn = (state.playerOnTurn == 1 ? 2 : 1);
                // vynuluj jeho tah  (aj otoc naspat jeho kartu ak ju mal otocenu)
                if (state.firstRow != -1) {
                    state.shownCards[state.firstRow][state.firstCol] = false;
                    state.firstRow = -1;
                    state.firstCol = -1;
                }
                lbPlayer.setText("" + state.playerOnTurn);
                canvas.paint();
                resetTimeout();
            }
        }));
        timeoutTimeline.setCycleCount(Timeline.INDEFINITE);
        timeoutTimeline.play();
    }


    class MojCanvas extends Canvas {
        MojCanvas() {
            setFocusTraversable(true);
            widthProperty().addListener(e -> paint());
            heightProperty().addListener(e -> paint());

            // nacitaj obrazky teraz a potom k nim budes mat pristup stale
            for (int i = 1; i < 40; i++) {
                images[i] = new Image("file:images_renamed/" + i + ".gif");
            }

            setOnMouseClicked( event -> {
                int row = getRow(event.getY());
                int col = getCol(event.getX());
                if (col < 0 || col >= state.N) return;
                if (row < 0 || row >= state.N) return;
                if (state.matched[row][col]) return; // uz najdena
                if (state.shownCards[row][col]) return; // uz otocena

                state.shownCards[row][col] = true; // zobraz kartu ukaze sa v paint
                // prvy klik
                if (state.firstRow == -1) {
                    state.firstRow = row;
                    state.firstCol = col;
                    paint();
                } // druhy klik
                else {
                    paint(); // zobraz klik
                    int r = state.firstRow, c = state.firstCol;
                    state.firstRow = -1; state.firstCol = -1; // vynuluj prvy tah

                    if (state.grid[r][c] == state.grid[row][col]) { // zhoduju sa obrazky
                        state.matched[r][c] = state.matched[row][col] = true; // nastav ich na najdene
                        if (state.playerOnTurn == 1) state.player1points++; // zvys skore
                        else state.player2points++;
                        lbScore.setText(state.player1points + ":" + state.player2points);
                        if (state.playerWon()) {
                            state.playerWon = true;
                            paint();
                            return;
                        }
                        resetTimeout(); // na dalsi tah ma ten isty hrac znova 10 sekund

                    } else { // nezhoduju sa obrazky
                        // aby si nachvilu videl co si klikol (aj ked zle), tak je delay 2 sekundy
                        // prepne sa na dalsieho hraca (to obnasa aj resetovanie casu), ukaze skore
                        setDisable(true); // vypne canvas neda sa klikat, canvas nereaguje
                        new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                            // otoc obidva tahy zase naspat
                            state.shownCards[r][c] = false;
                            state.shownCards[row][col] = false;
                            // na rade je druhy hrac
                            state.playerOnTurn = (state.playerOnTurn == 1 ? 2 : 1);
                            lbPlayer.setText("" + state.playerOnTurn);
                            paint();
                            setDisable(false); // zapne canvas
                            resetTimeout();
                        })).play();
                    }
                }
                event.consume();
            });
        }

        public void paint() {
            GraphicsContext gc = getGraphicsContext2D();
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, getWidth(), getHeight());

            //vykresli grid, karty co su shown ukaz
            for (int row = 0; row < state.N; row++) {
                for (int col = 0; col < state.N; col++) {
                    double px = getPixelX(col);
                    double py = getPixelY(row);

                    // ak je viditelna zobraz kartu
                    if (state.shownCards[row][col]) {
                        gc.drawImage(images[state.grid[row][col]], px, py, cellW(), cellH());
                    } else {
                        gc.setStroke(Color.BLACK);
                        gc.strokeRect(px, py, cellW(), cellH());
                        gc.setFill(Color.GRAY);
                        gc.fillRect(px+5, py+5, cellW()-5, cellH()-5);
                    }
                }
            }
            if (state.playerWon) {
                gc.setFont(Font.font(1/state.N*cellW()));
                gc.fillText("Player: " + state.playerOnTurn + " WON!",
                        getWidth()/state.N, getHeight()/state.N);
                timeoutTimeline.stop();
            }
        }

        // transformacie
        private double cellW() { return getWidth() / state.N;}
        private double cellH() { return getHeight() / state.N;}
        private int getCol(double px) { return (int)(px / cellW()); }
        private int getRow(double py) { return (int)(py / cellH()); }
        private double getPixelX(int col) { return col * cellW(); }
        private double getPixelY(int row) { return row * cellH();}
    }

    public static void main(String[] args) {
        launch(args);
    }
}
