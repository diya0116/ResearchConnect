export function getStoredToken() {
    return sessionStorage.getItem("researchConnectIdToken");
}

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
