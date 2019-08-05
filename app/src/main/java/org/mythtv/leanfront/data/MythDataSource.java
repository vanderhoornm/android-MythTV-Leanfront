package org.mythtv.leanfront.data;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.upstream.BaseDataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MythDataSource extends BaseDataSource implements DataSource {

    DefaultDataSource defSrc;
    boolean usingMyth;
    Context context;
    String userAgent;
    Uri uri;

    MythDataSource(Context context,String userAgent){
        super(true);
        this.context = context;
        this.userAgent = userAgent;
    }

    public long open(DataSpec dataSpec)
            throws IOException{
        uri = dataSpec.uri;
        if (uri.getScheme().equals("myth")) {
            usingMyth = true;
        }
        else {
            usingMyth = false;
            if (defSrc == null) {
                DefaultDataSourceFactory defFact = new DefaultDataSourceFactory(context, userAgent);
                defSrc = defFact.createDataSource();
            }
            return defSrc.open(dataSpec);
        }
        // TODO: Implement myth code
        return 0;
    }

    public int read(byte[] buffer,
             int offset,
             int readLength)
            throws IOException{
        if (!usingMyth)
            return defSrc.read(buffer,offset,readLength);
        // TODO: Implement myth code
        return 0;
    }

    @Nullable
    public Uri getUri(){
        return uri;
    }

    public void close()
            throws IOException{
        if (!usingMyth) {
            defSrc.close();
            return;
        }
        // TODO: Implement myth code
        return;
    }

    public static class Factory implements DataSource.Factory {

        Context context;
        String userAgent;

        public Factory(Context context,String userAgent){
            this.context = context;
            this.userAgent = userAgent;
        }

        public DataSource createDataSource() {
            return new MythDataSource(context,userAgent);
        }
    }
}
