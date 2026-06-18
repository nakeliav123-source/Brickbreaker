package com.example.brickbreaker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.lifecycle.viewmodel.CreationExtras;

public class GameView extends SurfaceView implements Runnable {
    int bakex = 20;
    int bakey = 120;
    int bakew = 200;
    int bakeh = 80;
    private boolean showrecordtext;
    private long corenttime = 0;
    private SharedPreferences prefer;
    private int Highscore = 0;
    private boolean isdring = true;
    private Thread gameThread;
    private boolean isPlaying = true;

    private SurfaceHolder holder;
    private Paint paint;

    private int screenWidth, screenHeight;

    private Player player;
    private Ball ball;

    private int rows = 4;
    private int columns = 10;
    private Brick[][] bricks;
    private boolean showgameoverscreen = false ;
    private int score = 0;
    private int lives = 3;
    private boolean isGameOver = false;
    private int  currentLevel = 1;

    public GameView(Context context, int width, int height) {
        super(context);

        screenWidth = width;
        screenHeight = height;

        holder = getHolder();
        paint = new Paint();

        player = new Player(screenWidth / 2 - 150, screenHeight - 20, 300, 30);
        ball = new Ball(screenWidth / 2, screenHeight / 2, 20, 20, -20);

        createBricks();
        prefer = context.getSharedPreferences("Brick breaker", context.MODE_PRIVATE);
        Highscore = prefer.getInt("high score", 0);
    }

    // -----------------------------------
    // לוגיקה לעדכון מצב המשחק
    // -----------------------------------
    private void update() {
        if (isGameOver) return;

        // עדכון מיקום הכדור
        ball.x += ball.speedX;
        ball.y += ball.speedY;

        // התנגשויות עם הקירות
        if (ball.x -ball.radius< 0 || ball.x + ball.radius > screenWidth ) ball.speedX *= -1;
        if (ball.y - ball.radius< 0) ball.speedY *= -1;

        // פגיעה בשחקן
        if (ball.speedY > 0 &&
                ball.y + ball.radius >= player.y &&
                ball.y - ball.radius <= player.y + player.height &&
                ball.x >= player.x &&
                ball.x <= player.x + player.width) {

            ball.y = player.y - ball.radius;
            ball.speedY *= -1;
            int hitPos = ball.x - (player.x + player.width / 2);
            ball.speedX = hitPos / 10;

        }

        // איבוד חיים
        if (ball.y > screenHeight) {
            lives--;
            if (lives <= 0){
                isGameOver =true;
                showgameoverscreen =true;
                // שליפת ה-Activity הראשי כדי להריץ עליו את חלונית הדיאלוג
                if (getContext() instanceof android.app.Activity) {
                    ((android.app.Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // יצירת מסד הנתונים מקומית מתוך ה-Context
                            final DatabaseHelper dbHelper = new DatabaseHelper(getContext());

                            // יצירת תיבת טקסט לקלט שם השחקן
                            final android.widget.EditText input = new android.widget.EditText(getContext());
                            input.setHint("הכנס שם");

                            // שימוש במשתנה הניקוד שלך (אם קוראים לו score או scoreX, שנה בהתאם)
                           final int finalScore = score;
                            try {
                                // כאן שמתי ברירת מחדל 'score'. אם למשתנה הניקוד שלך בקלאס קוראים אחרת (למשל points), שנה אותו כאן

                            } catch (Exception e) {}

                            new android.app.AlertDialog.Builder(getContext())
                                    .setTitle("Game Over! המשחק נגמר")
                                    .setMessage("הכנס את שמך לטבלת השיאים:")
                                    .setView(input)
                                    .setCancelable(false) // השחקן חייב ללחוץ אישור
                                    .setPositiveButton("שמור", new android.content.DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(android.content.DialogInterface dialog, int which) {
                                            String name = input.getText().toString().trim();
                                            if (name.isEmpty()) name = "Player";

                                            // 1. שמירה למסד הנתונים
                                            final int scoreToSend = finalScore ;
                                            dbHelper.saveScore(name, scoreToSend);

                                            // 2. שליפת 10 השיאים הכי טובים
                                            java.util.List<String> topScores = dbHelper.getTop10Scores();
                                            StringBuilder leaderboard = new StringBuilder();
                                            for (String record : topScores) {
                                                leaderboard.append(record).append("\n");
                                            }

                                            // 3. הקפצת מסך טבלת השיאים
                                            new android.app.AlertDialog.Builder(getContext())
                                                    .setTitle("🏆 טבלת 10 הגדולים 🏆")
                                                    .setMessage(leaderboard.toString())
                                                    .setPositiveButton("סגור", null)
                                                    .show();
                                        }
                                    }).show();
                        }
                    });
                }

            }
            else resetBall();
        }
        boolean hitBrick = false;

