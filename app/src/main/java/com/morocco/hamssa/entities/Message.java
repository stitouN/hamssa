package com.morocco.hamssa.entities;

/**
 * Created by hmontaner on 11/09/15.
 */
public class Message {
    String id, userId;
    Long votesUp, votesDown, time, ordinal;
    String name, text, imageURL,topicId;
    public Message(String id, String userId){
        this.id = id;
        this.userId = userId;
    }
    public Message(String id, String userId, String name, String text, Long votesUp, Long votesDown, Long time, Long ordinal, String imageURL, String topicId){
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.text = text;
        this.votesUp = votesUp;
        this.votesDown = votesDown;
        this.time = time;
        this.ordinal = ordinal;
        this.imageURL = imageURL;
        this.topicId = topicId;
    }
    public String getId(){
        return id;
    }
    public String getUserId(){
        return userId;
    }
    public String getName(){
        return name;
    }
    public String getText(){
        return text;
    }
    public Long getVotesUp(){
        return votesUp;
    }
    public Long getVotesDown(){
        return votesDown;
    }
    public Long getTime(){
        return time;
    }
    public Long getOrdinal(){
        return ordinal;
    }
    public String getImageURL(){
        return imageURL;
    }
    public String getTopicId(){ return topicId; }
}
