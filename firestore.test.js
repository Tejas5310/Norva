const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validPrivateUserPayload(uid, email, now) {
  return {
    userId: uid,
    email,
    displayName: "Alice Rivera",
    preferredLanguage: "English",
    themeMode: "DARK",
    feedDiversity: 50,
    privateAccount: false,
    createdAt: now,
    updatedAt: now,
  };
}

function validPublicProfilePayload(uid, now) {
  return {
    userId: uid,
    handle: "alice_dev",
    displayName: "Alice Rivera",
    bio: "Building spatial AI & creator tools.",
    avatarInitials: "AR",
    accentColorHex: "#00E5FF",
    interests: ["AI", "Design", "Creators"],
    creatorTier: "CREATOR",
    verifiedBadge: false,
    discoverable: true,
    subscriptionPriceUsd: 4.99,
    followersCount: 0,
    followingCount: 0,
    tipsEarnedCents: 0,
    createdAt: now,
    updatedAt: now,
  };
}

function validPostPayload(uid, now) {
  return {
    authorId: uid,
    authorName: "Alice Rivera",
    authorHandle: "alice_dev",
    authorVerified: false,
    visibility: "PUBLIC",
    mediaType: "TEXT",
    content: "Launching our new open creator economy on Novara!",
    videoDurationSec: 0,
    category: "Tech",
    tags: ["novara", "ai"],
    pollOptions: [],
    pollVotes: [],
    aiGenerated: false,
    likesCount: 0,
    commentsCount: 0,
    repostsCount: 0,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: denied across private users, public profiles, posts, communities, conversations", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("publicProfiles").where("discoverable", "==", true).get());
  await assertFails(unauthDb.collection("posts").where("visibility", "==", "PUBLIC").get());
  await assertFails(
    unauthDb.collection("posts").doc("post_1").set(validPostPayload(ALICE_UID, now))
  );
  await assertFails(unauthDb.collection("communities").where("visibility", "==", "PUBLIC").get());
  await assertFails(
    unauthDb.collection("conversations").where("memberUids", "array-contains", ALICE_UID).get()
  );
});

test("Private user PII isolation & savedPosts subcollection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validPrivateUserPayload(ALICE_UID, "alice@example.com", now))
  );

  // Bob cannot read or update Alice's private PII document
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hijacked" }));
  await assertFails(bobDb.collection("users").get());

  // Alice can partially update her preferences while keeping createdAt immutable
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "Spanish",
      themeMode: "LIGHT",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // SavedPosts subcollection
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("savedPosts").doc("post_1").set({
      postId: "post_1",
      authorName: "Bob",
      previewText: "Great thread",
      mediaType: "TEXT",
      createdAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("savedPosts").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("savedPosts").get()
  );
});

