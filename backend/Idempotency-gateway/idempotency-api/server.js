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
    const key = req.headers["idempotency-key"]?.trim().toLowerCase();
    const body = req.body;

    console.log("Idempotency key:", key);
    console.log("Store BEFORE:", store);

    // 🔍 If key exists
    if (store[key]) {
        const existing = store[key];

        // ⏳ Still processing
        if (existing.status === "processing") {
            return res.status(429).json({
                error: "Request is already being processed. Try again shortly.",
            });
        }

        // ✅ Completed — same body → replay, different body → conflict
        if (existing.status === "completed") {
            if (JSON.stringify(existing.body) === JSON.stringify(body)) {
                console.log("Returning cached response ✅");
                res.setHeader("X-Cache-Hit", "true");
                return res.status(existing.statusCode || 201).json(existing.response);
            } else {
                return res.status(409).json({
                    error: "Idempotency key already used with different request data",
                });
            }
        }
    }

    // 🟢 Mark as processing
    store[key] = { status: "processing" };

    try {
        console.log("Processing NEW payment 💰");
        await new Promise((resolve) => setTimeout(resolve, 2000));

        const response = { message: `Charged ${body.amount} ${body.currency}` };

        store[key] = { status: "completed", body, response, statusCode: 200 };
        console.log("Store AFTER:", store);

        return res.status(201).json(response);
    } catch (error) {
        delete store[key];
        return res.status(500).json({ error: "Something went wrong" });
    }
});

// Start server
app.listen(PORT, () => {
    console.log(`Server running on Port ${PORT}`);
});