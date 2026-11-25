const form = document.querySelector("#profile-form");
const errorElement = document.querySelector("#profile-error");

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const token = sessionStorage.getItem("researchConnectIdToken");
    if (!token) {
        errorElement.textContent = "Your sign-in session has expired. Please sign in again.";
        return;
    }

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
            throw new Error("We could not save your profile. Please review the form and try again.");
        }
        window.location.href = "/app";
    } catch (error) {
        errorElement.textContent = error.message;
    }
});
