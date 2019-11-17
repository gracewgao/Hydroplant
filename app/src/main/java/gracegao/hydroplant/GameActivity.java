package gracegao.hydroplant;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import uk.co.deanwild.materialshowcaseview.IShowcaseListener;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

// This class is an Activity for playing the video game
public class GameActivity extends AppCompatActivity {

    // Views
    private FrameLayout screen, gameFrame;
    private RelativeLayout startLayout, pauseLayout;
    private int frameHeight, frameWidth;
    private FloatingActionButton playButton, pauseButton;
    private ImageView plant, acid, rain, heart, bgImage, cloud1, cloud2, cloud3;
    private TextView scoreLabel, highScoreLabel, titleLabel;

    // Image resources
    private Drawable skyBg, plantBg, plantAlive, plantDead;

    // Size and position of objects
    private int plantHeight, plantWidth;
    private float plantX, plantY;
    private float acidX, acidY;
    private float rainX, rainY;
    private float heartX, heartY;

    // Status of game elements
    private boolean started = false;
    private boolean touch_right = false;
    private boolean touch_left = false;
    private boolean isHeart = false;
    private int score, highScore, timeCount, health=2;
    float acidSpeed, rainSpeed, plantSpeed, heartSpeed, acceleration, friction;

    // Other variables
    private Timer timer;
    private Handler handler = new Handler();
    // For playing the background music
    private SoundPlayer soundPlayer;
    private MediaPlayer mediaPlayer;
    // For phone storage
    private SharedPreferences settings;

    // When the Activity is created (similar to the main method)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        soundPlayer = new SoundPlayer(this);

        // Assigns variables to each view
        screen = findViewById(R.id.screen);
        gameFrame = findViewById(R.id.gameFrame);
        startLayout = findViewById(R.id.startLayout);
        pauseLayout = findViewById(R.id.pauseLayout);
        plant = findViewById(R.id.plant);
        acid = findViewById(R.id.acid);
        rain = findViewById(R.id.rain);
        heart = findViewById(R.id.heart);
        scoreLabel = findViewById(R.id.scoreLabel);
        titleLabel = findViewById(R.id.titleLabel);
        highScoreLabel = findViewById(R.id.highScoreLabel);
        playButton = findViewById(R.id.playButton);
        pauseButton = findViewById(R.id.pauseButton);
        bgImage = findViewById(R.id.bg);
        cloud1 = findViewById(R.id.cloud1);
        cloud2 = findViewById(R.id.cloud2);
        cloud3 = findViewById(R.id.cloud3);

        // Hides pause button on starting screen
        pauseButton.hide();

        // Retrieves image resources
        plantAlive = getResources().getDrawable(R.drawable.alive_plant);
        plantDead = getResources().getDrawable(R.drawable.dead_plant);
        skyBg = getResources().getDrawable(R.drawable.sky_bg);
        plantBg = getResources().getDrawable(R.drawable.plant_bg);

        // Retrieves (from phone storage) and displays high score
        settings = getSharedPreferences("HYDROPLANT", Context.MODE_PRIVATE);
        highScore = settings.getInt("HIGH_SCORE", 0);
        highScoreLabel.setText("best score : " + highScore);

        // Starts the game background music
        mediaPlayer= MediaPlayer.create(GameActivity.this,R.raw.music);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    // If the user switches to another app
    @Override
    protected void onPause() {
        super.onPause();
        // Pauses the game if the game is running
        if (started)
            pauseGame();
        // Pauses the background music
        mediaPlayer.pause();
    }

    // If the user returns to the app from another app
    @Override
    protected void onResume() {
        super.onResume();
        // Resumes background music
        mediaPlayer.start();
    }

