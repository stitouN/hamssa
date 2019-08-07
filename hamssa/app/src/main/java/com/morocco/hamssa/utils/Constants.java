package com.morocco.hamssa.utils;

public class Constants {

    public enum TASK{
        GET_TOPICS("getTopics"),
        GET_MESSAGES("getMessages"),
        CREATE_MESSAGE("createMessage"),
        CREATE_USER("createUser"),
        VOTE_MESSAGE("voteMessage"),
        UNVOTE_MESSAGE("unvoteMessage"),
        UPLOAD_IMAGE("uploadImage"),
        GET_MESSAGE("getMessage"),
        GET_MESSAGES_RECEIVED("getMessagesReceived"),
        GET_MESSAGES_SENT("getMessagesSent"),
        ADD_NOTIFICATION_TOKEN("addNotificationToken"),
        GET_MY_USER_INFO("getMyUserInfo"),
        CREATE_TOPIC("createTopic"),
        SET_EMAIL("setEmail"),
        LOGIN("login"),
        GET_TOPIC("getTopic"),
        GET_TOPIC_BY_USER("getTopicByUser"),
        REMOVE_TOPIC("removeTopic"),
        LOGOUT("logout"),
        RECOVER_PASSWORD("recoverPassword");

        private String name;
        TASK(String name){
            this.name = name;
        }
        public String toString(){
            return name;
        }
    }

    public static class SP{
        public static final String GCM_REGISTRATION_ID = "gcm_registration_id";
        public static final String USER_TOKEN = "user_token";
        public static final String USER_ID = "user_id";
        public static final String USER_ROLE = "user_role";
        public static final String USER_EMAIL = "user_email";
        public static final String USER_IMAGE_URL = "user_image_url";
        public static final String NOTIFY_FOLLOWING = "notify_following";
        public static final String NOTIFY_REFERENCES = "notify_references";
        public static final String NOTIFY_VOTES = "notify_votes";
    }

    public static String BACKEND = "http://localhost:8010/hamssa-47630/us-central1/";

    public static String CLIENT_TOKEN = "nza91FaAnc90afjwu8C2"; // chpmbl

    public static Long SOURCE_ID = 82271993221L; // El País chpmbl

    public static boolean SHOW_LINKS = true;

    // This flag controls whether every user can create a post or not
    public static boolean EVERYBODY_CAN_POST = true;
}
