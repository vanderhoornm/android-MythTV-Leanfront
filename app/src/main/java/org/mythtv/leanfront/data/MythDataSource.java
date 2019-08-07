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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MythDataSource extends BaseDataSource implements DataSource {

    DefaultDataSource defSrc;
    boolean usingMyth;
    Context context;
    String userAgent;
    Uri uri;
    MythSocket controlSock;
    MythSocket transferSock;
    int recorderNum;
    long fileSize;

    //TODO: Get these from settings
    static final int port = 6543;
    static final String feName = "android";

    static final String MYTH_PROTO_TOKEN = "BuzzOff";
    static final String MYTH_PROTO_VERSION = "91";
    static final String timeout = "2000";
    static final int blocksize = 327680;

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
        // Make Connection
        controlSock = new MythSocket(uri.getHost(),port);
        // Check protocol
        List<String> send = new ArrayList<String>();
        send.add("MYTH_PROTO_VERSION "+MYTH_PROTO_VERSION + " " + MYTH_PROTO_TOKEN);
        String[] resp = sendReceiveStringList(controlSock,send);
        if (resp.length < 2
            || ! resp[0].equals("ACCEPT")
            || ! resp[1].equals(MYTH_PROTO_VERSION))
            throw new IOException();
        // Make Connection
        transferSock = new MythSocket(uri.getHost(),port);
        send.clear();
        send.add("ANN FileTransfer " + feName + " 0 1 " + timeout);
        send.add(uri.getPath());
        send.add(""); // group name, empty string for default
        resp = sendReceiveStringList(transferSock,send);
        if (resp.length < 3
            || ! resp[0].equals("OK"))
            throw new IOException();
        recorderNum = Integer.parseInt(resp[1]);
        fileSize = Long.parseLong(resp[2]);
        return 0;
    }

    String[] sendReceiveStringList(MythSocket msock, List<String>inList)
            throws IOException {
        StringBuilder combo = new StringBuilder();
        for (String item: inList) {
            if (combo.length() > 0)
                combo.append("[]:[]");
            combo.append(item);
        }
        int leng = combo.length();
        String lengs = Integer.toString(leng) + "        ";
        combo.insert(0,lengs,0,8);
        byte[] buff = combo.toString().getBytes("UTF-8");
        msock.out.write(buff,0,leng+8);
        // read first 8 bytes of response - (length)
        buff = new byte[8];
        int remains = 8;
        while (remains > 0) {
            remains = remains - msock.in.read(buff,8-remains, remains);
        }
        int len = Integer.parseInt((new String(buff,"UTF-8")).trim());
        buff = new byte[len];
        remains = len;
        while (remains > 0) {
            remains = remains - msock.in.read(buff,len-remains, remains);
        }
        String resp = new String(buff,"UTF-8");
        String[] ret = resp.split("\\[\\]:\\[\\]");
        return ret;
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
        if (controlSock != null && controlSock.sock != null) {
            controlSock.sock.close();
            controlSock = null;
        }
        if (transferSock != null && transferSock.sock != null) {
            transferSock.sock.close();
            transferSock = null;
        }
        return;
    }

/*
public void disconnect(){
}
*/

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

    class MythSocket {
        Socket sock;
        InputStream in;
        OutputStream out;

        MythSocket(String host, int port) throws IOException {
            InetAddress address = InetAddress.getByName(host);
            sock = new Socket(address,port);
            in = sock.getInputStream();
            out = sock.getOutputStream();
        }
    }
}
