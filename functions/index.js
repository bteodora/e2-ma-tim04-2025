const {onDocumentCreated, onDocumentUpdated} = require("firebase-functions/v2/firestore");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");
const logger = require("firebase-functions/logger");

initializeApp();

exports.sendAllianceInvites = onDocumentCreated("alliances/{allianceId}", async (event) => {
  const snap = event.data;
  if (!snap) {
    logger.log("sendAllianceInvites: Nema podataka u događaju, izlazim.");
    return;
  }
  const newAlliance = snap.data();

  const allianceName = newAlliance.name;
  const leaderUsername = newAlliance.leaderUsername;
  const invitedUserIds = newAlliance.pendingInviteIds;

  if (!invitedUserIds || invitedUserIds.length === 0) {
    logger.log("sendAllianceInvites: Nema pozvanih korisnika, funkcija se završava.");
    return;
  }

  const tokens = [];
  const db = getFirestore();
  for (const userId of invitedUserIds) {
    const userDoc = await db.collection("users").doc(userId).get();
    if (userDoc.exists && userDoc.data().fcmToken) {
      tokens.push(userDoc.data().fcmToken);
    }
  }

  if (tokens.length === 0) {
    logger.log("sendAllianceInvites: Nismo pronašli FCM tokene za pozvane korisnike.");
    return;
  }

  const messaging = getMessaging();
  const response = await messaging.sendEachForMulticast({
    tokens,
    data: {
      type: "ALLIANCE_INVITE",
      title: "Alliance Invitation!",
      body: `${leaderUsername} has invited you to join the alliance '${allianceName}'!`,
      allianceId: event.params.allianceId,
      leaderUsername: leaderUsername,
      allianceName: allianceName,
    },
    android: {
      priority: "high",
    },
    apns: {
      payload: {
        aps: { "content-available": 1 },
      },
      headers: {
        "apns-push-type": "background",
        "apns-priority": "10",
      },
    },
  });

  logger.log(`sendAllianceInvites: Poslate notifikacije na ${tokens.length} tokena.`);
  return response;
});

exports.notifyLeaderOnMemberJoin = onDocumentUpdated("alliances/{allianceId}", async (event) => {
  const beforeData = event.data.before.data();
  const afterData = event.data.after.data();

  if (!beforeData || !afterData) {
    logger.log("notifyLeaderOnMemberJoin: Nedostaju podaci pre ili posle promene.");
    return;
  }

  const oldMembers = beforeData.memberIds || [];
  const newMembers = afterData.memberIds || [];

  if (newMembers.length <= oldMembers.length) {
    return;
  }

  const newMemberId = newMembers.find((id) => !oldMembers.includes(id));

  if (!newMemberId) {
    logger.log("notifyLeaderOnMemberJoin: Nije pronađen ID novog člana.");
    return;
  }

  const db = getFirestore();
  const userDoc = await db.collection("users").doc(newMemberId).get();
  if (!userDoc.exists) {
    logger.log(`notifyLeaderOnMemberJoin: Korisnik sa ID-jem ${newMemberId} nije pronađen.`);
    return;
  }
  const newMemberUsername = userDoc.data().username;
  const leaderId = afterData.leaderId;
  const allianceName = afterData.name;

  const leaderDoc = await db.collection("users").doc(leaderId).get();
  if (!leaderDoc.exists || !leaderDoc.data().fcmToken) {
    logger.log(`notifyLeaderOnMemberJoin: Vođa sa ID-jem ${leaderId} nema FCM token.`);
    return;
  }
  const leaderToken = leaderDoc.data().fcmToken;

  const messaging = getMessaging();
  const response = await messaging.send({
    token: leaderToken,
    data: {
      type: "MEMBER_JOINED",
      title: "New Alliance Member!",
      body: `${newMemberUsername} has joined your alliance '${allianceName}'!`,
    },
    android: {
      priority: "high",
    },
  });

  logger.log(`notifyLeaderOnMemberJoin: Poslata notifikacija vođi (${leaderId}) o novom članu.`);
  return response;
});

