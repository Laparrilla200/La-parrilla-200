import { initializeApp, getApps } from 'firebase/app';
import { getFirestore, initializeFirestore, persistentLocalCache, persistentMultipleTabManager } from 'firebase/firestore';
import { getAI, GoogleAIBackend } from 'firebase/ai';

export const FIREBASE_DATABASE_ID =
  import.meta.env.VITE_FIREBASE_DATABASE_ID ||
  'ai-studio-android-laparril-1b643e62-cfb5-4b1c-96c5-d20e626d77dd';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || 'AIzaSyBEKzZaJfQrnqr7UbT5M1JU11GGmNJKoiE',
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || 'gen-lang-client-0261940484.firebaseapp.com',
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || 'gen-lang-client-0261940484',
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || 'gen-lang-client-0261940484.firebasestorage.app',
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || '602253194395',
  appId: import.meta.env.VITE_FIREBASE_APP_ID || '1:602253194395:android:0065a3d0573a85e6c6c23e'
};

export const firebaseApp = getApps().length === 0 ? initializeApp(firebaseConfig) : getApps()[0];

function createDb() {
  try {
    return initializeFirestore(
      firebaseApp,
      {
        localCache: persistentLocalCache({
          tabManager: persistentMultipleTabManager()
        })
      },
      FIREBASE_DATABASE_ID
    );
  } catch {
    return getFirestore(firebaseApp, FIREBASE_DATABASE_ID);
  }
}

export const db = createDb();

export function getFirebaseAiInstance() {
  return getAI(firebaseApp, { backend: new GoogleAIBackend() });
}
