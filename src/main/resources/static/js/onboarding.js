import "./firebase-session.js";

const form = document.querySelector("#profile-form");
const errorElement = document.querySelector("#profile-error");
const submitButton = form.querySelector('button[type="submit"]');

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const token = sessionStorage.getItem("researchConnectIdToken");
    if (!token) {
        errorElement.textContent = "Your sign-in session has expired. Please sign in again.";
        return;
    }

    submitButton.disabled = true;
    try {
        const response = await fetch("/api/auth/profile", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`
            },
            body: JSON.stringify({
                role: document.querySelector("#role").value,
                department: document.querySelector("#department").value,
                cohort: document.querySelector("#cohort").value,
                description: document.querySelector("#description").value
            })
        });
        if (!response.ok) {
            const payload = await response.json().catch(() => null);
            throw new Error(payload?.message || "We could not save your profile. Please review the form and try again.");
        }
        window.location.href = "/app";
    } catch (error) {
        errorElement.textContent = error.message;
        submitButton.disabled = false;
    }
});
