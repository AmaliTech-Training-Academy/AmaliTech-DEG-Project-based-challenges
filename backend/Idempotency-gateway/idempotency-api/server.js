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

    // 🔍 If key exists
    if (store[key]) {
        const existing = store[key];

        // ⏳ Still processing
        if (existing.status === "processing") {
            return res.status(429).json({
                error: "Request is already being processed. Try again shortly.",
            });
        }

        // ✅ Completed — same body → replay
        if (JSON.stringify(existing.body) === JSON.stringify(body)) {
            res.set("X-Cache-Hit", "true");
            return res.status(existing.statusCode).json(existing.response);
        }

        // ❌ Completed — different body → conflict
        return res.status(409).json({
            error: "Idempotency key already used with different request data",
        });
    }

    // 🟢 Mark as processing
    store[key] = { status: "processing" };

    try {
        await new Promise((resolve) => setTimeout(resolve, 2000));

        const response = { message: `Charged ${body.amount} ${body.currency}` };

        store[key] = { status: "completed", body, response, statusCode: 200 };

        return res.status(200).json(response);
    } catch (error) {
        delete store[key];
        return res.status(500).json({ error: "Something went wrong" });
    }
});

// Start server
app.listen(PORT, () => {
    console.log(`Server running on Port ${PORT}`);
});