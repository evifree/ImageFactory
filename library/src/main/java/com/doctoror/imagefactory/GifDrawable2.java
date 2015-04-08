package com.doctoror.imagefactory;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Movie;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.support.annotation.NonNull;

/**
 * Created by doctor on 4/8/15.
 */
public class GifDrawable2 extends AnimationDrawable {

    /**
     * Minimum delay
     */
    private static final int MIN_DELAY = 10;

    private final GifDecoder2 mGifDecoder;

    private final Movie mMovie;

    private int mMovieDuration;
    private final int mMovieHeight;
    private final int mMovieWidth;
    private final Bitmap mTmpBitmap;
    private final BitmapDrawable mTmpDrawable;
    private final Canvas mTmpCanvas;

    private final int mMaxFrames;

    private int mFramesDrawn;

    private boolean mAnimationEnded;

    private int mMovieTime;

    public GifDrawable2(@NonNull final Resources res, @NonNull final GifDecoder2 gifDecoder,
            @NonNull final Movie movie) {
        mGifDecoder = gifDecoder;
        mFramesDrawn = 1;
        mMaxFrames = gifDecoder.loopCount * gifDecoder.frameCount;
        mMovie = movie;
        gifDecoder.advance();

        //mMovieDuration = movie.duration();
        //if (mMovieDuration <= 0) {
            for (int i = 0; i < gifDecoder.frameCount; i++) {
                mMovieDuration += gifDecoder.getDelay(i);
            }
        //}

        System.out.println("c duration: " + mMovieDuration);
        System.out.println("m duration: " + movie.duration());

        mMovieHeight = movie.height();
        mMovieWidth = movie.width();

        mTmpBitmap = Bitmap.createBitmap(mMovieWidth, mMovieHeight, Bitmap.Config.ARGB_8888);
        mTmpDrawable = new BitmapDrawable(res, mTmpBitmap);
        mTmpCanvas = new Canvas(mTmpBitmap);

        addFrame(mTmpDrawable, gifDecoder.getDelay(0));
    }

    @Override
    public void start() {
        // Don't ever call super.start()!
        if (!mAnimationEnded) {
            scheduleSelf(this, SystemClock.uptimeMillis() + Math
                    .max(MIN_DELAY, mGifDecoder.getDelay(mGifDecoder.framePointer)));
        }
    }

    /**
     * If loop count has reached this will force-restart.
     * Restarts only if called after {@link #isAnimationEnded()} returned true.
     * Ignored if loop has not ended yet.
     */
    public void restart() {
        if (mAnimationEnded) {
            mAnimationEnded = false;
            mFramesDrawn = 0;
            // Don't call start() here!
            run();
        }
    }

    /**
     * Returns true if animation has ended (e.g. loop count reached)
     *
     * @return true if animation has ended, false if not started or not ended yet.
     */
    public boolean isAnimationEnded() {
        return mAnimationEnded;
    }

    @Override
    public void run() {
        if (!mAnimationEnded) {
            final long start = SystemClock.uptimeMillis();
            invalidateSelf();
            mGifDecoder.advance();
            if (mGifDecoder.loopCount != 0 && mFramesDrawn >= mMaxFrames) {
                mAnimationEnded = true;
                unscheduleSelf(this);
            } else {
                final int frameDelay = mGifDecoder.getDelay(mGifDecoder.framePointer);
                final long now = SystemClock.uptimeMillis();
                final long drawTime = SystemClock.uptimeMillis() - start;
                final long calculatedDelay = Math.max(MIN_DELAY, frameDelay - drawTime);
                mMovieTime += frameDelay;
                scheduleSelf(this, now + calculatedDelay);
            }
            mFramesDrawn++;
        }
    }

    @Override
    public void draw(final Canvas canvas) {
        if (mMovie.width() == 0 || mMovie.height() == 0) {
            return; // nothing to draw (empty bounds)
        }

        if (mMovieTime > mMovieDuration) {
            mMovieTime -= mMovieDuration;
        }

        //System.out.println("time is: " + (mMovieTime));

        mMovie.setTime(mMovieTime);
        mTmpBitmap.eraseColor(Color.TRANSPARENT);
        mMovie.draw(mTmpCanvas, 0, 0);
        mTmpDrawable.draw(canvas);
    }

    @Override
    public int getIntrinsicWidth() {
        return mMovieWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mMovieHeight;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }

    @Override
    public void stop() {
        unscheduleSelf(this);
    }

    @Override
    public boolean isRunning() {
        return !mAnimationEnded;
    }
}
