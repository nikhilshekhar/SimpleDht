package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {

    static final String TAG = SimpleDhtProvider.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static boolean isQueryOriginatingPort=true;
    static String queryOriginatingPort="";
    ArrayList<String> recordOfData = new ArrayList<String>(7);

    static MatrixCursor cursorWithAllMessages = new MatrixCursor(new String[]{"key","value"});
    static MatrixCursor cursorWithStarMessages = new MatrixCursor(new String[]{"key","value"});

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub

        if(selection.compareToIgnoreCase("*")==0) {
            String[] files = getContext().fileList();
            for (String file : files) {
                getContext().deleteFile(file);
            }
            new deleteQueryClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if(selection.compareToIgnoreCase("@")==0){

            String[] files = getContext().fileList();

            for(String file: files){
                getContext().deleteFile(file);
            }
        }else {
            getContext().deleteFile(selection);
        }

        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        try {
            //        myPort   myNodeId  myHashNode  mySuccessor   myHashSuccessor    myPredecessor    myHashPredecessor
            //        portNumber:NodeId:hashedNode:  nextNode:     hashedNextNode:    previousNode:    hashedPreviousNode
            //         0            1      2         3               4                    5                 6

            String temp1 = "aaa";
            if(recordOfData.get(3).equals("")||recordOfData.get(3).equals(recordOfData.get(1))||getPortNumber(values.getAsString("key"),temp1)){
                FileWriter fw = new FileWriter(new File(getContext().getFilesDir().getAbsolutePath(), values.getAsString("key")));
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(values.getAsString("value"));
                bw.close();

            }else{

                Integer portId = 0;
                if(recordOfData.get(3).compareToIgnoreCase("5554")==0){
                    portId = 11108;
                }else if(recordOfData.get(3).compareToIgnoreCase("5556")==0){
                    portId = 11112;
                }else if(recordOfData.get(3).compareToIgnoreCase("5558")==0){
                    portId = 11116;
                }else if(recordOfData.get(3).compareToIgnoreCase("5560")==0){
                    portId = 11120;
                }
                else if(recordOfData.get(3).compareToIgnoreCase("5562")==0){
                    portId = 11124;
                }
                new insertClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,portId.toString(),values.get("key").toString(),values.get("value").toString());
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return uri;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // TODO Auto-generated method stub
        Log.v(TAG,"Inside query");
        MatrixCursor msgCursor = new MatrixCursor(new String[]{"key","value"});
        String msgValue="" ;
        BufferedInputStream bis;
        int temp;
        if(selection.compareToIgnoreCase("@")==0){
            String[] files=getContext().fileList();
            for(String fileName: files){
                msgValue="";
                try {
                    bis = new BufferedInputStream(getContext().openFileInput(fileName));
                    while ((temp = bis.read()) != -1) {
                        msgValue += (char) temp;
                    }
                    bis.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                msgCursor.addRow(new String[]{fileName, msgValue});
            }
            return msgCursor;
        }else if(selection.compareToIgnoreCase("*")==0) {
            String[] files = getContext().fileList();
            //Reading from local avd
            for (String fileName : files) {
                msgValue = "";
                try {
                    bis = new BufferedInputStream(getContext().openFileInput(fileName));
                    while ((temp = bis.read()) != -1) {
                        msgValue += (char) temp;
                    }
                    bis.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                cursorWithStarMessages.addRow(new String[]{fileName, msgValue});
            }

            //Reading from remote avd
            if(!recordOfData.get(1).equals(recordOfData.get(3))) {
                Integer portId = 0;
                if(recordOfData.get(3).compareToIgnoreCase("5554")==0){
                    portId = 11108;
                }else if(recordOfData.get(3).compareToIgnoreCase("5556")==0){
                    portId = 11112;
                }else if(recordOfData.get(3).compareToIgnoreCase("5558")==0){
                    portId = 11116;
                }else if(recordOfData.get(3).compareToIgnoreCase("5560")==0){
                    portId = 11120;
                }
                else if(recordOfData.get(3).compareToIgnoreCase("5562")==0){
                    portId = 11124;
                }

                new starQueryClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, portId.toString(), recordOfData.get(0));
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                }
            }
            return cursorWithStarMessages;
        }else{
            try {

                //        myPort   myNodeId  myHashNode  mySuccessor   myHashSuccessor    myPredecessor    myHashPredecessor
                //        portNumber:NodeId:hashedNode:  nextNode:     hashedNextNode:    previousNode:    hashedPreviousNode
                //         0            1      2         3               4                    5                 6
                String temp1 = "aaa";
                if(recordOfData.get(3).equals("")||recordOfData.get(3).equals(recordOfData.get(1))||getPortNumber(selection,temp1)){
                    bis = new BufferedInputStream(getContext().openFileInput(selection));
                    while ((temp = bis.read()) != -1) {
                        msgValue += (char) temp;
                    }
                    bis.close();
                    msgCursor.addRow(new String[]{selection, msgValue});
                }else {

                    String originatingPort = recordOfData.get(0);
                    if (!isQueryOriginatingPort) {
                        originatingPort = queryOriginatingPort;
                    }

                    Integer portId = 0;
                    if(recordOfData.get(3).compareToIgnoreCase("5554")==0){
                        portId = 11108;
                    }else if(recordOfData.get(3).compareToIgnoreCase("5556")==0){
                        portId = 11112;
                    }else if(recordOfData.get(3).compareToIgnoreCase("5558")==0){
                        portId = 11116;
                    }else if(recordOfData.get(3).compareToIgnoreCase("5560")==0){
                        portId = 11120;
                    }
                    else if(recordOfData.get(3).compareToIgnoreCase("5562")==0){
                        portId = 11124;
                    }

                    new queryClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, portId.toString(), selection, originatingPort);

                    if (isQueryOriginatingPort) {
                        Thread.sleep(1500);
                    }

                    if (cursorWithAllMessages.moveToFirst()) {
                        do {
                            if (selection.equals(cursorWithAllMessages.getString(msgCursor.getColumnIndex("key")))) {
                                msgCursor.addRow(new String[]{selection, cursorWithAllMessages.getString(msgCursor.getColumnIndex("value"))});
                                return msgCursor;
                            }
                        } while (cursorWithAllMessages.moveToNext());
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        return msgCursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean onCreate() {

        // TODO Auto-generated method stub
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        String hashedNode = null;
        try{
            hashedNode = genHash(portStr);
        }catch(Exception e){
            e.printStackTrace();
        }

//        portNumber:NodeId:hashedNode:nextNode:hashedNextNode:previousNode:hashedPreviousNode
//         0            1      2         3         4               5             6

        recordOfData.add(0,myPort);
        recordOfData.add(1,portStr);
        recordOfData.add(2,hashedNode);
        recordOfData.add(3,"");
        recordOfData.add(4,"");
        recordOfData.add(5, "");
        recordOfData.add(6, "");
        try{
            Log.v(TAG,"Hash of  5554:"+genHash("5554"));//33d6357cfaaf0f72991b0ecd8c56da066613c089
            Log.v(TAG,"Hash of  5556:"+genHash("5556"));//208f7f72b198dadd244e61801abe1ec3a4857bc9
            Log.v(TAG,"Hash of  5558:"+genHash("5558"));//abf0fd8db03e5ecb199a9b82929e9db79b909643
            Log.v(TAG,"Hash of  5560:"+genHash("5560"));//c25ddd596aa7c81fa12378fa725f706d54325d12
            Log.v(TAG,"Hash of  5562:"+genHash("5562"));//177ccecaec32c54b82d5aaafc18a2dadb753e3b1
        }catch (Exception e){
            e.printStackTrace();
        }

        try {
            ServerSocket serverSocket = new ServerSocket(10000);
            Log.v(TAG,"Calling server task");
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Error while creating Server task");
            e.printStackTrace();
            return false;
        }

        Log.v(TAG, "Record data56:"+recordOfData.toString());
        new insertClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,myPort);
        return false;
    }


    private class insertClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            Socket socket = null;
            DataOutputStream writeOutputStream = null;

            if(msgs.length == 1){
                try {

                    String msgToSend = "NodeJoin"+":"+msgs[0]+":" + recordOfData.get(1) + ":";
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt("11108"));
                    writeOutputStream = new DataOutputStream(socket.getOutputStream());
                    writeOutputStream.write(msgToSend.getBytes());
                    writeOutputStream.flush();
                    socket.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else{
                try {
                    String msgToSend = "InsertTask"+":"+msgs[1]+":" + msgs[2] + ":";
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(msgs[0]));
                    writeOutputStream = new DataOutputStream(socket.getOutputStream());
                    writeOutputStream.write(msgToSend.getBytes());
                    writeOutputStream.flush();
                    socket.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    private class queryClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            Socket socket = null;
            DataOutputStream writeOutputStream = null;

            if(msgs.length==3){
                try {
                    String msgToSend = "QueryTask"+":"+msgs[1]+":" + msgs[2] + ":";
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(msgs[0]));
                    writeOutputStream = new DataOutputStream(socket.getOutputStream());
                    writeOutputStream.write(msgToSend.getBytes());
                    writeOutputStream.flush();///check this
                    socket.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else{
                try {
                    String msgToSend = "ReturnStarQuery" + ":" + msgs[1] + recordOfData.get(1)+":";
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(msgs[0]));
                    writeOutputStream = new DataOutputStream(socket.getOutputStream());
                    writeOutputStream.write(msgToSend.getBytes());
                    writeOutputStream.flush();///check this
                    socket.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }

    private class deleteQueryClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            try {
                String msgToSend = "DeleteTask" + ":";
                if (!REMOTE_PORT0.equals(recordOfData.get(0))) {
                    Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT0));
                    socket0.getOutputStream().write(msgToSend.getBytes());
                    socket0.getOutputStream().flush();
                    socket0.close();
                    Log.v(TAG, "Message sent from REMOTE_PORT0:" + REMOTE_PORT0);
                }

                if (!REMOTE_PORT1.equals(recordOfData.get(0))) {
                    Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT1));
                    socket1.getOutputStream().write(msgToSend.getBytes());
                    socket1.getOutputStream().flush();
                    socket1.close();
                    Log.v(TAG, "Message sent from REMOTE_PORT1:" + REMOTE_PORT1);
                }

                if (!REMOTE_PORT2.equals(recordOfData.get(0))) {
                    Socket socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT2));
                    socket2.getOutputStream().write(msgToSend.getBytes());
                    socket2.getOutputStream().flush();
                    socket2.close();
                    Log.v(TAG, "Message sent from REMOTE_PORT2:" + REMOTE_PORT2);
                }

                if (!REMOTE_PORT3.equals(recordOfData.get(0))) {
                    Socket socket3 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT3));
                    socket3.getOutputStream().write(msgToSend.getBytes());
                    socket3.getOutputStream().flush();
                    socket3.close();
                    Log.v(TAG, "Message sent from REMOTE_PORT3:" + REMOTE_PORT3);
                }

                if (!REMOTE_PORT4.equals(recordOfData.get(0))) {
                    Socket socket4 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT4));
                    socket4.getOutputStream().write(msgToSend.getBytes());
                    socket4.getOutputStream().flush();
                    socket4.close();
                    Log.v(TAG, "Message sent from REMOTE_PORT4:" + REMOTE_PORT4);
                }

                Log.e(TAG, "Sending message:" + msgToSend);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */

            try {
                while(true) {
                    Socket serverSocketConnection = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(serverSocketConnection.getInputStream()));
                    String p = in.readLine();
                    serverSocketConnection.close();
                    String[] partsOfMessageReceived = p.split(":");
                    if (partsOfMessageReceived[0].compareToIgnoreCase("NodeJoin") == 0) {
                        joinTask(p);

                    }else if(partsOfMessageReceived[0].compareToIgnoreCase("NodeAdded") == 0){
                        //        myPort   myNodeId  myHashNode  mySuccessor   myHashSuccessor    myPredecessor    myHashPredecessor
                        //        portNumber:NodeId:hashedNode:  nextNode:     hashedNextNode:    previousNode:    hashedPreviousNode
                        //         0            1      2         3               4                    5                 6

                        String newHash;
                        if(partsOfMessageReceived[1]!=null){
                            newHash = genHash(partsOfMessageReceived[1]);
                        }else{
                            newHash = "";
                        }
                        String newHash1;
                        if(partsOfMessageReceived[2]!=null){
                            newHash1 = genHash(partsOfMessageReceived[2]);
                        }else{
                            newHash1 = "";
                        }
                        recordOfData.set(3, partsOfMessageReceived[1]);
                        recordOfData.set(4,newHash);
                        recordOfData.set(5,partsOfMessageReceived[2]);
                        recordOfData.set(6, newHash1);

                    }else if(partsOfMessageReceived[0].compareToIgnoreCase("UpdatePredecessor") == 0){
                        String newHash;
                        if(partsOfMessageReceived[1]!=null){
                            newHash = genHash(partsOfMessageReceived[1]);
                        }else{
                            newHash = "";
                        }
                        recordOfData.set(3, partsOfMessageReceived[1]);
                        recordOfData.set(4, newHash);

                    }else if(partsOfMessageReceived[0].compareToIgnoreCase("InsertTask") == 0){
                        ContentValues cv = new ContentValues();
                        cv.put("key", partsOfMessageReceived[1]);
                        cv.put("value", partsOfMessageReceived[2]);
                        Uri uri = new Uri.Builder().scheme("content").authority("edu.buffalo.cse.cse486586.simpledht.provider").build();
                        insert(uri,cv);

                    }else if(partsOfMessageReceived[0].compareToIgnoreCase("QueryTask") == 0){
                        isQueryOriginatingPort = false;
                        queryOriginatingPort=partsOfMessageReceived[2];
                        Uri uri = new Uri.Builder().scheme("content").authority("edu.buffalo.cse.cse486586.simpledht.provider").build();
                        Cursor returnValues = query(uri, null, partsOfMessageReceived[1], null, null);

                        if(returnValues.moveToFirst()){
                            String msgToSend =  "ReturnQueryTask"+":"+returnValues.getString(returnValues.getColumnIndex("key"))+":"+returnValues.getString(returnValues.getColumnIndex("value"))+":"+partsOfMessageReceived[2]+":";
                            joinTask(msgToSend);
                        }
                        isQueryOriginatingPort=true;
                        queryOriginatingPort="";

                    }else if(partsOfMessageReceived[0].compareToIgnoreCase("ReturnQueryTask")==0){
                        cursorWithAllMessages.addRow(new String[]{partsOfMessageReceived[1],partsOfMessageReceived[2]});
                    }else if(partsOfMessageReceived[0].compareToIgnoreCase("StarQuery")==0){
                        //        myPort   myNodeId  myHashNode  mySuccessor   myHashSuccessor    myPredecessor    myHashPredecessor
                        //        portNumber:NodeId:hashedNode:  nextNode:     hashedNextNode:    previousNode:    hashedPreviousNode
                        //         0            1      2         3               4                    5                 6
                        if(!partsOfMessageReceived[1].equals(recordOfData.get(0))){
                            Uri uri = new Uri.Builder().scheme("content").authority("edu.buffalo.cse.cse486586.simpledht.provider").build();
                            Cursor returnValues = query(uri, null, "@", null, null);
                            String rowList="";
                            if (returnValues != null && returnValues.getCount() > 0) {
                                if (returnValues.moveToFirst()) {
                                    do {
                                        rowList += returnValues.getString(returnValues.getColumnIndex("key")) + "#" + returnValues.getString(returnValues.getColumnIndex("value")) + ":";
                                    } while (returnValues.moveToNext());
                                }
                                callReturnStarQueryClientTask(partsOfMessageReceived[1], rowList);
                            }

                            Integer portId = 0;
                            if(recordOfData.get(3).compareToIgnoreCase("5554")==0){
                                portId = 11108;
                            }else if(recordOfData.get(3).compareToIgnoreCase("5556")==0){
                                portId = 11112;
                            }else if(recordOfData.get(3).compareToIgnoreCase("5558")==0){
                                portId = 11116;
                            }else if(recordOfData.get(3).compareToIgnoreCase("5560")==0){
                                portId = 11120;
                            }
                            else if(recordOfData.get(3).compareToIgnoreCase("5562")==0){
                                portId = 11124;
                            }

                            callStarQueryClientTask(portId.toString(), partsOfMessageReceived[1]);
                        }
                    }else if(partsOfMessageReceived[0].compareToIgnoreCase("ReturnStarQuery")==0){
                        for(int i=1;i<partsOfMessageReceived.length;i++){
                            if(partsOfMessageReceived[i].contains("#")){
                                cursorWithStarMessages.addRow(new String[]{partsOfMessageReceived[i].split("#")[0], partsOfMessageReceived[i].split("#")[1]});
                            }
                        }
                    }else if(partsOfMessageReceived[0].compareToIgnoreCase("DeleteTask")==0){
                        Uri uri = new Uri.Builder().scheme("content").authority("edu.buffalo.cse.cse486586.simpledht.provider").build();
                        delete(uri,"@",null);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        protected void joinTask(String p){
            String[] partsOfMessageReceived = p.split(":");
            String tempJoin = partsOfMessageReceived[0];
            String tempPortNumber = partsOfMessageReceived[1];
            String tempNodeId = partsOfMessageReceived[2];
            if (p != null){
                if (tempJoin.compareToIgnoreCase("NodeJoin") == 0) {
                    new joinCurrentNodeTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, tempPortNumber,tempNodeId);
                }else if(tempJoin.compareToIgnoreCase("ReturnQueryTask") == 0){
                    String originatingPort = partsOfMessageReceived[3];
                    new starQueryClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, originatingPort,tempPortNumber,tempNodeId);

                }
            }
        }



    }

    protected  void callReturnStarQueryClientTask(String partOfMessage,String rowList){
        new queryClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, partOfMessage, rowList);
    }

    protected  void callStarQueryClientTask(String portId,String partOfMessage){
        new starQueryClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, portId, partOfMessage);
    }

    private class starQueryClientTask extends AsyncTask<String,Void,Void> {
        @Override
        protected Void doInBackground(String... msgs) {
            Socket socket;
            DataOutputStream writeOutputStream ;
            if(msgs.length == 3){
                try {
                    String msgToSend = "ReturnQueryTask" + ":" + msgs[1] + ":" + msgs[2] + ":" + recordOfData.get(1) + ":";
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(msgs[0]));
                    writeOutputStream = new DataOutputStream(socket.getOutputStream());
                    writeOutputStream.write(msgToSend.getBytes());
                    writeOutputStream.flush();
                    socket.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else{
                try {
                    String msgToSend = "StarQuery" + ":" + msgs[1] + ":";
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(msgs[0]));
                    writeOutputStream = new DataOutputStream(socket.getOutputStream());
                    writeOutputStream.write(msgToSend.getBytes());
                    writeOutputStream.flush();
                    socket.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            return  null;
        }
    }

    private class joinCurrentNodeTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            //        myPort   myNodeId  myHashNode  mySuccessor   myHashSuccessor    myPredecessor    myHashPredecessor
            //        portNumber:NodeId:hashedNode:  nextNode:     hashedNextNode:    previousNode:    hashedPreviousNode
            //         0            1      2         3               4                    5                 6

            String msgToSend;
            try {
                Socket socket;
                DataOutputStream writeOutputStream;
                String temp2 = "bbb";
                if(getPortNumber(msgs[1],temp2)){

                    String temp;
                    if(recordOfData.get(5).equals("")){
                        temp = recordOfData.get(1);
                    }else{
                        temp = recordOfData.get(5);
                    }
                    msgToSend = "NodeAdded" + ":" + recordOfData.get(1) + ":" + temp + ":";
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(msgs[0]));
                    writeOutputStream = new DataOutputStream(socket.getOutputStream());
                    writeOutputStream.write(msgToSend.getBytes());
                    writeOutputStream.flush();
                    socket.close();

                    if(null!= recordOfData.get(5) && !recordOfData.get(5).equals("")){
                        msgToSend = "UpdatePredecessor" + ":" + msgs[1] + ":";

                        Integer portId = 0;
                        if(recordOfData.get(5).compareToIgnoreCase("5554")==0){
                            portId = 11108;
                        }else if(recordOfData.get(5).compareToIgnoreCase("5556")==0){
                            portId = 11112;
                        }else if(recordOfData.get(5).compareToIgnoreCase("5558")==0){
                            portId = 11116;
                        }else if(recordOfData.get(5).compareToIgnoreCase("5560")==0){
                            portId = 11120;
                        }
                        else if(recordOfData.get(5).compareToIgnoreCase("5562")==0){
                            portId = 11124;
                        }

                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), portId);
                        writeOutputStream = new DataOutputStream(socket.getOutputStream());
                        writeOutputStream.write(msgToSend.getBytes());
                        writeOutputStream.flush();
                        socket.close();
                    }
                    String newHash = null;
                    if(msgs[1]!=null){
                        newHash = genHash(msgs[1]);
                    }else{
                        newHash = "";
                    }
                    recordOfData.set(5,msgs[1]);
                    recordOfData.set(6,newHash);

                }else if( recordOfData.get(1).equals(recordOfData.get(3))){
                    msgToSend = "NodeAdded" + ":" + recordOfData.get(1) + ":" + recordOfData.get(1) + ":" ;
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(msgs[0]));
                    writeOutputStream = new DataOutputStream(socket.getOutputStream());
                    writeOutputStream.write(msgToSend.getBytes());
                    writeOutputStream.flush();
                    socket.close();

                    //        myPort   myNodeId  myHashNode  mySuccessor   myHashSuccessor    myPredecessor    myHashPredecessor
                    //        portNumber:NodeId:hashedNode:  nextNode:     hashedNextNode:    previousNode:    hashedPreviousNode
                    //         0            1      2         3               4                    5                 6
                    String newHash1;
                    if(msgs[1]!=null){
                        newHash1 = genHash(msgs[1]);
                    }else{
                        newHash1 = "";
                    }
                    recordOfData.set(3,msgs[1]);
                    recordOfData.set(4,newHash1);
                    recordOfData.set(5,msgs[1]);
                    recordOfData.set(6,newHash1);
                }else{
                    msgToSend = "NodeJoin" + ":" +msgs[0] + ":" + msgs[1] + ":";
                    Integer portId = 0;
                    if(recordOfData.get(3).compareToIgnoreCase("5554")==0){
                        portId = 11108;
                    }else if(recordOfData.get(3).compareToIgnoreCase("5556")==0){
                        portId = 11112;
                    }else if(recordOfData.get(3).compareToIgnoreCase("5558")==0){
                        portId = 11116;
                    }else if(recordOfData.get(3).compareToIgnoreCase("5560")==0){
                        portId = 11120;
                    }
                    else if(recordOfData.get(3).compareToIgnoreCase("5562")==0){
                        portId = 11124;
                    }
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), portId);
                    writeOutputStream = new DataOutputStream(socket.getOutputStream());
                    writeOutputStream.write(msgToSend.getBytes());
                    writeOutputStream.flush();
                    socket.close();
                }
            } catch (UnknownHostException e) {
                Log.e(TAG,"Unknown host"+e.getMessage());
            }catch (Exception e){
                Log.e(TAG,e.getMessage());
            }
            return null;
        }
    }

    public boolean getPortNumber(String message,String temp){

        String hashNewkey;
        if(temp.compareToIgnoreCase("aaa")==0){
            hashNewkey = null;
        }else{
            hashNewkey = "";
        }
        try{
            hashNewkey = genHash(message);
        }catch(Exception e){
            e.printStackTrace();
        }

        if(hashNewkey.compareTo(recordOfData.get(6)) > 0 && hashNewkey.compareTo(recordOfData.get(2))<= 0) {
            return true;
        }else if(hashNewkey.compareTo(recordOfData.get(6)) > 0 && recordOfData.get(6).compareTo(recordOfData.get(2))>0){
            return true;
        }else if(hashNewkey.compareTo(recordOfData.get(2)) <= 0 && recordOfData.get(6).compareTo(recordOfData.get(2))>0){
            return true;
        } else {
            return false;
        }

    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
