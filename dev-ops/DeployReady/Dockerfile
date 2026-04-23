# ==========================================
# STAGE 1: The Builder
# ==========================================
FROM node:20-alpine AS builder

WORKDIR /usr/src/app

# Copy only the dependency files first
COPY app/package*.json ./

# Install only production dependencies. 
# We don't care about cleaning the cache here because this entire container will be thrown away.
RUN npm ci --omit=dev

# Copy the rest of the application code
COPY app/ ./

# ==========================================
# STAGE 2: The Production Runtime
# ==========================================
FROM node:20-alpine

WORKDIR /usr/src/app

# Set the Node environment to production immediately
ENV NODE_ENV=production \
    PORT=3000

# Copy ONLY the clean, final files from the builder stage, and assign them to the node user
COPY --from=builder --chown=node:node /usr/src/app ./

# Switch to the non-root user for security
USER node

# Expose the dynamic port
EXPOSE $PORT

# Start the application
CMD ["npm", "start"]