package com.example.rpgapp.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;

public class Alliance {

    private String allianceId;
    private String name;
    private String leaderId;
    private String leaderUsername;
    private List<String> memberIds;
    private List<String> pendingInviteIds;
    private boolean missionStarted;
    @ServerTimestamp
    private Date createdAt;

    public Alliance() {}

    public Alliance(String name, String leaderId, String leaderUsername, List<String> memberIds, List<String> pendingInviteIds) {
        this.name = name;
        this.leaderId = leaderId;
        this.leaderUsername = leaderUsername;
        this.memberIds = memberIds;
        this.pendingInviteIds = pendingInviteIds;
        this.missionStarted = false;
    }


    public String getAllianceId() {
        return allianceId;
    }

    public void setAllianceId(String allianceId) {
        this.allianceId = allianceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }

    public String getLeaderUsername() {
        return leaderUsername;
    }

    public void setLeaderUsername(String leaderUsername) {
        this.leaderUsername = leaderUsername;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public int getMemberCount(){
        return memberIds.size();
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    public List<String> getPendingInviteIds() {
        return pendingInviteIds;
    }

    public void setPendingInviteIds(List<String> pendingInviteIds) {
        this.pendingInviteIds = pendingInviteIds;
    }

    public boolean isMissionStarted() {
        return missionStarted;
    }

    public void setMissionStarted(boolean missionStarted) {
        this.missionStarted = missionStarted;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}