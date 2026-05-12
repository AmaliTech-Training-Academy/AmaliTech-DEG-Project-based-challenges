const express = require("express");

const app = express();
const store = {};
app.use(express.json());

// TEST ROUTE
app.get("/", (req, res) => {
  res.send("Server is running 🚀");
});

// ✅ IMPORTANT: POST route
app.post("/process-payment", async (req, res) => {
  const key = req.headers["idempotency-key"];
  const body = req.body;

  if (!key) {
    return res.status(400).json({
      error: "Idempotency-Key header is required",
    });
  }

  // 🔍 Check if key already exists
  if (store[key]) {
    const existing = store[key];

    // ✅ Compare request bodies
    if (JSON.stringify(existing.body) === JSON.stringify(body)) {
      res.set("X-Cache-Hit", "true");
      return res.status(existing.status).json(existing.response);
    } else {
      return res.status(409).json({
        error: "Idempotency key already used for a different request body",
      });
    }
  }

  // 🟢 First time request → simulate processing
  await new Promise((resolve) => setTimeout(resolve, 2000));

  const response = {
    message: `Charged ${body.amount} ${body.currency}`,
  };

  // 💾 Save result
  store[key] = {
    body: body,
    response: response,
    status: 200,
  };

  return res.status(200).json(response);
});

const PORT = 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