exports.sendChatMessageNotification = onDocumentCreated("alliances/{allianceId}/messages/{messageId}", async (event) => {
  const messageData = event.data.data();
  const allianceId = event.params.allianceId;

  if (!messageData) {
    logger.log("Nema podataka o poruci.");
    return;
  }

  const senderId = messageData.senderId;
  const senderUsername = messageData.senderUsername;
  const messageText = messageData.text;

  const db = getFirestore();
  const allianceDoc = await db.collection("alliances").doc(allianceId).get();
  if (!allianceDoc.exists) {
    logger.log(`Savez sa ID-jem ${allianceId} nije pronađen.`);
    return;
  }
  const allianceData = allianceDoc.data();
  const allianceName = allianceData.name;
  const allMemberIds = allianceData.memberIds || [];

  const recipientIds = allMemberIds.filter((id) => id !== senderId);

  if (recipientIds.length === 0) {
    logger.log("Nema drugih članova kojima treba poslati notifikaciju.");
    return;
  }

  const tokens = [];
  for (const userId of recipientIds) {
    const userDoc = await db.collection("users").doc(userId).get();
    if (userDoc.exists && userDoc.data().fcmToken) {
      tokens.push(userDoc.data().fcmToken);
    }
  }

  if (tokens.length === 0) {
    logger.log("Nismo pronašli FCM tokene za primaoce poruke.");
    return;
  }

  const message = {
    data: {
      type: "NEW_CHAT_MESSAGE",
      title: `New message in ${allianceName}`,
      body: `${senderUsername}: ${messageText}`,
    },
    android: {
      priority: "normal",
    },
    tokens: tokens,
  };

  logger.log(`Slanje CHAT notifikacije na ${tokens.length} tokena.`);
  const messaging = getMessaging();
  return messaging.sendEachForMulticast(message);
});


exports.sendSingleAllianceInvite = onDocumentUpdated("alliances/{allianceId}", async (event) => {
  const beforeData = event.data.before.data();
  const afterData = event.data.after.data();

  if (!beforeData || !afterData) {
    logger.log("sendSingleAllianceInvite: Nedostaju podaci pre ili posle promene.");
    return;
  }

  const oldInvites = beforeData.pendingInviteIds || [];
  const newInvites = afterData.pendingInviteIds || [];

  if (newInvites.length <= oldInvites.length) {
    return;
  }

  const newInviteId = newInvites.find((id) => !oldInvites.includes(id));
  if (!newInviteId) {
    logger.log("sendSingleAllianceInvite: Nije pronađen ID novopozvanog korisnika.");
    return;
  }

  const db = getFirestore();
  const userDoc = await db.collection("users").doc(newInviteId).get();
  if (!userDoc.exists) {
    logger.log(`sendSingleAllianceInvite: Korisnik ${newInviteId} ne postoji.`);
    return;
  }

  const token = userDoc.data().fcmToken;
  if (!token) {
    logger.log(`sendSingleAllianceInvite: Korisnik ${newInviteId} nema FCM token.`);
    return;
  }

  const allianceName = afterData.name;
  const leaderUsername = afterData.leaderUsername;
  const allianceId = event.params.allianceId;

  const message = {
    token: token,
    notification: {
      title: "Alliance Invitation!",
      body: `${leaderUsername} has invited you to join '${allianceName}'!`,
    },
    data: {
      type: "ALLIANCE_INVITE",
      allianceId: allianceId,
      leaderUsername: leaderUsername,
      allianceName: allianceName,
    },
    android: { priority: "high" },
    apns: {
      headers: { "apns-priority": "10" },
      payload: { aps: { "content-available": 1 } },
    },
  };

  logger.log(`sendSingleAllianceInvite: Šaljem invite notifikaciju korisniku ${newInviteId}.`);
  const messaging = getMessaging();
  return messaging.send(message);
});