/*
 * Copyright (c) 2019-2020 Peter Bennett
 *
 * This file is part of MythTV-leanfront.
 *
 * MythTV-leanfront is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * MythTV-leanfront is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with MythTV-leanfront.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.mythtv.leanfront.data;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.upstream.BaseDataSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;

import org.mythtv.leanfront.ui.playback.PlaybackFragment;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MythHttpDataSource extends BaseDataSource implements DataSource {

    private DataSpec mDataSpec;
    private PlaybackFragment mPlaybackFragment;
    private HttpDataSource mHttpDataSource;
    private long mTotalLength;
    private long mCurrentPos;
    private long mOffsetBytes;
    private static final String TAG = "lfe";
    private static final String CLASS = "MythHttpDataSource";


    public MythHttpDataSource(String userAgent, PlaybackFragment playbackFragment){
        super(true);
        mPlaybackFragment = playbackFragment;
        Map defaultRequestProperties = new HashMap<String, String>();
        defaultRequestProperties.put("accept-encoding","identity");
        mHttpDataSource = new DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setDefaultRequestProperties(defaultRequestProperties)
                .createDataSource();
        mPlaybackFragment.setDataSource(this);
    }

    @Override
    public long open(DataSpec dataSpec)
            throws IOException {
        mOffsetBytes = mPlaybackFragment.getOffsetBytes();
        this.mDataSpec = new DataSpec(dataSpec.uri,
                dataSpec.httpMethod,
                dataSpec.httpBody,
                dataSpec.absoluteStreamPosition + mOffsetBytes,
                dataSpec.position + mOffsetBytes,
                dataSpec.length,
                dataSpec.key,
                dataSpec.flags);

        long leng = 0;
        try {
            leng = mHttpDataSource.open(mDataSpec);
        } catch (HttpDataSource.InvalidResponseCodeException e) {
            // Response code 416 = read past eof
            if (e.responseCode == 416) {
                leng = 0;
                Log.i(TAG, CLASS + " End of file.");
            }
            else {
                Log.e(TAG, CLASS + " Bad Http Response Code:" +e.responseCode
                    + " " + e.responseMessage);
                throw e;
            }
        }
        mTotalLength = mDataSpec.absoluteStreamPosition + leng;
        mCurrentPos = mDataSpec.absoluteStreamPosition;
        if (!mPlaybackFragment.isBounded())
            leng = -1;
        return leng;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        if (readLength <= 0)
            return 0;
        int leng = mHttpDataSource.read(buffer,offset,readLength);
        if (leng == -1) {
            leng = 0;
        }
        if (!mPlaybackFragment.isBounded() && leng == 0) {
            DataSpec dataSpec2 = new DataSpec(mDataSpec.uri,
                    mDataSpec.httpMethod,
                    mDataSpec.httpBody,
                    mCurrentPos + leng,
                    mCurrentPos + leng,
                    mDataSpec.length,
                    mDataSpec.key,
                    mDataSpec.flags);
            mHttpDataSource.close();

            if (mPlaybackFragment.isSpeededUp()) {
                Activity activity = mPlaybackFragment.getActivity();
                if (activity != null)
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                        mPlaybackFragment.resetSpeed();
                    }
                    });
            }
            long leng2 = 0;
            try {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // Ignore this exception.
                }
                leng2 = mHttpDataSource.open(dataSpec2);
            } catch (HttpDataSource.InvalidResponseCodeException e) {
                // Response code 416 = read past eof
                if (e.responseCode == 416) {
                    leng2 = 0;
                    Log.i(TAG, CLASS + " End of file.");
                }
                else {
                    Log.e(TAG, CLASS + " Bad Http Response Code:" +e.responseCode
                            + " " + e.responseMessage);
                    throw e;
                }
            }
            long totalLength2 = dataSpec2.absoluteStreamPosition + leng2;
            Log.d(TAG, CLASS + " Incremental data length:" + leng2);
            if (totalLength2 > mTotalLength) {
                mTotalLength = totalLength2;
                leng = mHttpDataSource.read(buffer, offset, readLength);
                mCurrentPos = dataSpec2.absoluteStreamPosition;
                mDataSpec = dataSpec2;
            }
        }
        if (leng > 0)
            mCurrentPos += leng;
        else
            leng = -1;
        return leng;
    }

    @Nullable
    @Override
    public Uri getUri() {
        return mDataSpec.uri;
    }

    @Override
    public void close() throws IOException {
        mHttpDataSource.close();
    }

    public long getCurrentPos() {
        return mCurrentPos;
    }

    public static class Factory implements DataSource.Factory {
        private PlaybackFragment mPlaybackFragment;
        private String mUserAgent;

        public Factory(String userAgent, PlaybackFragment playbackFragment) {
            mUserAgent = userAgent;
            mPlaybackFragment = playbackFragment;
        }

        @Override
        public DataSource createDataSource() {
            return new MythHttpDataSource(mUserAgent, mPlaybackFragment);
        }
    }

}
