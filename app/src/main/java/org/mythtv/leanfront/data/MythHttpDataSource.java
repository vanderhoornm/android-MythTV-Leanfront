package org.mythtv.leanfront.data;


import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.upstream.BaseDataSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;

import org.mythtv.leanfront.ui.PlaybackFragment;

import java.io.IOException;

public class MythHttpDataSource extends BaseDataSource implements DataSource {

    private DataSpec mDataSpec;
    private PlaybackFragment mPlaybackFragment;
    private HttpDataSource mHttpDataSource;
    private long mTotalLength;
    private long mCurrentPos;
    private long mOffsetBytes;

    public MythHttpDataSource(String userAgent, PlaybackFragment playbackFragment){
        super(true);
        mPlaybackFragment = playbackFragment;
        mHttpDataSource = new DefaultHttpDataSource(userAgent);
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
            leng = 0;
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

            long leng2 = 0;
            try {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                leng2 = mHttpDataSource.open(dataSpec2);
            } catch (HttpDataSource.InvalidResponseCodeException e) {

                leng2 = 0;
            }
            long totalLength2 = dataSpec2.absoluteStreamPosition + leng2;
            if (totalLength2 > mTotalLength) {
                mTotalLength = totalLength2;
                leng2 = mHttpDataSource.read(buffer, offset, readLength);
                mCurrentPos = dataSpec2.absoluteStreamPosition;
                mDataSpec = dataSpec2;
                leng += leng2;
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
