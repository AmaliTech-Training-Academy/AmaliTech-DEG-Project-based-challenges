# Lightweight Node.js image
FROM node:20-alpine

# Working directory
WORKDIR /app

# Copy package files
COPY app/package*.json ./

# Install dependencies
RUN npm install --production

# Copy application code
COPY app .

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Give permissions
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose app port
EXPOSE 3000

# Start app
CMD ["npm", "start"]