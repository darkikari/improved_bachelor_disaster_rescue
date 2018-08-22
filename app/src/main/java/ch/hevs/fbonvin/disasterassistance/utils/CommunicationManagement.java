package ch.hevs.fbonvin.disasterassistance.utils;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.nearby.connection.Payload;
import com.google.gson.Gson;

import java.util.ArrayList;

import ch.hevs.fbonvin.disasterassistance.MainActivity;
import ch.hevs.fbonvin.disasterassistance.R;
import ch.hevs.fbonvin.disasterassistance.models.Endpoint;
import ch.hevs.fbonvin.disasterassistance.models.Message;

import static ch.hevs.fbonvin.disasterassistance.Constant.FRAG_MESSAGES_SENT;
import static ch.hevs.fbonvin.disasterassistance.Constant.FRAG_MESSAGE_LIST;
import static ch.hevs.fbonvin.disasterassistance.Constant.HEADER_MESSAGE;
import static ch.hevs.fbonvin.disasterassistance.Constant.HEADER_UPDATE_STATUS;
import static ch.hevs.fbonvin.disasterassistance.Constant.MESSAGES_RECEIVED;
import static ch.hevs.fbonvin.disasterassistance.Constant.MESSAGE_QUEUE;
import static ch.hevs.fbonvin.disasterassistance.Constant.MESSAGE_QUEUE_DELETED;
import static ch.hevs.fbonvin.disasterassistance.Constant.MESSAGE_SENT;
import static ch.hevs.fbonvin.disasterassistance.Constant.MESSAGE_SEPARATOR;
import static ch.hevs.fbonvin.disasterassistance.Constant.MESSAGE_STATUS_DELETE;
import static ch.hevs.fbonvin.disasterassistance.Constant.MESSAGE_STATUS_NEW;
import static ch.hevs.fbonvin.disasterassistance.Constant.MESSAGE_STATUS_UPDATE;
import static ch.hevs.fbonvin.disasterassistance.Constant.NEARBY_MANAGEMENT;
import static ch.hevs.fbonvin.disasterassistance.Constant.TAG;
import static ch.hevs.fbonvin.disasterassistance.Constant.UPDATE_MESSAGE_STATUS_NON_OK;
import static ch.hevs.fbonvin.disasterassistance.Constant.UPDATE_MESSAGE_STATUS_OK;
import static ch.hevs.fbonvin.disasterassistance.Constant.VALUE_PREF_APPID;

public abstract class CommunicationManagement {

    private static Application sApplication;
    private static final int NOTI_PRIMARY1 = 1100;
    private static final int NOTI_PRIMARY2 = 1101;
    private static final int NOTI_SECONDARY1 = 1200;
    private static final int NOTI_SECONDARY2 = 1201;




    public static Application getApplication() {
        return sApplication;
    }

    public static Context getContext() {
        return getApplication().getApplicationContext();
    }
    /**
     * Send a message to a list of recipient
     * @param sendTo list of recipient
     * @param message message to send
     */
    public static void sendMessageListRecipient(ArrayList<String> sendTo, Message message){
        Gson gson = new Gson();
        String[] content = new String[]{HEADER_MESSAGE, message.toString()};

        NEARBY_MANAGEMENT.sendDataAsByteListRecipient(sendTo, gson.toJson(content));
    }


    /**
     * Send a message to a list of recipient
     * @param sendTo list of recipient
     * @param message message to send
     */
    private static void sendMessageUniqueRecipient(String sendTo, Message message){
        Gson gson = new Gson();
        String[] content = {HEADER_MESSAGE, message.toString()};

        NEARBY_MANAGEMENT.sendDataAsByteUniqueRecipient(sendTo, gson.toJson(content));
    }

    /**
     *  Delete a message to a list of recipient
     * @param sendTo list of recipient
     * @param message message to delete
     */
    public static void sendMessageDeletion(ArrayList<String> sendTo, Message message){
        message.setMessageStatus(MESSAGE_STATUS_DELETE);

        Gson gson = new Gson();
        String[] content = new String[]{HEADER_MESSAGE, message.toString()};

        NEARBY_MANAGEMENT.sendDataAsByteListRecipient(sendTo, gson.toJson(content));
    }

    /**
     * Send a message update to a list of recipient
     * @param sendTo list of recipient
     * @param message message to send
     * @param status the status if the message to update
     */
    public static void sendUpdateMessage(ArrayList<String> sendTo, Message message, String status){

        String update = message.getDateCreatedMillis() + MESSAGE_SEPARATOR + message.getTitle() + MESSAGE_SEPARATOR + status;

        Gson gson = new Gson();
        String [] content = new String[]{HEADER_UPDATE_STATUS, update};
        NEARBY_MANAGEMENT.sendDataAsByteListRecipient(sendTo, gson.toJson(content));
    }



