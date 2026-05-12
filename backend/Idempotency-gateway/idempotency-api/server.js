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

app.post("/process-payment", (req, res) => {
    const key = req.headers["idempotency-key"];
    const { amount, currency } = req.body;

    res.status(201).json({
        message: `Payment of ${amount} ${currency} received`,
        idempotencyKey: key,
    });
});

// Start server
app.listen(PORT, () => {
    console.log(`Server running on Port ${PORT}`);
});