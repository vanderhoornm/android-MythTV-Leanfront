package org.mythtv.leanfront.data;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
    DataSpec dataSpec;
    boolean isTransferStarted;
    long filePos;

    //TODO: Get these from settings
    static final int port = 6543;
    static final String feName = "android";

    static final String MYTH_PROTO_TOKEN = "BuzzOff";
    static final String MYTH_PROTO_VERSION = "91";
    static final String timeout = "2000";
    static final String SEEK_SET = "0";
    private static final String TAG = "MythDataSource";

    MythDataSource(Context context,String userAgent){
        super(true);
        this.context = context;
        this.userAgent = userAgent;
    }

    public long open(DataSpec dataSpec)
            throws IOException{
        this.dataSpec = dataSpec;
        uri = dataSpec.uri;
        isTransferStarted = false;
        filePos = dataSpec.absoluteStreamPosition;
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
        send.clear();
        send.add("ANN Playback " + feName + " 0 ");
        resp = sendReceiveStringList(controlSock,send);
        if (resp.length < 1
                || ! resp[0].equals("OK"))
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
        transferInitializing(dataSpec);
        if (dataSpec.absoluteStreamPosition != 0){
            send.clear();
            send.add("QUERY_FILETRANSFER " + recorderNum);
            send.add("SEEK");
            send.add(String.valueOf(dataSpec.absoluteStreamPosition));
            send.add (SEEK_SET);
            send.add("0"); // curpos not needed for SEEK_SET
            sendReceiveStringList(controlSock,send);
        }
        long length = fileSize - dataSpec.absoluteStreamPosition;
        if (dataSpec.length != C.LENGTH_UNSET
            && dataSpec.length < length)
            length = dataSpec.length;
        return length;
    }

    String[] sendReceiveStringList(MythSocket msock, List<String>inList)
            throws IOException {
        sendStringList(msock, inList);
        return receiveStringList(msock);
    }

    void sendStringList(MythSocket msock, List<String>inList)
            throws IOException {
        StringBuilder combo = new StringBuilder();
        for (String item: inList) {
            if (combo.length() > 0)
                combo.append("[]:[]");
            combo.append(item);
        }
        int leng = combo.length();
        String lengs = leng + "        ";
        combo.insert(0,lengs,0,8);
        byte[] buff = combo.toString().getBytes(StandardCharsets.UTF_8);
        msock.out.write(buff,0,leng+8);
    }

    String[] receiveStringList(MythSocket msock)
            throws IOException {
        // read first 8 bytes of response - (length)
        byte[] buff = new byte[8];
        int remains = 8;
        while (remains > 0) {
            remains = remains - msock.in.read(buff,8-remains, remains);
        }
        int len = Integer.parseInt((new String(buff, StandardCharsets.UTF_8)).trim());
        buff = new byte[len];
        remains = len;
        while (remains > 0) {
            remains = remains - msock.in.read(buff,len-remains, remains);
        }
        String resp = new String(buff, StandardCharsets.UTF_8);
        String[] ret = resp.split("\\[\\]:\\[\\]");
        return ret;
    }

    public int read(byte[] buffer,
             int offset,
             int readLength)
            throws IOException{
        int length;
        if (!usingMyth) {
            length = defSrc.read(buffer, offset, readLength);
            filePos += length;
            return length;
        }
        // TODO: Implement myth code
        if (!isTransferStarted) {
            transferStarted(dataSpec);
            isTransferStarted = true;
        }
        controlSock.reset();
        transferSock.reset();
        List<String> send = new ArrayList<String>();
        send.add("QUERY_FILETRANSFER " + recorderNum);
        send.add("REQUEST_BLOCK");
        send.add(String.valueOf(readLength));
        sendStringList(controlSock,send);

        length = readLength;
        int remains = length;
        while (remains > 0) {
            remains = remains - transferSock.in.read(buffer,offset+length-remains, remains);
            if (controlSock.in.available() > 8) {
                String[] resp = receiveStringList(controlSock);
                if (resp.length < 1)
                    throw new IOException();
                length = Integer.parseInt(resp[0]);
                remains = remains - readLength + length;
            }
        }
        bytesTransferred(length);
        filePos += length;
        return length;
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
        if (isTransferStarted) {
            transferEnded();
            isTransferStarted = false;
        }

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
        void reset() throws IOException {
            int leng = in.available();
            while (leng > 0) {
                in.skip(leng);
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                }
                leng = in.available();
            }
        }

    }
}