    // Updates position of game elements
    public void changePos() {

        // Adds to time elapsed
        timeCount += 20;

        // CLOUDS
        // Cloud moving right
        float cloud1X = cloud1.getX();
        if(cloud1X<screen.getWidth())
            cloud1.setX(cloud1X+2);
        else {
            cloud1.setX(0 - cloud1.getWidth());
            cloud1.setAlpha((float)(Math.random()/2 + 0.5));
        }
        // Cloud moving left
        float cloud2X = cloud2.getX();
        if(cloud2X > 0-cloud2.getWidth())
            cloud2.setX(cloud2X-4);
        else {
            cloud2.setX(screen.getWidth());
            cloud2.setAlpha((float)(Math.random()/2 + 0.5));
        }
        // Cloud moving left
        float cloud3X = cloud3.getX();
        if(cloud3X > 0-cloud3.getWidth())
            cloud3.setX(cloud3X-2);
        else {
            cloud3.setX(screen.getWidth());
            cloud3.setAlpha((float)(Math.random()/2 + 0.5));
        }

        // ACCELERATION
        // Speed of moving elements gradually gets faster to make the game harder
        // Sets a maximum speed for the game based on the acid's speed
        if (acidSpeed < 60) {
            acidSpeed += 0.005;
            rainSpeed += 0.004;
            heartSpeed += 0.003;
            plantSpeed += 0.003;
        }

        // RAINDROPS
        // Moves down vertically
        rainY += rainSpeed;
        // Finds position of raindrops
        float rainCenterX = rainX + rain.getWidth() / 2;
        float rainCenterY = rainY + rain.getHeight() / 2;
        // If raindrop falls onto plant
        if (hitCheck(rainCenterX, rainCenterY, rain.getWidth(), rain.getHeight())) {
            // Moves element out of frame (disappears off the screen)
            rainY = frameHeight + 100;
            // Adds 1 to the current score
            score++;
            soundPlayer.playRainSound();
        }
        // Creates a new raindrop after the previous raindrop has fallen past the frame
        if (rainY > frameHeight) {
            rainY = -100;
            rainX = (float) Math.floor(Math.random() * (frameWidth - rain.getWidth()));
        }
        // Displays raindrop on screen
        rain.setX(rainX);
        rain.setY(rainY);

        // HEARTS
        // Creates a new heart if there is currently no heart on screen and 10 seconds has passed since the last heart
        if (!isHeart && timeCount % 10000 == 0) {
            // Heart is on screen
            isHeart = true;
            // Sets initial position of heart
            heartY = -20;
            heartX = (float) Math.floor(Math.random() * (frameWidth - heart.getWidth()));
        }
        // If there is a heart on the screen
        if (isHeart) {
            // Moves down vertically
            heartY += heartSpeed;
            // Finds the position of the heart
            float heartCenterX = heartX + heart.getWidth() / 2;
            float heartCenterY = heartY + heart.getHeight() / 2;
            // If heart falls onto plant
            if (hitCheck(heartCenterX, heartCenterY, heart.getWidth(), heart.getHeight())) {
                // Moves heart out of frame (disappears off the screen)
                heartY = frameHeight + 100;
                // Adds to the score
                score += 3;
                // Plays sound effect
                soundPlayer.playHeartSound();
                // Changes plant image
                plant.setImageDrawable(plantAlive);
                // Replenishes health
                health=2;
            }
            // If the heart falls past the frame, there is no heart on the screen
            if (heartY > frameHeight)
                isHeart = false;
            // Sets new position of heart
            heart.setX(heartX);
            heart.setY(heartY);
        }

        // ACID RAIN
        // Moves down vertically at the current speed
        acidY += acidSpeed;
        // Position of acid rain
        float acidCenterX = acidX + acid.getWidth() / 2;
        float acidCenterY = acidY + acid.getHeight() / 2;
        // If the plant is hit by acid rain
        if (hitCheck(acidCenterX, acidCenterY, acid.getWidth(), acid.getHeight())) {
            acidY = frameHeight + 100;
            // Plays a sound
            soundPlayer.playAcidSound();
            // Changes plant image
            plant.setImageDrawable(plantDead);
            // Subtract from health
            health--;
            // If the plant has no health
            if (health<=0){
                gameOver();
            }
        }
        // If acid has fallen out of frame, a new one is regenerated
        if (acidY > frameHeight) {
            acidY = -100;
            acidX = (float) Math.floor(Math.random() * (frameWidth - acid.getWidth()));
        }
        // Changes position of the element
        acid.setX(acidX);
        acid.setY(acidY);

        // USER INPUT AND GESTURES
        // Moves plant if user if touching the screen
        if (touch_right) {
            // Moves right
            plantX += plantSpeed;
        } else if (touch_left){
            // Moves left
            plantX -= plantSpeed;
        }
        // Sets min and max x-coordinates if plant hits left or right borders
        if (plantX < 0) {
            plantX = 0;
        } else if (plantX > frameWidth - plantWidth) {
            plantX = frameWidth - plantWidth;
        }
        // Sets x-coordinate of plant
        plant.setX(plantX);

        // Updates and displays score
        scoreLabel.setText(String.valueOf(score));
    }

    // Method to check if an object collides with the plant
    private boolean hitCheck(float x, float y, float width, float height) {
        // Checks if x-coordinates and y-coordinates overlap with plant coordinates
        float xLeft = x-(width/2);
        float xRight = x+(width/2);
        float yBottom = y+(height/2);
        boolean xHit = (plantX <= xLeft && xLeft <= plantX + plantWidth) || (plantX + plantWidth >= xRight && xRight >= plantX);
        boolean yHit = plantY <= y && yBottom <= frameHeight;
        // Return true if there is both an x and y overlap
        if (xHit && yHit)
            return true;
        // Otherwise return false
        return false;
    }

    // Method to end the current game
    private void gameOver() {
        // Game is no longer running
        started = false;

        // Stops timer
        timer.cancel();
        timer = null;

        // Sleeps 1 second before "game over" screen is shown
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Hides game components
        gameFrame.setVisibility(View.INVISIBLE);
        pauseLayout.setVisibility(View.INVISIBLE);
        pauseButton.hide();
        // Changes background and title text
        bgImage.setImageDrawable(plantBg);
        titleLabel.setText("game over");
        // Makes "game over" screen visible
        startLayout.setVisibility(View.VISIBLE);

        // Updates high score if higher than previous high score
        if (score > highScore) {
            highScore = score;
            highScoreLabel.setText("best score: " + highScore);
            // Stored high score in phone storage
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt("HIGH_SCORE", highScore);
            editor.commit();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // If the game is running
        if (started) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // Gets x-coordinate where screen is touched
                float x = event.getX();
                if (x>(screen.getWidth()/2)){
                    // If the left half of the screen is touched
                    touch_right = true;
                } else {
                    // If the right half of the screen is touched
                    touch_left = true;
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                // If the screen is not being touched
                touch_left = false;
                touch_right = false;
            }
        }
        return true;
    }

