const store = {};
const express = require("express");

const app = express();
const PORT = 3000;

//Middleware to read JSON
app.use(express.json());

// Middleware: require Idempotency-Key header on all requests
app.use((req, res, next) => {
    const idempotencyKey = req.headers["idempotency-key"];
    if (!idempotencyKey) {
        return res.status(400).json({ error: "Missing required header: Idempotency-Key" });
    }
    next();
});

//Test route
app.get("/", (req, res) => {
    res.send("API is running");
});

app.post("/process-payment", async (req, res) => {
    const key = req.headers["idempotency-key"];
    const body = req.body;

    // 🔍 Check if key already exists in store
    if (store[key]) {
        const existing = store[key];

        // ✅ Same key + same body → replay saved response
        if (JSON.stringify(existing.body) === JSON.stringify(body)) {
            res.set("X-Cache-Hit", "true");
            return res.status(existing.status).json(existing.response);
        }

        // ❌ Same key, different body → conflict
        return res.status(409).json({
            error: "Idempotency key already used with different request data",
        });
    }

    // 🟢 First-time request → simulate processing delay
    await new Promise((resolve) => setTimeout(resolve, 2000));

    const response = { message: `Charged ${body.amount} ${body.currency}` };

    // 💾 Save to store
    store[key] = { body, response, status: 200 };

    return res.status(200).json(response);
});

// Start server
app.listen(PORT, () => {
    console.log(`Server running on Port ${PORT}`);
});