// התנגשויות עם לבנים
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {

                Brick b = bricks[i][j];

                if (b.isAlive &&
                        ball.x + ball.radius >= b.x &&
                        ball.x+ ball.radius <= b.x + b.width &&
                        ball.y + ball.radius >= b.y &&
                        ball.y - ball.radius<= b.y + b.height) {

                    b.hit();
                    ball.speedY *= -1;

                    if (!b.isAlive) {
                        score += 10;
                    }

                    hitBrick = true;
                    break;
                }
            }

            if (hitBrick) break;
        }
        if (levelcompleted()) {
            currentLevel++;
            resetBall();
            createBricks();
            ball.speedY -= 2;
        }
        if (score > Highscore) {
            Highscore = score;
            savehighscore();
            shownewrecordmassge();
        }
    }

    private void resetBall() {
        ball.x = player.x + player.width / 2;
        ball.y = player.y - ball.radius;
        ball.speedX = (Math.random() > 0.5 ? 20 : -20);
        ball.speedY = -20;
    }



    // -----------------------------------
    // ציור המסך
    // -----------------------------------
    private void draw() {
        if (!holder.getSurface().isValid()) return;

        Canvas canvas = holder.lockCanvas();
        if(canvas==null)return;
        canvas.drawColor(Color.BLACK);

        paint.setColor(Color.BLUE);
        canvas.drawRect(bakex, bakey, bakex + bakew, bakey + bakeh, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(40);
        canvas.drawText("bake", bakex + 40, bakey + 55, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(50);
        canvas.drawText("Score: " + score, 50, 280, paint);
        canvas.drawText("Lives: " + lives, screenWidth - 250, 280, paint);

        if (showgameoverscreen) {
            paint.setColor(Color.BLACK);
            paint.setAlpha(200);
            canvas.drawRect(0,0,screenWidth,screenHeight,paint);
            paint.setColor(Color.WHITE);

            paint.setTextSize(100);
            canvas.drawText("GAME OVER", screenWidth / 2 - 300, screenHeight / 2, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(60);
            canvas.drawText("Tap to restart",screenWidth/2-250,screenHeight/2+150,paint);
        } else {
            // ציור שחקן
            canvas.drawRect(player.x, player.y, player.x + player.width, player.y + player.height, paint);

            // ציור כדור
            canvas.drawCircle(ball.x, ball.y, ball.radius, paint);

            // ציור לבנים

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    Brick b = bricks[i][j];
                    if (b.isAlive) {
                        paint.setColor(b.color);
                        canvas.drawRect(b.x, b.y, b.x + b.width, b.y + b.height, paint);
                    }
                }
            }
        }
        paint.setColor(Color.WHITE);
        paint.setTextSize(40);
        canvas.drawText("hight score" + Highscore, 500, 280, paint);

        if (showrecordtext) {
            paint.setTextSize(60);
            paint.setColor(Color.YELLOW);
            float textwithe = paint.measureText("new record");
            canvas.drawText("newrecord", (screenWidth - textwithe), 150, paint);
            if (System.currentTimeMillis() - corenttime > 2000) {
                showrecordtext = false;
            }
        }

        holder.unlockCanvasAndPost(canvas);
    }

    // -----------------------------------
    // יצירת הלבנים
    // -----------------------------------
    private void createBricks() {
        bricks = new Brick[rows][columns];
        int brickWidth = screenWidth / columns;
        int brickHeight = 80;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
              double r = Math.random();
              Bricktype type ;
              if(r<Math.max(0.05,0.2-currentLevel*0.02)) type = Bricktype.Empty;
              else if(r<0.7) type = Bricktype.NORMAL;
              else type = Bricktype.STRONG ;
              bricks[i][j] = new Brick(
                        j * brickWidth,
                        i * brickHeight + 300,
                        brickWidth - 5,
                        brickHeight - 5,
                      type
                );
            }
        }
    }

    // -----------------------------------
    // לולאת המשחק
    // -----------------------------------
    @Override
    public void run() {
        while (isPlaying) {
            update();
            draw();
            try {
                Thread.sleep(16);
            } catch (Exception ignored) {
            }
        }
    }

    public void resume() {
        isPlaying = true;
        if(gameThread==null||!gameThread.isAlive()){
            gameThread = new Thread(this);
            gameThread.start();

        }

    }

    public void pause() {
        isPlaying = false;
        try {
            gameThread.join();
        } catch (Exception ignored) {
        }
    }

    enum Bricktype {
        Empty,
        NORMAL,
        STRONG,
        UNBREAKABLE
    }

    // -----------------------------------
    // מחלקות פנימיות
    // -----------------------------------
    class Player {
        int x, y, width, height;

        Player(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    class Ball {
        int x, y;
        int radius;
        int speedX, speedY;

        Ball(int x, int y, int radius, int speedX, int speedY) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.speedX = speedX;
            this.speedY = speedY;
        }
    }

    class Brick {
        int x, y, width, height;
        Bricktype type ;
        boolean isAlive = true;
        int color;
        int hitleft ;

        Brick(int x, int y, int width, int height ,Bricktype type ) {
            this.x = x;
            this.y = y;
            this.type = type ;
            this.width = width;
            this.height = height;
           if(type==Bricktype.NORMAL) {
               hitleft = 1;
               color = Color.GREEN;
           }  else if(type==Bricktype.STRONG) {
               hitleft = 2;
               color = Color.RED;
           }else if(type==Bricktype.UNBREAKABLE){
               hitleft =3 ;
               color=Color.GRAY;
           }else{
               isAlive = false ;
           }
        }
        void hit (){
            hitleft-- ;
            if(hitleft<=0 ){
                isAlive = false;
            }
        }

    }

    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(showgameoverscreen) {
                    resetGame();

                    return true;
                }
                if (x >= bakex && x <= bakex + bakew && y >= bakey && y <= bakey + bakeh) {


                    ((MainActivity) getContext()).runOnUiThread(() -> {
                        ((MainActivity) getContext()).finish();
                    });


                }
                if (x >= player.x && x <= player.x + player.width &&
                        y >= player.y && y <= player.y + player.height) {
                    isdring = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isdring) {
                    player.x = (int) (x - player.width / 2);
                    if (player.x < 0) player.x = 0;
                    if (player.x + player.width > screenWidth)
                        player.x = screenWidth - player.width;

                }


                break;
            case MotionEvent.ACTION_UP:
                if (x >= bakex && x <= bakex + bakew && y >= bakey && y <= bakey + bakeh) {
                    ((MainActivity) getContext()).runOnUiThread(() -> {
                        ((MainActivity) getContext()).finish();
                    });


                        }
                isdring = false;
                break;


        }

        return true;

    }

    private void savehighscore() {
        SharedPreferences.Editor editor = prefer.edit();
        editor.putInt("high score", Highscore);
        editor.apply();
    }

    public void shownewrecordmassge() {
        showrecordtext = true;
        corenttime = System.currentTimeMillis();
    }
    private boolean levelcompleted(){
        for(int i = 0 ; i < rows; i++ ){
            for (int j = 0 ; j< columns ; j++){
               if (bricks[i][j].isAlive ){
                   return false ;
               }
            }
        }
        return  true;
    }

    private void resetGame() {
        score = 0;
        lives = 3;
        currentLevel = 1;

        isGameOver = false;
        showgameoverscreen =false;

        player.x = screenWidth / 2 - player.width / 2;

        resetBall();
        createBricks();
    }
}



