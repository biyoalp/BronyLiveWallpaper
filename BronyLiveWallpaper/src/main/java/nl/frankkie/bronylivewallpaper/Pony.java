package nl.frankkie.bronylivewallpaper;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

import au.com.bytecode.opencsv.CSVReader;
import jp.tomorrowkey.android.gifplayer.GifDecoder;

/**
 * Created by FrankkieNL on 31-7-13.
 */
public class Pony {

    public static final int DIR_LEFT = 180;
    public static final int DIR_UP = 90;
    public static final int DIR_RIGHT = 0;
    public static final int DIR_DOWN = 270;
    //TODO FIX THESE
    public static final int DIR_LEFT_UP = 135;
    public static final int DIR_LEFT_DOWN = 225;
    public static final int DIR_RIGHT_UP = 45;
    public static final int DIR_RIGHT_DOWN = 325;

    boolean debug = false;
    float positionX = 50; //default
    float positionY = 150; //default
    int width = 130; //default
    int height = 96; //default
    String name = "Unnamed Pony"; //default
    boolean outsideWrap = false;
    boolean limitAtEdge = true;
    Context context;
    ArrayList<Behaviour> behaviours = new ArrayList<Behaviour>();
    Behaviour currentBehaviour = null;
    float velocity = 0f;
    float direction = 0f;
    long timeToChangeBehaviour = 0;
    boolean imageRight = true;
    GifDecoder gifDecoder;
    long gifTimeForNextFrame = 0L;
    Bitmap currentFrameBitmap = null;
    int currentFrameInt = 0;

    public Pony(Context context, String name) {
        this.name = name;
        this.context = context;
        init();
    }

    public Rect getScreen() {
        return MyWallpaperService.screen;
    }

    public void setCurrentBehaviour(Behaviour behaviour) {
        currentBehaviour = behaviour;
        //set stuff!
        Random random = new Random();
        //change behaviour time
        timeToChangeBehaviour = System.currentTimeMillis() + (long) (currentBehaviour.maxDuration * 1000);
        //timeToChangeBehaviour += (long) random.nextInt((int) ((currentBehaviour.maxDuration - currentBehaviour.minDuration) * 1000));
        //velocity and direction
        velocity = currentBehaviour.movementSpeed / 3.0f; // to fix for 60fps istead of 10 fps.
        if (currentBehaviour.movementsAllowed.equalsIgnoreCase("None")) {
            direction = (random.nextBoolean()) ? DIR_LEFT : DIR_RIGHT; //just for image!
        } else if (currentBehaviour.movementsAllowed.equalsIgnoreCase("All")) {
            direction = random.nextFloat();
        } else if (currentBehaviour.movementsAllowed.equalsIgnoreCase("horizontal_only")) {
            direction = (random.nextBoolean()) ? DIR_LEFT : DIR_RIGHT;
        } else if (currentBehaviour.movementsAllowed.equalsIgnoreCase("vertical_only")) {
            direction = (random.nextBoolean()) ? DIR_UP : DIR_DOWN;
        } else if (currentBehaviour.movementsAllowed.equalsIgnoreCase("horizontal_vertical")) {
            switch (random.nextInt(4)) {
                case 0: {
                    direction = DIR_LEFT;
                    break;
                }
                case 1: {
                    direction = DIR_UP;
                    break;
                }
                case 2: {
                    direction = DIR_RIGHT;
                    break;
                }
                case 3: {
                    direction = DIR_DOWN;
                    break;
                }
            }
        } else if (currentBehaviour.movementsAllowed.equalsIgnoreCase("diagonal_only")) {
            switch (random.nextInt(4)) {
                case 0: {
                    direction = DIR_LEFT_UP;
                    break;
                }
                case 1: {
                    direction = DIR_LEFT_DOWN;
                    break;
                }
                case 2: {
                    direction = DIR_RIGHT_UP;
                    break;
                }
                case 3: {
                    direction = DIR_RIGHT_DOWN;
                    break;
                }
            }
        } else if (currentBehaviour.movementsAllowed.equalsIgnoreCase("diagonal_horizontal")
                || currentBehaviour.movementsAllowed.equalsIgnoreCase("diagonal_vertical")) {
            //just go with it. Trust me i'm a developer.
            direction = random.nextFloat();
        }
        //
        refreshImageDirection();
        //mMovie = null; //get new image at draw time!
        gifDecoder = null;
    }

    public void refreshImageDirection(){
        if (direction > DIR_UP && direction < DIR_DOWN) { //right
            imageRight = false;
        } else {
            imageRight = true;
        }
    }

