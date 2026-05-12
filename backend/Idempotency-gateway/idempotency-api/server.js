const express = require("express");

const app = express();
const PORT = 3000;

//Middleware to read JSON
app.use(express.json());

//Test route
app.get("/", (req, res) => {
    res.send("API is runnind");
});

// Start server
app.listen(PORT, ()=> {
    console.log(`Server running on Port ${PORT}`);
});

app.post("/process-payment", (req, res) => {
    const {amount, currency } = req.body;

    res.json({
        message: `Payment of ${amount} ${currency} recieved`,

    });
});