    private void gameTutorial(){
        int green = Color.argb(255,49, 113, 48);
        int black = Color.argb(255, 0, 0, 0);
        int white = Color.argb(240, 255, 255, 255);

        MaterialShowcaseView gameView = new MaterialShowcaseView.Builder(this)
                .setTarget(plant)
                .setTitleText(Html.fromHtml("<b>how to play</b>"))
                .setContentText(Html.fromHtml("<p>Tap and hold the left/right side of the screen to move in that direction.</p><p>Earn points by collecting raindrops (+1) and hearts (+3).</p>" +
                        "<p>Avoid acid rain, catching two consecutive drops will end the game.</p><p> Catching a heart will restore your plant's health.</p><p>Tap anywhere to start!</p>"))
                .setListener(new IShowcaseListener() {
                         @Override
                         public void onShowcaseDisplayed(MaterialShowcaseView showcaseView) {
                             // Sets x-coordinate of plant
                             plant.setX(plantX);
                         }
                         @Override
                         public void onShowcaseDismissed(MaterialShowcaseView showcaseView) {
                             // Sets the game status to running
                             started = true;
                         }
                     }
                )
                .setMaskColour(white)
                .setContentTextColor(black)
                .setTitleTextColor(green)
                .setDismissTextColor(green)
                .setShapePadding(100)
                .setDismissOnTouch(true)
                .build();
        gameView.setPadding(84,84, 84, 84);
        gameView.show(this);
    }

    // Method to start a new game
    public void startGame(View view) {
        started = true;

        // Removes other layout
        startLayout.setVisibility(View.GONE);
        // Changes background image
        bgImage.setImageDrawable(skyBg);
        // Makes game layout and score label visible
        scoreLabel.setVisibility(View.VISIBLE);
        cloud1.setVisibility(View.VISIBLE);
        cloud2.setVisibility(View.VISIBLE);
        cloud3.setVisibility(View.VISIBLE);
        gameFrame.setVisibility(View.VISIBLE);
        pauseButton.show();

        // Measures game dimensions
        if (frameHeight == 0) {
            // Dimensions of frame
            frameHeight = gameFrame.getHeight();
            frameWidth = gameFrame.getWidth();
            // Dimensions of plant
            plantHeight = plant.getHeight();
            plantWidth = plant.getWidth();
            // Sets the plant starting position to center
            plantX = (float)frameWidth/2 - (float)plantWidth/2;
            plantY = plant.getY();
        }

        // Sets random starting positions for the clouds
        cloud1.setX((float)Math.random() * frameWidth);
        cloud2.setX((float)Math.random() * frameWidth);
        cloud3.setX((float)Math.random() * frameWidth);
        // Sets random opacity from 0.5 to 1
        cloud1.setAlpha((float)(Math.random()/2 + 0.5));
        cloud2.setAlpha((float)(Math.random()/2 + 0.5));
        cloud3.setAlpha((float)(Math.random()/2 + 0.5));

        // Sets y-coordinates of falling elements to somewhere off the screen
        acid.setY(3000.0f);
        rain.setY(3000.0f);
        heart.setY(3000.0f);
        // Stores y-coordinates of elements
        acidY = acid.getY();
        rainY = rain.getY();
        heartY = heart.getY();

        // Resets game elements
        plant.setImageDrawable(plantAlive);
        health = 2;
        acidSpeed = 15;
        rainSpeed = 13;
        heartSpeed = 20;
        plantSpeed = 10;
        timeCount = 0;
        score = 0;
        scoreLabel.setText("0");

        if (highScore==0){
            started=false;
            gameTutorial();
        }

        // Sets timer to run the game, updates state every 20 milliseconds
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (started) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // Updates game state
                            changePos();
                        }
                    });
                }
            }
        }, 0, 20);
    }

    // Method to pause game when pause button is pressed
    public void pauseGame(){
        // Pauses game state
        started = false;
        // Hides the pause button and displays the pause screen layout
        pauseButton.hide();
        pauseLayout.setVisibility(View.VISIBLE);
    }

    // Overloading method to pause game when pause button is pressed (onClick property of a button requires a method with View parameters)
    public void pauseGame(View view) {
        pauseGame();
    }

    // Method to resume paused game when play button is pressed
    public void resumeGame(View view){
        // Resumes game
        started = true;
        // Displays the pause button and hides the pause screen layout
        pauseButton.show();
        pauseLayout.setVisibility(View.INVISIBLE);
    }

    // Method to quit the game and return to main screen
    public void quitGame(View view) {
        // Stops background music
        mediaPlayer.stop();
        // Closes the screen and ends any pending tasks
        finishAndRemoveTask();
    }

}
