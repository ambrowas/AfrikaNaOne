const admin = require('firebase-admin');
const fs = require('fs');

// Initialize Firebase Admin with credentials from your service account JSON file.
const serviceAccount = require('/Users/elebi/AndroidStudioProjects/AfrikaNaOne/afrikanaone-firebase-adminsdk-jnleq-1ee6b368a2.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();
const filePath = '/Users/elebi/AndroidStudioProjects/AfrikaNaOne/app/src/main/assets/quiz_questions.json';

// Read the JSON file containing the quiz questions.
fs.readFile(filePath, 'utf8', (err, data) => {
  if (err) {
    console.error('Error reading the file:', err);
    return;
  }

  const questions = JSON.parse(data).questions; // Assuming the JSON structure has a 'questions' array.
  uploadQuestions(questions);
});

function uploadQuestions(questions) {
  const promises = [];
  questions.forEach((question, index) => {
    // Convert number field to integer
    const number = parseInt(question.NUMBER, 10);
    const docRef = db.collection('SINGLEMODEQUESTIONS').doc(`question${index + 1}`);
    const promise = docRef.set({
        number: isNaN(number) ? 0 : number, // Set to 0 or some default if conversion fails
        category: question.CATEGORY,
        question: question.QUESTION,
        answer: question.ANSWER,
        options: {
            a: question['OPTION A'],
            b: question['OPTION B'],
            c: question['OPTION C'],
        },
        createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
    promises.push(promise);
  });

  Promise.all(promises)
    .then(() => {
      console.log('All questions have been uploaded successfully.');
    })
    .catch(error => {
      console.error('Error uploading questions:', error);
    });
}
