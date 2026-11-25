export function getStoredToken() {
    return sessionStorage.getItem("researchConnectIdToken");
}

const signOutButton = document.querySelector("#sign-out");
signOutButton?.addEventListener("click", async () => {
    signOutButton.disabled = true;
    try {
        if (window.firebaseConfig?.apiKey) {
            const [{ getApps, initializeApp }, { getAuth, signOut }] = await Promise.all([
                import("https://www.gstatic.com/firebasejs/11.0.2/firebase-app.js"),
                import("https://www.gstatic.com/firebasejs/11.0.2/firebase-auth.js")
            ]);
            const app = getApps().length ? getApps()[0] : initializeApp(window.firebaseConfig);
            await signOut(getAuth(app));
        }
        sessionStorage.removeItem("researchConnectIdToken");
        window.location.replace("/login");
    } catch (error) {
        console.error("Unable to sign out of Firebase", error);
        let errorElement = document.querySelector("#sign-out-error");
        if (!errorElement) {
            errorElement = document.createElement("span");
            errorElement.id = "sign-out-error";
            errorElement.className = "form-error";
            errorElement.setAttribute("role", "alert");
            signOutButton.insertAdjacentElement("afterend", errorElement);
        }
        errorElement.textContent = "Sign out failed. Please try again.";
        signOutButton.disabled = false;
    }
});

export async function authenticatedFetch(url, options = {}) {
    let token = getStoredToken();
    if (window.firebaseConfig?.apiKey) {
        try {
            const [{ initializeApp, getApps }, { getAuth, onAuthStateChanged }] = await Promise.all([
                import("https://www.gstatic.com/firebasejs/11.0.2/firebase-app.js"),
                import("https://www.gstatic.com/firebasejs/11.0.2/firebase-auth.js")
            ]);
            const app = getApps().length ? getApps()[0] : initializeApp(window.firebaseConfig);
            const auth = getAuth(app);
            token = await new Promise((resolve, reject) => {
                const unsubscribe = onAuthStateChanged(auth, async user => {
                    unsubscribe();
                    if (!user) {
                        resolve(null);
                        return;
                    }
                    try {
                        resolve(await user.getIdToken(true));
                    } catch (error) {
                        reject(error);
                    }
                });
            });
            if (token) {
                sessionStorage.setItem("researchConnectIdToken", token);
            }
        } catch (error) {
            console.error("Unable to initialize Firebase authentication state", error);
        }
    }
    if (!token) {
        window.location.href = "/login";
        throw new Error("Please sign in to continue.");
    }
    const headers = new Headers(options.headers || {});
    headers.set("Authorization", `Bearer ${token}`);
    try {
        return await fetch(url, { ...options, headers });
    } catch (error) {
        console.error("ResearchConnect API request failed", { url, error });
        throw new Error("The server could not be reached. Check your connection and try again.");
    }
}
