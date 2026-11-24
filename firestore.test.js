const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ELDER_UID = "elder_uid_001";
const CARETAKER_UID = "caretaker_uid_002";
const STRANGER_UID = "stranger_uid_999";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

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

test("Unauthenticated users are denied across all collections", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("users").doc(ELDER_UID).get());
  await assertFails(db.collection("elder_profiles").get());
  await assertFails(db.collection("medicines").get());
  await assertFails(db.collection("sos_events").get());
  await assertFails(
    db.collection("users").doc(ELDER_UID).set({
      userId: ELDER_UID,
      displayName: "Anon",
      role: "elder",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("User profile isolation and strict schema validation", async () => {
  const elderDb = testEnv.authenticatedContext(ELDER_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const now = pastDate(5000);

  await assertSucceeds(
    elderDb.collection("users").doc(ELDER_UID).set({
      userId: ELDER_UID,
      displayName: "Grandpa Arthur",
      role: "elder",
      email: "arthur@example.com",
      avatarKey: "grandpa",
      themePreset: "classic_pastel",
      darkMode: false,
      textScale: "normal",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Stranger cannot read elder's private user document
  await assertFails(strangerDb.collection("users").doc(ELDER_UID).get());

  // Ghost field rejected
  await assertFails(
    elderDb.collection("users").doc(ELDER_UID).update({
      isAdmin: true,
      updatedAt: pastDate(1000),
    })
  );

  // Partial update of theme preferences succeeds
  await assertSucceeds(
    elderDb.collection("users").doc(ELDER_UID).update({
      themePreset: "warm_cream",
      darkMode: true,
      textScale: "large",
      updatedAt: pastDate(1000),
    })
  );
});

function futureDate(msOffset = 3600000) {
  return new Date(Date.now() + msOffset);
}

test("Consent-based daily pairing code flow links caretaker to elder profile atomically", async () => {
  const elderDb = testEnv.authenticatedContext(ELDER_UID).firestore();
  const caretakerDb = testEnv.authenticatedContext(CARETAKER_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const created = pastDate(10000);
  const expiresTomorrow = futureDate(86400000);

  // 1. Elder creates own elder_profile
  await assertSucceeds(
    elderDb.collection("elder_profiles").doc(ELDER_UID).set({
      elderId: ELDER_UID,
      displayName: "Grandpa Arthur",
      dailyWaterGoal: 8,
      locationSharingEnabled: true,
      onlineStatus: "online",
      avatarKey: "grandpa",
      authorizedUids: [ELDER_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  // 2. Caretaker cannot read elder_profile before linking
  await assertFails(caretakerDb.collection("elder_profiles").doc(ELDER_UID).get());

  // 3. Elder creates 6-char daily pairing invite (and can delete old invites)
  const oldCode = "OLD111";
  await assertSucceeds(
    elderDb.collection("pairing_invites").doc(oldCode).set({
      code: oldCode,
      elderId: ELDER_UID,
      elderName: "Grandpa Arthur",
      status: "active",
      dayKey: "2026-11-23",
      expiresAt: expiresTomorrow,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(elderDb.collection("pairing_invites").doc(oldCode).delete());

  const code = "CARE88";
  await assertSucceeds(
    elderDb.collection("pairing_invites").doc(code).set({
      code,
      elderId: ELDER_UID,
      elderName: "Grandpa Arthur",
      status: "active",
      dayKey: "2026-11-24",
      expiresAt: expiresTomorrow,
      createdAt: created,
      updatedAt: created,
    })
  );

  // 4. Caretaker claims invite WITHOUT pre-reading elder_profiles, creates caretaker_link, and updates authorizedUids
  const now = pastDate(1000);
  const batch = caretakerDb.batch();
  batch.set(
    caretakerDb.collection("caretaker_links").doc(`${ELDER_UID}_${CARETAKER_UID}`),
    {
      elderId: ELDER_UID,
      elderName: "Grandpa Arthur",
      caretakerId: CARETAKER_UID,
      caretakerName: "Sarah",
      inviteCode: code,
      status: "active",
      authorizedUids: [ELDER_UID, CARETAKER_UID],
      createdAt: now,
      updatedAt: now,
    }
  );
  batch.update(caretakerDb.collection("pairing_invites").doc(code), {
    status: "claimed",
    claimedByUid: CARETAKER_UID,
    updatedAt: now,
  });
  batch.update(caretakerDb.collection("elder_profiles").doc(ELDER_UID), {
    authorizedUids: [ELDER_UID, CARETAKER_UID],
    updatedAt: now,
  });
  await assertSucceeds(batch.commit());

  // 5. Linked caretaker can now query elder_profiles with array-contains
  await assertSucceeds(
    caretakerDb
      .collection("elder_profiles")
      .where("authorizedUids", "array-contains", CARETAKER_UID)
      .get()
  );

  // 6. Elder deletes claimed invite once claimed
  await assertSucceeds(elderDb.collection("pairing_invites").doc(code).delete());

  // 7. Expired pairing invites cannot be claimed
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("pairing_invites").doc("EXP999").set({
      code: "EXP999",
      elderId: ELDER_UID,
      elderName: "Grandpa Arthur",
      status: "active",
      dayKey: "2026-11-23",
      expiresAt: pastDate(60000),
      createdAt: created,
      updatedAt: created,
    });
  });
  await assertFails(
    strangerDb.collection("pairing_invites").doc("EXP999").update({
      status: "claimed",
      claimedByUid: STRANGER_UID,
      updatedAt: pastDate(500),
    })
  );

  // 8. Elder revokes caretaker, then caretaker re-links with a fresh daily code
  const revokeBatch = elderDb.batch();
  revokeBatch.update(
    elderDb.collection("caretaker_links").doc(`${ELDER_UID}_${CARETAKER_UID}`),
    { status: "revoked", updatedAt: pastDate(400) }
  );
  revokeBatch.update(
    elderDb.collection("elder_profiles").doc(ELDER_UID),
    { authorizedUids: [ELDER_UID], updatedAt: pastDate(400) }
  );
  await assertSucceeds(revokeBatch.commit());

  const newCode = "DAY277";
  await assertSucceeds(
    elderDb.collection("pairing_invites").doc(newCode).set({
      code: newCode,
      elderId: ELDER_UID,
      elderName: "Grandpa Arthur",
      status: "active",
      dayKey: "2026-11-25",
      expiresAt: expiresTomorrow,
      createdAt: pastDate(300),
      updatedAt: pastDate(300),
    })
  );
  const relinkBatch = caretakerDb.batch();
  relinkBatch.update(
    caretakerDb.collection("caretaker_links").doc(`${ELDER_UID}_${CARETAKER_UID}`),
    {
      elderName: "Grandpa Arthur",
      caretakerName: "Sarah",
      inviteCode: newCode,
      status: "active",
      authorizedUids: [ELDER_UID, CARETAKER_UID],
      updatedAt: pastDate(200),
    }
  );
  relinkBatch.update(caretakerDb.collection("pairing_invites").doc(newCode), {
    status: "claimed",
    claimedByUid: CARETAKER_UID,
    updatedAt: pastDate(200),
  });
  relinkBatch.update(caretakerDb.collection("elder_profiles").doc(ELDER_UID), {
    authorizedUids: [ELDER_UID, CARETAKER_UID],
    updatedAt: pastDate(200),
  });
  await assertSucceeds(relinkBatch.commit());

  // 9. Unfiltered list query and stranger access remain denied
  await assertFails(caretakerDb.collection("elder_profiles").get());
  await assertFails(
    strangerDb
      .collection("elder_profiles")
      .where("authorizedUids", "array-contains", CARETAKER_UID)
      .get()
  );
});

test("Medicine, water, tasks, contacts, appointments, notifications, and SOS workflows", async () => {
  const elderDb = testEnv.authenticatedContext(ELDER_UID).firestore();
  const caretakerDb = testEnv.authenticatedContext(CARETAKER_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const created = pastDate(10000);

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("elder_profiles").doc(ELDER_UID).set({
      elderId: ELDER_UID,
      displayName: "Grandpa Arthur",
      dailyWaterGoal: 8,
      locationSharingEnabled: true,
      onlineStatus: "online",
      avatarKey: "grandpa",
      authorizedUids: [ELDER_UID, CARETAKER_UID],
      createdAt: created,
      updatedAt: created,
    });
  });

  // Caretaker adds medicine (no `id` field)
  await assertSucceeds(
    caretakerDb.collection("medicines").doc("med_01").set({
      elderId: ELDER_UID,
      name: "Vitamin D3",
      dosage: "1000 IU (Prescription)",
      timeMinutes: 1200,
      timeLabel: "8:00 PM",
      repeatSchedule: "Daily",
      activeStatus: "active",
      createdByUid: CARETAKER_UID,
      authorizedUids: [ELDER_UID, CARETAKER_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject `@DocumentId` id field inside payload
  await assertFails(
    caretakerDb.collection("medicines").doc("med_bad").set({
      id: "med_bad",
      elderId: ELDER_UID,
      name: "Bad",
      dosage: "1",
      timeMinutes: 600,
      timeLabel: "10:00 AM",
      repeatSchedule: "Daily",
      activeStatus: "active",
      createdByUid: CARETAKER_UID,
      authorizedUids: [ELDER_UID, CARETAKER_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Elder logs medicine status
  await assertSucceeds(
    elderDb.collection("medicine_logs").doc("med_01_20261124").set({
      medicineId: "med_01",
      elderId: ELDER_UID,
      medicineName: "Vitamin D3",
      dosage: "1000 IU (Prescription)",
      dateKey: "2026-11-24",
      status: "taken",
      loggedByUid: ELDER_UID,
      authorizedUids: [ELDER_UID, CARETAKER_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Elder updates water log
  await assertSucceeds(
    elderDb.collection("water_logs").doc(`${ELDER_UID}_20261124`).set({
      elderId: ELDER_UID,
      dateKey: "2026-11-24",
      glasses: 4,
      goal: 8,
      authorizedUids: [ELDER_UID, CARETAKER_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Caretaker adds task, emergency contact, appointment
  await assertSucceeds(
    caretakerDb.collection("tasks").doc("task_01").set({
      elderId: ELDER_UID,
      title: "Evening walk for 15 minutes",
      dueDateText: "Today",
      dueTimeText: "5:30 PM",
      completionStatus: "pending",
      assignedByUid: CARETAKER_UID,
      authorizedUids: [ELDER_UID, CARETAKER_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    caretakerDb.collection("emergency_contacts").doc("contact_01").set({
      elderId: ELDER_UID,
      name: "Sarah (Daughter)",
      phoneNumber: "+1-555-0101",
      relationship: "Daughter",
      priorityOrder: 1,
      authorizedUids: [ELDER_UID, CARETAKER_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    caretakerDb.collection("appointments").doc("appt_01").set({
      elderId: ELDER_UID,
      title: "Dr. Rao Checkup",
      location: "City Health Clinic",
      dateTimeText: "Nov 28, 10:30 AM",
      reminderFlag: "enabled",
      authorizedUids: [ELDER_UID, CARETAKER_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Elder triggers SOS, Caretaker acknowledges, then Elder cancels (terminal lock prevents post-cancel edits)
  await assertSucceeds(
    elderDb.collection("sos_events").doc("sos_01").set({
      elderId: ELDER_UID,
      elderName: "Grandpa Arthur",
      status: "active",
      areaLabel: "Near Oak Street",
      latitude: 37.422,
      longitude: -122.084,
      deliveryState: "delivered",
      authorizedUids: [ELDER_UID, CARETAKER_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    caretakerDb.collection("sos_events").doc("sos_01").update({
      status: "acknowledged",
      deliveryState: "acknowledged",
      acknowledgedByUid: CARETAKER_UID,
      updatedAt: pastDate(1000),
    })
  );

  await assertSucceeds(
    elderDb.collection("sos_events").doc("sos_01").update({
      status: "cancelled",
      deliveryState: "cancelled",
      updatedAt: pastDate(500),
    })
  );

  // Terminal state lock: cancelled SOS cannot be reopened or deleted
  await assertFails(
    elderDb.collection("sos_events").doc("sos_01").update({
      status: "active",
      deliveryState: "delivered",
      updatedAt: pastDate(200),
    })
  );
  await assertFails(elderDb.collection("sos_events").doc("sos_01").delete());

  // Stranger cannot query any elder care collection
  await assertFails(
    strangerDb
      .collection("medicines")
      .where("authorizedUids", "array-contains", CARETAKER_UID)
      .get()
  );
});

test("Video calls and fall-detection SOS workflows enforce member authorization", async () => {
  const elderDb = testEnv.authenticatedContext(ELDER_UID).firestore();
  const caretakerDb = testEnv.authenticatedContext(CARETAKER_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const created = pastDate(8000);

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("elder_profiles").doc(ELDER_UID).set({
      elderId: ELDER_UID,
      displayName: "Grandpa Arthur",
      dailyWaterGoal: 8,
      locationSharingEnabled: true,
      onlineStatus: "online",
      avatarKey: "grandpa",
      fallDetectionEnabled: true,
      authorizedUids: [ELDER_UID, CARETAKER_UID],
      createdAt: created,
      updatedAt: created,
    });
  });

  // Linked caretaker initiates video call to elder
  await assertSucceeds(
    caretakerDb.collection("video_calls").doc("call_01").set({
      elderId: ELDER_UID,
      elderName: "Grandpa Arthur",
      callerUid: CARETAKER_UID,
      callerName: "Sarah",
      callerRole: "caretaker",
      roomCode: "CARE9482ABCD",
      callStatus: "ringing",
      callerCameraEnabled: true,
      calleeCameraEnabled: true,
      callerMicMuted: false,
      calleeMicMuted: false,
      authorizedUids: [ELDER_UID, CARETAKER_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Elder answers and toggles camera/mic, then ends call
  await assertSucceeds(
    elderDb.collection("video_calls").doc("call_01").update({
      callStatus: "active",
      calleeCameraEnabled: true,
      updatedAt: pastDate(3000),
    })
  );
  await assertSucceeds(
    elderDb.collection("video_calls").doc("call_01").update({
      callStatus: "ended",
      updatedAt: pastDate(1000),
    })
  );

  // Stranger cannot read, list, create, or update video_calls
  await assertFails(strangerDb.collection("video_calls").doc("call_01").get());
  await assertFails(
    strangerDb.collection("video_calls").doc("call_bad").set({
      elderId: ELDER_UID,
      elderName: "Grandpa Arthur",
      callerUid: STRANGER_UID,
      callerName: "Stranger",
      callerRole: "caretaker",
      roomCode: "HACK12345678",
      callStatus: "ringing",
      callerCameraEnabled: true,
      calleeCameraEnabled: true,
      callerMicMuted: false,
      calleeMicMuted: false,
      authorizedUids: [ELDER_UID, STRANGER_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Fall detection triggers SOS event with source + peakGForce
  await assertSucceeds(
    elderDb.collection("sos_events").doc("sos_fall_01").set({
      elderId: ELDER_UID,
      elderName: "Grandpa Arthur",
      status: "active",
      areaLabel: "Near Oak Street",
      latitude: 37.422,
      longitude: -122.084,
      deliveryState: "delivered",
      source: "fall_detection",
      peakGForce: 3.15,
      authorizedUids: [ELDER_UID, CARETAKER_UID],
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Pre-creation owner get, strict link enforcement, SOS UID sync, and post-revocation isolation", async () => {
  const elderDb = testEnv.authenticatedContext(ELDER_UID).firestore();
  const caretakerDb = testEnv.authenticatedContext(CARETAKER_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const created = pastDate(10000);
  const expiresTomorrow = futureDate(86400000);

  // 1. Elder can check existence of own elder_profile before it exists; caretaker and stranger are denied
  await assertSucceeds(elderDb.collection("elder_profiles").doc(ELDER_UID).get());
  await assertFails(caretakerDb.collection("elder_profiles").doc(ELDER_UID).get());
  await assertFails(strangerDb.collection("elder_profiles").doc(ELDER_UID).get());

  // 2. Elder creates own elder_profile
  await assertSucceeds(
    elderDb.collection("elder_profiles").doc(ELDER_UID).set({
      elderId: ELDER_UID,
      displayName: "Grandpa Arthur",
      dailyWaterGoal: 8,
      locationSharingEnabled: false,
      onlineStatus: "online",
      avatarKey: "grandpa",
      authorizedUids: [ELDER_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  // 3. Caretaker CANNOT add themselves to elder_profile.authorizedUids without an active caretaker_link in the same batch
  await assertFails(
    caretakerDb.collection("elder_profiles").doc(ELDER_UID).update({
      authorizedUids: [ELDER_UID, CARETAKER_UID],
      updatedAt: pastDate(500),
    })
  );

  // 4. Even if a revoked caretaker_link exists, caretaker CANNOT add themselves without reactivating caretaker_link via a valid invite
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("caretaker_links").doc(`${ELDER_UID}_${CARETAKER_UID}`).set({
      elderId: ELDER_UID,
      elderName: "Grandpa Arthur",
      caretakerId: CARETAKER_UID,
      caretakerName: "Sarah",
      inviteCode: "OLD001",
      status: "revoked",
      authorizedUids: [ELDER_UID, CARETAKER_UID],
      createdAt: created,
      updatedAt: created,
    });
  });
  await assertFails(
    caretakerDb.collection("elder_profiles").doc(ELDER_UID).update({
      authorizedUids: [ELDER_UID, CARETAKER_UID],
      updatedAt: pastDate(400),
    })
  );

  // 5. Link caretaker cleanly with an active pairing invite
  const code = "LNK246";
  await assertSucceeds(
    elderDb.collection("pairing_invites").doc(code).set({
      code,
      elderId: ELDER_UID,
      elderName: "Grandpa Arthur",
      status: "active",
      dayKey: "2026-11-24",
      expiresAt: expiresTomorrow,
      createdAt: created,
      updatedAt: created,
    })
  );
  const relinkBatch = caretakerDb.batch();
  relinkBatch.update(
    caretakerDb.collection("caretaker_links").doc(`${ELDER_UID}_${CARETAKER_UID}`),
    {
      elderName: "Grandpa Arthur",
      caretakerName: "Sarah",
      inviteCode: code,
      status: "active",
      authorizedUids: [ELDER_UID, CARETAKER_UID],
      updatedAt: pastDate(300),
    }
  );
  relinkBatch.update(caretakerDb.collection("pairing_invites").doc(code), {
    status: "claimed",
    claimedByUid: CARETAKER_UID,
    updatedAt: pastDate(300),
  });
  relinkBatch.update(caretakerDb.collection("elder_profiles").doc(ELDER_UID), {
    authorizedUids: [ELDER_UID, CARETAKER_UID],
    updatedAt: pastDate(300),
  });
  await assertSucceeds(relinkBatch.commit());

  // 6. Seed shared records across collections and verify authorized pair read/write vs stranger denial
  await assertSucceeds(
    elderDb.collection("notifications").doc("notif_01").set({
      elderId: ELDER_UID,
      category: "sos",
      title: "SOS Alert",
      body: "Elder pressed SOS",
      readStatus: "unread",
      authorizedUids: [ELDER_UID, CARETAKER_UID],
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    elderDb.collection("sos_events").doc("sos_sync_01").set({
      elderId: ELDER_UID,
      elderName: "Grandpa Arthur",
      status: "active",
      areaLabel: "Near Oak Street",
      deliveryState: "delivered",
      authorizedUids: [ELDER_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Elder propagates newly linked caretaker UID to existing sos_events without altering status/deliveryState
  await assertSucceeds(
    elderDb.collection("sos_events").doc("sos_sync_01").update({
      authorizedUids: [ELDER_UID, CARETAKER_UID],
      updatedAt: pastDate(200),
    })
  );
  await assertSucceeds(
    caretakerDb.collection("sos_events").where("authorizedUids", "array-contains", CARETAKER_UID).get()
  );
  await assertSucceeds(
    caretakerDb.collection("notifications").doc("notif_01").update({
      readStatus: "read",
      updatedAt: pastDate(200),
    })
  );

  // Stranger is denied on notifications and sos_events
  await assertFails(strangerDb.collection("notifications").doc("notif_01").get());
  await assertFails(strangerDb.collection("sos_events").doc("sos_sync_01").get());
  await assertFails(
    strangerDb.collection("sos_events").doc("sos_sync_01").update({
      authorizedUids: [ELDER_UID, STRANGER_UID],
      updatedAt: pastDate(150),
    })
  );

  // 7. After elder revokes caretaker and propagates [ELDER_UID], caretaker loses access to elder_profile and shared docs
  const revokeBatch = elderDb.batch();
  revokeBatch.update(
    elderDb.collection("caretaker_links").doc(`${ELDER_UID}_${CARETAKER_UID}`),
    { status: "revoked", updatedAt: pastDate(100) }
  );
  revokeBatch.update(
    elderDb.collection("elder_profiles").doc(ELDER_UID),
    { authorizedUids: [ELDER_UID], updatedAt: pastDate(100) }
  );
  revokeBatch.update(
    elderDb.collection("notifications").doc("notif_01"),
    { authorizedUids: [ELDER_UID], updatedAt: pastDate(100) }
  );
  revokeBatch.update(
    elderDb.collection("sos_events").doc("sos_sync_01"),
    { authorizedUids: [ELDER_UID], updatedAt: pastDate(100) }
  );
  await assertSucceeds(revokeBatch.commit());

  await assertFails(caretakerDb.collection("elder_profiles").doc(ELDER_UID).get());
  await assertFails(caretakerDb.collection("notifications").doc("notif_01").get());
  await assertFails(caretakerDb.collection("sos_events").doc("sos_sync_01").get());
});