    public void init() {
        behaviours.clear();
        AssetManager assets = context.getAssets();
        try {
            InputStream inputStream = assets.open(name + "/pony.ini");
            CSVReader reader = new CSVReader(new InputStreamReader(inputStream));
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine[0].equalsIgnoreCase("behavior")) {
                    behaviours.add(new Behaviour(nextLine));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //
        Log.e("BronyLiveWallpaper", "Loaded " + name);
        setCurrentBehaviour(getRandomBehaviour());

        positionY = (float) (50 + (Math.random() * 600));
        positionX = (float) (50 + (Math.random() * 400));
    }

    public void updateTick() {
        if (System.currentTimeMillis() > timeToChangeBehaviour) {
            setCurrentBehaviour(getRandomBehaviour());
        }
        move(1.0);
        ///
        if (gifDecoder == null) {
            refreshGifDecoder();
        }
        if (gifTimeForNextFrame < System.currentTimeMillis()) {
//            gifTimeForNextFrame = System.currentTimeMillis() + gifDecoder.getDelay(currentFrameInt);
            gifTimeForNextFrame = System.currentTimeMillis() + 50;
            currentFrameInt++;
        }
        if (currentFrameInt > gifDecoder.getFrameCount()) {
            currentFrameInt = 0;
        }
        try {
//            currentFrameBitmap.recycle();
            currentFrameBitmap = null;
            //Runtime.getRuntime().gc();
            currentFrameBitmap = gifDecoder.getFrame(currentFrameInt);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void move(double delta) {
        //velocity -= Math.abs((float) (friction * delta));
//        if (velocity < 0) { //limit
//            velocity = 0;
//        }Ben
//            s.position += s.velocity * delta;
        positionY += (float) ((Math.sin((-direction) * (Math.PI / 180))) * velocity) * delta;
        positionX += (float) ((Math.cos((-direction) * (Math.PI / 180))) * velocity) * delta;

        if (limitAtEdge && !outsideWrap) {
            if (positionY > getScreen().height() - height) {
                positionY = (getScreen().height() - height) - 50;
                direction += 180;
                direction = direction % 360;
                refreshImageDirection();
                refreshGifDecoder();
                Log.e("BornyLiveWallpaper", "Changed Direction ! " + direction);
            }
            if (positionY < 0) {
                positionY = 1;
                direction += 180;
                direction = direction % 360;
                refreshImageDirection();
                refreshGifDecoder();
                Log.e("BornyLiveWallpaper", "Changed Direction ! " + direction);
            }
            if (positionX > getScreen().width() - width) {
                positionX = (getScreen().width() - width) - 1;
                direction += 180;
                direction = direction % 360;
                refreshImageDirection();
                refreshGifDecoder();
                Log.e("BornyLiveWallpaper", "Changed Direction ! " + direction);
            }
            if (positionX < 0) {
                positionX = 1;
                direction += 180;
                direction = direction % 360;
                refreshImageDirection();
                refreshGifDecoder();
                Log.e("BornyLiveWallpaper", "Changed Direction ! " + direction);
            }
        }
    }

    public Behaviour findBehaviourByName(String name) {
        for (Behaviour behaviour : behaviours) {
            if (behaviour.name.equalsIgnoreCase(name)) {
                return behaviour;
            }
        }
        return null;
    }

    public void refreshGifDecoder() {
        try {
            gifDecoder = new GifDecoder();
            if (imageRight) {
                gifDecoder.read(context.getAssets().open(name + "/" + currentBehaviour.imageRight));
            } else {
                gifDecoder.read(context.getAssets().open(name + "/" + currentBehaviour.imageLeft));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * TODO fix die stuff
     */

    public Behaviour getRandomBehaviour() {
        Behaviour b = behaviours.get((int) (Math.random() * behaviours.size()));
        Log.e("BronyLiveWallpaper", "Changing behaviour of " + name);
        return b;
    }

    public void draw(Canvas canvas, Paint paint) {
        //drawTemp(canvas, paint);
        if (currentFrameBitmap != null) {
            canvas.drawBitmap(currentFrameBitmap, positionX, positionY, paint);
        }
        if (debug) {
            paint.setColor(Color.WHITE);
            canvas.drawText("X: " + positionX + "; Y: " + positionY + ";", 50, 50, paint);
            canvas.drawText("Dir: " + direction + "; Speed: " + velocity + ";", 50, 90, paint);
            canvas.drawText("B: " + currentBehaviour.name, 50, 130, paint);
            canvas.drawCircle(positionX, positionY, 15, paint);
        }
    }


    Movie mMovie;
    long mMovieStart = 0;

    /**
     * Will be replaced by better method soon(tm)
     *
     * @param canvas
     * @param paint
     */
    public void drawTemp(Canvas canvas, Paint paint) {
        long now = android.os.SystemClock.uptimeMillis();
        if (mMovieStart == 0) {   // first time
            mMovieStart = now;
        }
        if (mMovie != null) {
            int dur = mMovie.duration();
            if (dur == 0) {
                dur = 600;
            }
            int relTime = (int) ((now - mMovieStart) % dur);
            mMovie.setTime(relTime);
            mMovie.draw(canvas, positionX, positionY);
        } else {
            InputStream is = context.getResources().openRawResource(R.raw.aj_gallop_right);
            mMovie = Movie.decodeStream(is);
        }
    }

    public class Behaviour {

        String name;
        String[] line;
        float probability; //0.1 - 1.0
        float maxDuration;
        float minDuration;
        int movementSpeed; //pixels per 100ms, so calculate for current refresh rate!
        String imageRight;
        String imageLeft;
        String movementsAllowed;

        public Behaviour(String[] line) {
            this.line = line;
            name = line[1];
            probability = Float.parseFloat(line[2]);
            maxDuration = Float.parseFloat(line[3]);
            minDuration = Float.parseFloat(line[4]);
            movementSpeed = Integer.parseInt(line[5]);
            imageRight = line[6];
            imageLeft = line[7];
            movementsAllowed = line[8];
        }
    }
}
