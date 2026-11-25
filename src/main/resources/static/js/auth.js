import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.2/firebase-app.js";
import { getAuth, GoogleAuthProvider, signInWithPopup } from "https://www.gstatic.com/firebasejs/11.0.2/firebase-auth.js";

const errorElement = document.querySelector("#auth-error");
const button = document.querySelector("#google-sign-in");

if (!window.firebaseConfig.apiKey || !window.firebaseConfig.projectId) {
    errorElement.textContent = "Firebase web configuration is not available. Configure the application environment first.";
    button.disabled = true;
} else {
    const app = initializeApp(window.firebaseConfig);
    const auth = getAuth(app);
    const provider = new GoogleAuthProvider();
    provider.setCustomParameters({ prompt: "select_account" });

    button.addEventListener("click", async () => {
        button.disabled = true;
        errorElement.textContent = "";
        try {
            const result = await signInWithPopup(auth, provider);
            const token = await result.user.getIdToken(true);
            const response = await fetch("/api/auth/me", {
                headers: { Authorization: `Bearer ${token}` }
            });
            if (!response.ok) {
                throw new Error("Sign in with a verified @thapar.edu Google account.");
            }
            sessionStorage.setItem("researchConnectIdToken", token);
            const account = await response.json();
            window.location.href = account.profileComplete ? "/app" : "/onboarding";
        } catch (error) {
            errorElement.textContent = error.message || "Sign-in failed. Please try again.";
            button.disabled = false;
        }
    });
}
