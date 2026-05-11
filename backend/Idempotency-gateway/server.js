const express = require("express");

const app = express();
app.use(express.json());

// TEST ROUTE
app.get("/", (req, res) => {
  res.send("Server is running 🚀");
});

// ✅ IMPORTANT: POST route
app.post("/process-payment", (req, res) => {
  const idempotencyKey = req.headers["idempotency-key"];
  const { amount, currency } = req.body;

  if (!idempotencyKey) {
    return res.status(400).json({
      error: "Idempotency-Key header is required",
    });
  }

  res.status(200).json({
    message: `Charged ${amount} ${currency}`,
  });
});

const PORT = 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