    /**
     * Check and send all messages to a new connected peers
     * @param endpoint information about the new peer
     */
    public static void sendAllMessagesNewPeer(Endpoint endpoint){
        Log.i(TAG, "sendAllMessagesNewPeer: " + endpoint.getName());

        ArrayList<Message> listMessage = new ArrayList<>();

        //Update the expiration date of the message with the current time
        for(Message m : MESSAGE_QUEUE){
            m.updateExpirationDate();
        }

        listMessage.addAll(checkRecipientMessages(MESSAGES_RECEIVED, endpoint));
        listMessage.addAll(checkRecipientMessages(MESSAGE_QUEUE, endpoint));
        listMessage.addAll(checkRecipientMessages(MESSAGE_SENT, endpoint));
        listMessage.addAll(MESSAGE_QUEUE_DELETED);

        Log.i(TAG, "sendAllMessagesNewPeer: messages sent to the new peer " + listMessage.size());

        //Messages are ordered by distance, to send first the closest message of the peer
        MessagesManagement.OrderByDistance(listMessage);

        for (Message m : listMessage){
            if(MESSAGE_QUEUE.contains(m)){
                Log.i(TAG, "sendAllMessagesNewPeer: remove message from MESSAGE_QUEUE");
                MESSAGE_QUEUE.remove(m);
                FRAG_MESSAGES_SENT.updateDisplay(m);
            }
            if(MESSAGE_QUEUE_DELETED.contains(m)){
                Log.i(TAG, "sendAllMessagesNewPeer: remove message from MESSAGE_QUEUE_DELETED");
                MESSAGE_QUEUE_DELETED.remove(m);
            }

            sendMessageUniqueRecipient(endpoint.getId(), m);
        }
    }



    /**
     * Handle the payload received and forward them to correcting methods depending on the type
     * @param payload is the data received
     */
    public static void receivePayload(Payload payload) {

        switch (payload.getType()){
            case Payload.Type.BYTES:
                Log.i(TAG, "CommunicationManagement receivePayload: payload as byte received");
                receivePayloadBytes(payload);
                break;
            case Payload.Type.FILE:
                break;
            case Payload.Type.STREAM:
            default:
                Log.e(TAG, "CommunicationManagement receivePayload: unknown payload type");
        }
    }



    /**
     * Handle the reception of a byte payload
     * @param payload data received
     */
    private static void receivePayloadBytes(Payload payload){

        Gson gson = new Gson();

        String s = new String(payload.asBytes());

        String[] payloadStringArray = gson.fromJson(s, String[].class);

        switch (payloadStringArray[0].trim()){
            case HEADER_MESSAGE:
                Log.i(TAG, "CommunicationManagement receivePayloadBytes: new message received");
                receiveMessage(payloadStringArray[1]);
                break;
            case HEADER_UPDATE_STATUS:
                Log.i(TAG, "CommunicationManagement receivePayloadBytes: message status update");
                receiveMessageUpdate(payloadStringArray[1]);
                break;

            default:
                Log.e(TAG, "CommunicationManagement receivePayloadBytes: unknown header type " + payloadStringArray[0]);
        }
    }

    /**
     * Handle all the payload received as byte, create new message
     * @param payload data to handle
     */
    private static void receiveMessage(String payload) {
        Log.i(TAG, "New message incoming");
        Message m = getMessageFromString(payload);

        m.setDistance(LocationManagement.getDistance(m.getTitle(), m.getMessageLatitude(), m.getMessageLongitude()));

        if(m != null){
            switch (m.getMessageStatus()){
                case MESSAGE_STATUS_NEW:
                    Log.i(TAG, "CommunicationManagement payloadAsByte: handle new message");
                    handleNewMessages(m);
                    break;
                case MESSAGE_STATUS_DELETE:
                    Log.i(TAG, "CommunicationManagement payloadAsByte: handle message deletion");
                    handleMessageDeletion(m);
                    break;
                case MESSAGE_STATUS_UPDATE:
                    break;
                default:
                    Log.e(TAG, "CommunicationManagement payloadAsByte: unknown message status" + m.getMessageStatus());
            }

        }
    }

    /**
     * Handle the reception of a new message, avoid duplicate messages by checking the list of recipient
     * @param m message to handle
     */
    private static void handleNewMessages(Message m){

        boolean flagAlreadyReceived = true;


        if(!MESSAGES_RECEIVED.contains(m) && !m.getCreatorAppId().equals(VALUE_PREF_APPID)){
            flagAlreadyReceived = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                sendNotificationOreo(NOTI_PRIMARY1, "New message received");
            } else {
                sendNotificationOld("New message received");
            }

        } else {
            Log.i(TAG, "CommunicationManagement handleNewMessages: message already received " + m.getTitle());
            MESSAGES_RECEIVED.get(MESSAGES_RECEIVED.indexOf(m)).setDateExpirationMillis(m.getDateExpirationMillis());
        }

