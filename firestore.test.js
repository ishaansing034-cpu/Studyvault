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

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("notes").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      userId: ALICE_UID,
      title: "Hacked",
      description: "",
      content: "Test",
      subjectId: "science",
      subjectName: "Science",
      sectionName: "Physics",
      chapterName: "Electricity",
      noteType: "NOTES",
      tags: [],
      attachments: [],
      pinned: false,
      favourite: false,
      bookmarked: false,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("User profile: create, update, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Aarav Sharma",
      email: "aarav@example.com",
      hindiCourse: "COURSE_A",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      hindiCourse: "COURSE_B",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user read/write must fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  // Listing /users must fail
  await assertFails(aliceDb.collection("users").get());
});

test("StudyNotes: valid Kotlin create, partial update, list, and negative security bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  const validNote = {
    userId: ALICE_UID,
    title: "Ohm's Law & Resistance",
    description: "Key formulas and derivations",
    content: "# Ohm's Law\n**V = I * R**",
    subjectId: "science",
    subjectName: "Science",
    sectionName: "Physics",
    chapterName: "Electricity",
    noteType: "FORMULA",
    tags: ["physics", "electricity", "formulas"],
    attachments: [
      { id: "att1", name: "circuit.jpg", type: "IMAGE", localPath: "/files/circuit.jpg", sizeBytes: 1024 }
    ],
    pinned: true,
    favourite: true,
    bookmarked: false,
    aiTopic: "Ohm's Law",
    createdAt: created,
    updatedAt: created,
  };

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set(validNote)
  );

  // List subcollection succeeds (empty or populated)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").get()
  );

  // Partial update preserving createdAt & userId
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").update({
      pinned: false,
      favourite: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field / @DocumentId id field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_2").set({
      ...validNote,
      id: "note_2",
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_3").set({
      ...validNote,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user access denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notes").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").delete()
  );
});

test("Custom subjects, chapters, progress, flashcards, questions, and quiz attempts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customSubjects").doc("sub_1").set({
      userId: ALICE_UID,
      name: "Computer Applications",
      sections: ["Networking", "HTML"],
      colorHex: "#8B5CF6",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customChapters").doc("chap_1").set({
      userId: ALICE_UID,
      subjectId: "maths",
      sectionName: "NCERT",
      name: "Extra Trigonometry Drills",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chapterProgress").doc("prog_1").set({
      userId: ALICE_UID,
      subjectId: "maths",
      sectionName: "NCERT",
      chapterName: "Introduction to Trigonometry",
      completed: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chapterProgress").doc("prog_1").update({
      completed: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("flashcards").doc("fc_1").set({
      userId: ALICE_UID,
      subjectName: "Maths",
      chapterName: "Introduction to Trigonometry",
      question: "What is sin^2(A) + cos^2(A)?",
      answer: "1",
      mastered: false,
      noteId: "note_1",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("revisionQuestions").doc("rq_1").set({
      userId: ALICE_UID,
      subjectName: "History",
      chapterName: "Nationalism in India",
      marks: 5,
      question: "Explain the Non-Cooperation Movement.",
      answer: "Launched by Mahatma Gandhi in 1920...",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("quizAttempts").doc("qa_1").set({
      userId: ALICE_UID,
      subjectName: "Science",
      chapterName: "Electricity",
      score: 4,
      totalQuestions: 5,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cross-user isolation on revision collections
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("flashcards").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("revisionQuestions").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("quizAttempts").get());
});