test("PublicProfile: self-verification blocked, shadow ghost fields blocked, filtered list succeeds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Cannot create profile already verified
  const spoofed = { ...validPublicProfilePayload(ALICE_UID, now), verifiedBadge: true };
  await assertFails(aliceDb.collection("publicProfiles").doc(ALICE_UID).set(spoofed));

  // Cannot create profile with ghost field or @DocumentId "id"
  const ghost = { ...validPublicProfilePayload(ALICE_UID, now), id: ALICE_UID };
  await assertFails(aliceDb.collection("publicProfiles").doc(ALICE_UID).set(ghost));

  // Valid create succeeds
  await assertSucceeds(
    aliceDb.collection("publicProfiles").doc(ALICE_UID).set(validPublicProfilePayload(ALICE_UID, now))
  );

  // Alice cannot self-verify on update
  await assertFails(
    aliceDb.collection("publicProfiles").doc(ALICE_UID).update({
      verifiedBadge: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Filtered discovery query succeeds; unfiltered query fails
  await assertSucceeds(
    bobDb.collection("publicProfiles").where("discoverable", "==", true).get()
  );
  await assertFails(bobDb.collection("publicProfiles").get());

  // Bob can tip Alice's profile tipsEarnedCents atomically
  await assertSucceeds(
    bobDb.collection("publicProfiles").doc(ALICE_UID).update({
      tipsEarnedCents: 500,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Posts, Likes, Comments, and Client Query Alignment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 3000);

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("posts").doc("post_future").set(
      validPostPayload(ALICE_UID, new Date(Date.now() + 60000))
    )
  );

  // Valid create succeeds
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set(validPostPayload(ALICE_UID, now))
  );

  // Filtered feed query succeeds; unfiltered collection list fails
  await assertSucceeds(
    bobDb.collection("posts").where("visibility", "==", "PUBLIC").get()
  );
  await assertSucceeds(
    aliceDb.collection("posts").where("authorId", "==", ALICE_UID).get()
  );
  await assertFails(bobDb.collection("posts").get());

  // Bob likes post_1 and comments
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("likes").doc(BOB_UID).set({
      userId: BOB_UID,
      createdAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").update({
      likesCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // Bob cannot jump likesCount by +500
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      likesCount: 500,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("comments").doc("c_1").set({
      postId: "post_1",
      authorId: BOB_UID,
      authorName: "Bob",
      authorHandle: "bob_creates",
      text: "Inspiring vision!",
      createdAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").collection("comments").where("postId", "==", "post_1").get()
  );
});

test("Communities, Conversations, Messages, Tips, and Email-Verified Admin Checks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "tejashpatel02010@gmail.com",
    email_verified: false,
  }).firestore();
  const verifiedAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "tejashpatel02010@gmail.com",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 2000);

  // Community creation & discussion
  await assertSucceeds(
    aliceDb.collection("communities").doc("comm_1").set({
      ownerId: ALICE_UID,
      name: "AI Builders Guild",
      description: "Global collective for generative media & mobile AI.",
      category: "Tech",
      visibility: "PUBLIC",
      memberUids: [ALICE_UID],
      membersCount: 1,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("communities").where("visibility", "==", "PUBLIC").get()
  );
  await assertSucceeds(
    bobDb.collection("communities").doc("comm_1").collection("discussions").doc("disc_1").set({
      communityId: "comm_1",
      authorId: BOB_UID,
      authorName: "Bob",
      title: "Welcome thread",
      body: "Excited to build together!",
      createdAt: now,
    })
  );

  // Conversation & messages with memberUids isolation
  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_1").set({
      title: "Alice & Bob",
      groupChat: false,
      memberUids: [ALICE_UID, BOB_UID],
      memberNames: ["Alice", "Bob"],
      lastMessageText: "Hey!",
      lastSenderId: ALICE_UID,
      disappearingEnabled: false,
      encryptedBadge: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("conversations").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertFails(
    testEnv.authenticatedContext("charlie_999").firestore().collection("conversations").doc("conv_1").get()
  );
  await assertSucceeds(
    bobDb.collection("conversations").doc("conv_1").collection("messages").doc("msg_1").set({
      conversationId: "conv_1",
      senderId: BOB_UID,
      senderName: "Bob",
      text: "Hola desde Madrid!",
      reactions: [],
      memberUids: [ALICE_UID, BOB_UID],
      editedFlag: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Tips split check (95/5)
  await assertSucceeds(
    bobDb.collection("tips").doc("tip_1").set({
      senderId: BOB_UID,
      senderName: "Bob",
      recipientId: ALICE_UID,
      recipientName: "Alice",
      giftType: "DIAMOND",
      amountCents: 1000,
      creatorNetCents: 950,
      platformFeeCents: 50,
      note: "Awesome stream!",
      createdAt: now,
    })
  );
  // Cannot tip yourself
  await assertFails(
    aliceDb.collection("tips").doc("tip_self").set({
      senderId: ALICE_UID,
      senderName: "Alice",
      recipientId: ALICE_UID,
      recipientName: "Alice",
      giftType: "SPARK",
      amountCents: 500,
      creatorNetCents: 475,
      platformFeeCents: 25,
      note: "Self tip",
      createdAt: now,
    })
  );

  // Moderation report & Email-Spoofing Red Team Test
  await assertSucceeds(
    bobDb.collection("reports").doc("rep_1").set({
      reporterId: BOB_UID,
      targetType: "POST",
      targetId: "post_1",
      reason: "Spam link check",
      status: "OPEN",
      createdAt: now,
      updatedAt: now,
    })
  );
  // Spoofed admin with email_verified: false MUST fail
  await assertFails(
    spoofedAdminDb.collection("reports").doc("rep_1").update({
      status: "RESOLVED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  // Verified admin succeeds
  await assertSucceeds(
    verifiedAdminDb.collection("reports").doc("rep_1").update({
      status: "RESOLVED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