        if(!flagAlreadyReceived){
            Log.i(TAG, "CommunicationManagement handleNewMessages: new message received");
            MESSAGES_RECEIVED.add(m);
            FRAG_MESSAGE_LIST.updateMessages();
        }
    }


    /**
     * Handle the reception and deletion of a message.
     * @param m message to handle
     */
    private static void handleMessageDeletion(Message m) {

        int toDelete = -1;

        for (int i = 0; i < MESSAGES_RECEIVED.size(); i++){
            if(MESSAGES_RECEIVED.get(i).getDateCreatedMillis().equals(m.getDateCreatedMillis()) &&
                    MESSAGES_RECEIVED.get(i).getCreatorAppId().equals(m.getCreatorAppId())) {

                toDelete = i;
            }
        }

        if(toDelete != -1){
            FRAG_MESSAGE_LIST.removeItem(toDelete);
        }
    }


    /**
     * Handle the reception of message update, it can extend or reduce the remaining time of the message
     * @param payload message to update
     */
    private static void receiveMessageUpdate(String payload){

        String[] data = payload.split(MESSAGE_SEPARATOR);

        String msgDate = data[0];
        String msgTitle = data[1];
        String status = data[2];

        Message toUpdate = null;
        for (Message m : MESSAGES_RECEIVED){
            if(m.getTitle().equals(msgTitle) && m.getDateCreatedMillis().equals(msgDate)){
                toUpdate = m;
            }
        }

        if(toUpdate != null){
            switch (status){
                case UPDATE_MESSAGE_STATUS_OK:
                    handleMessageStatusOk(toUpdate);
                    break;
                case UPDATE_MESSAGE_STATUS_NON_OK:
                    handleMessageStatusNonOk(toUpdate);
                    break;
                default:
                    Log.i(TAG, "receiveMessageUpdate: unknown update status: " + status);
            }
        }
    }

    /**
     * Extend the time of the message
     * @param toUpdate message to update
     */
    private static void handleMessageStatusOk(Message toUpdate) {
        toUpdate.extendExpirationDate();
        FRAG_MESSAGE_LIST.updateDisplay();
    }

    /**
     * Reduce the time of the message
     * @param toUpdate message to update
     */
    private static void handleMessageStatusNonOk(Message toUpdate) {
        toUpdate.decreaseExpirationDate();
        FRAG_MESSAGE_LIST.updateDisplay();
    }


    /**
     * Check the content of messages list to know if it has already been send to the endpoint
     * @param messages ArrayList of messages to test
     * @param endpoint information about the peer
     * @return list of messages that can be send to the endpoint
     */
    private static ArrayList<Message> checkRecipientMessages(ArrayList<Message> messages, Endpoint endpoint){

        ArrayList<Message> listMessage = new ArrayList<>();

        if(messages.size() > 0){

            for (Message m : messages){
                if(!m.getMessageSentTo().contains(endpoint.getName())){
                    m.getMessageSentTo().add(endpoint.getName());

                    listMessage.add(m);
                }
            }
        }

        return listMessage;
    }


    /**
     * Try to convert a string to a  message object
     * @param payload string to convert
     * @return message created
     */
    private static Message getMessageFromString(String payload){

        Message message = null;

        try {
            message = Message.createFromPayload(payload);
        } catch (Exception e) {
            Log.w(TAG, "payloadAsByte: ", e);
        }
        return message;
    }

    /**
     * Send activity notifications.
     *
     * @param id The ID of the notification to create
     * @param title The title of the notification
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void sendNotificationOreo(int id, String text) {

        NotificationHelper noti = new NotificationHelper(MainActivity.getAppContext());

        Notification.Builder nb = null;
        switch (id) {
            case NOTI_PRIMARY1:
                nb = noti.getNotification1(text, String.valueOf(R.string.primary1_body));
                break;

//            case NOTI_PRIMARY2:
//                nb = noti.getNotification1(title, String.valueOf(R.string.primary2_body));
//                break;

            case NOTI_SECONDARY1:
                nb = noti.getNotification2(text, String.valueOf(R.string.secondary1_body));
                break;

//            case NOTI_SECONDARY2:
//                nb = noti.getNotification2(title, String.valueOf(R.string.secondary2_body));
//                break;
        }
        if (nb != null) {
            noti.notify(id, nb);
        }
    }

    public static void sendNotificationOld(String text) {

        final int MY_NOTIFICATION_ID=1;
        NotificationManager notificationManager;
        Notification myNotification;

        Intent myIntent = new Intent(MainActivity.getAppContext(), MainActivity.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.getAppContext(),0, myIntent, 0);


        myNotification = new NotificationCompat.Builder(MainActivity.getAppContext())
                .setContentTitle("Notification from Floater")
                .setContentText(text)
                .setTicker("Notification!")
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.logo_git)
                .build();

        notificationManager =
                (NotificationManager)MainActivity.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(MY_NOTIFICATION_ID, myNotification);
    }

}

