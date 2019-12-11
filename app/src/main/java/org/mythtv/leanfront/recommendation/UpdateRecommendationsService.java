/*
 * Copyright (c) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mythtv.leanfront.recommendation;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import androidx.recommendation.app.ContentRecommendation;
import android.util.Log;

import com.bumptech.glide.Glide;
import org.mythtv.leanfront.BuildConfig;
import org.mythtv.leanfront.R;
import org.mythtv.leanfront.data.VideoContract;
import org.mythtv.leanfront.model.Video;
import org.mythtv.leanfront.model.VideoCursorMapper;
import org.mythtv.leanfront.ui.VideoDetailsActivity;

import java.util.concurrent.ExecutionException;

/*
 * This class builds up to MAX_RECOMMENDATIONS of ContentRecommendations and defines what happens
 * when they're selected from Recommendations section on the Home screen by creating an Intent.
 */
public class UpdateRecommendationsService extends IntentService {
    private static final String TAG = "RecommendationService";
    private static final int MAX_RECOMMENDATIONS = 3;
    private static final VideoCursorMapper mVideoCursorMapper = new VideoCursorMapper();

    private NotificationManager mNotifManager;

    public UpdateRecommendationsService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (mNotifManager == null) {
            mNotifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Generate recommendations, but only if recommendations are enabled
//        if (!sharedPreferences.getBoolean(getString(R.string.pref_key_recommendations), true)) {
//            Log.d(TAG, "Recommendations disabled");
//            mNotifManager.cancelAll();
//            return;
//        }
        Resources res = getResources();
        int cardWidth = res.getDimensionPixelSize(R.dimen.card_width);
        int cardHeight = res.getDimensionPixelSize(R.dimen.card_height);
        ContentRecommendation.Builder builder = new ContentRecommendation.Builder()
                .setBadgeIcon(R.drawable.videos_icon);

        Cursor cursor = getContentResolver().query(
                VideoContract.VideoEntry.CONTENT_URI,
                null, // projection
                null, // selection
                null, // selection clause
                "RANDOM() LIMIT " + MAX_RECOMMENDATIONS // sort order
        );

        if (cursor != null && cursor.moveToNext()) {
            try {
                do {
                    Video video = (Video) mVideoCursorMapper.convert(cursor);
                    int id = Long.valueOf(video.id).hashCode();

                    builder.setIdTag("Video" + id)
                            .setTitle(video.title)
                            .setText(getString(R.string.popular_header))
                            .setContentIntentData(ContentRecommendation.INTENT_TYPE_ACTIVITY,
                                    buildPendingIntent(video, id), 0, null);
                    if (video.cardImageUrl == null) {
                        Bitmap bitmap = Glide.with(getApplication())
                                .asBitmap()
                                .load(R.drawable.im_movie)
                                .submit(cardWidth, cardHeight) // Only use for synchronous .get()
                                .get();
                        builder.setContentImage(bitmap);
                    }  else {
                        Bitmap bitmap = Glide.with(getApplication())
                                .asBitmap()
                                .load(video.cardImageUrl + "&time=" + System.currentTimeMillis())
                                .submit(cardWidth, cardHeight) // Only use for synchronous .get()
                                .get();
                        builder.setContentImage(bitmap);
                    }
                    // Create an object holding all the information used to recommend the content.
                    ContentRecommendation rec = builder.build();
                    Notification notification = rec.getNotificationObject(getApplicationContext());

                    if (BuildConfig.DEBUG) Log.d(TAG, "Recommending video " + video.title);

                    // Recommend the content by publishing the notification.
                    mNotifManager.notify(id, notification);
                } while (cursor.moveToNext());
            } catch (InterruptedException | ExecutionException e) {
                Log.e(TAG, "Could not create recommendation.", e);
            } finally {
                cursor.close();
            }
        }
    }

    private Intent buildPendingIntent(Video video, int id) {
        Intent detailsIntent = new Intent(this, VideoDetailsActivity.class);
        detailsIntent.putExtra(VideoDetailsActivity.VIDEO, video);
        detailsIntent.putExtra(VideoDetailsActivity.NOTIFICATION_ID, id);
        detailsIntent.setAction(Long.toString(video.id));

        return detailsIntent;
    }
}
